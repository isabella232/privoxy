package sql;

import com.google.common.collect.ImmutableList;
import com.microsoft.z3.*;
import org.apache.calcite.sql.*;
import org.apache.calcite.sql.type.SqlTypeName;
import planner.PrivacyColumn;
import planner.PrivacyTable;
import solver.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;

public class ParsedPSJ {
    private final List<String> relations;
    private boolean hasRelAlias = false;
    private final HashMap<String, Integer> relAliasToIdx;
    private final List<String> projectColumns;
    private final List<String> thetaColumns;
    private final List<Object> parameters;
    private final List<String> paramNames;
    private final List<SqlBasicCall> theta;
    private List<Boolean> resultBitmap;

    public ParsedPSJ(SqlNode parsedSql, SchemaPlusWithKey schema, List<Object> parameters, List<String> paramNames) {
        projectColumns = new ArrayList<>();
        thetaColumns = new ArrayList<>();
        this.relAliasToIdx = new HashMap<>(); // Maps relation aliases (and alias-less relation names) to index.
        this.parameters = parameters;
        this.paramNames = paramNames;
        this.theta = new ArrayList<>();
        this.resultBitmap = new ArrayList<>();

        SqlSelect sqlSelect = (SqlSelect) parsedSql;
        SqlNode fromClause = sqlSelect.getFrom();
        // innermost subquery while no joins in from
        while (fromClause.getKind() == SqlKind.AS && (((SqlBasicCall) fromClause).operand(0) instanceof SqlSelect || ((SqlBasicCall) fromClause).operand(0) instanceof SqlOrderBy)) {
            if (((SqlBasicCall) fromClause).operand(0) instanceof SqlOrderBy) {
                sqlSelect = (SqlSelect) ((SqlOrderBy) ((SqlBasicCall) fromClause).operand(0)).query;
            } else {
                sqlSelect = ((SqlBasicCall) fromClause).operand(0);
            }
            fromClause = sqlSelect.getFrom();
            this.resultBitmap = null;
        }
        if (fromClause.getKind() != SqlKind.JOIN) {
            if (!(fromClause instanceof SqlIdentifier)) {
                throw new RuntimeException("unhandled from clause: " + fromClause);
            }
            List<String> names = ((SqlIdentifier) fromClause).names;
            String relation = names.get(names.size() - 1).toUpperCase();
            relations = Collections.singletonList(relation);
            relAliasToIdx.put(relation, 0);
        } else {
            relations = new ArrayList<>();
            extractRelationNames((SqlJoin) fromClause);
        }
        for (SqlNode sn : sqlSelect.getSelectList()) {
            // ignore unary function calls and use whatever they're called with instead
            boolean addPrimaryKey = false;
            while (sn instanceof SqlBasicCall) {
                SqlBasicCall call = (SqlBasicCall) sn;
                if (call.getOperator() instanceof SqlAsOperator) {
                    SqlNode op0 = call.operand(0);
                    if (!(op0.getKind() == SqlKind.LITERAL || op0.getKind() == SqlKind.IDENTIFIER)) {
                        throw new RuntimeException("only literal & identifier aliases are handled");
                    }
                    sn = op0;
                    continue;
                }
                if (call.operandCount() != 1) { // only supporting unary functions
                    throw new RuntimeException("only supporting unary functions");
                }
                sn = call.getOperands()[0];
                this.resultBitmap = null;
                addPrimaryKey = true;
            }

            if (sn instanceof SqlLiteral) {
                resultBitmap.add(false);
                continue;
            }

            SqlIdentifier identifier = (SqlIdentifier) sn;

            if (addPrimaryKey) {
                if (hasRelAlias) {
                    throw new RuntimeException("not supported: relation alias");
                }
                if (identifier.names.size() == 1) {
                    for (String relation : relations) {
                        for (String column : schema.primaryKeys.get(relation)) {
                            projectColumns.add((relation + "." + column).toUpperCase());
                        }
                    }
                } else {
                    String relation = identifier.names.get(0).toUpperCase();
                    for (String column : schema.primaryKeys.get(relation)) {
                        projectColumns.add((relation + "." + column).toUpperCase());
                    }
                }
            }

            if (identifier.names.get(identifier.names.size() - 1).equals("")) {
                if (identifier.names.size() == 1) { // SELECT * FROM ...
                    if (hasRelAlias) {
                        throw new RuntimeException("not supported: relation alias");
                    }
                    for (String relation : relations) {
                        for (PrivacyColumn column : ((PrivacyTable) schema.schema.getTable(relation.toLowerCase())).getColumns()) {
                            addProjectColumn((relation + "." + column.name).toUpperCase());
                        }
                    }
                } else { // SELECT table.* FROM ...
                    String quantifier = identifier.names.get(identifier.names.size() - 2).toUpperCase();
                    String relation = relations.get(relAliasToIdx.get(quantifier));
                    for (PrivacyColumn column : ((PrivacyTable) schema.schema.getTable(relation.toLowerCase())).getColumns()) {
                        addProjectColumn((quantifier + "." + column.name).toUpperCase());
                    }
                }
            } else {
                addProjectColumn(quantifyName(identifier));
            }
        }

        // not WHERE TRUE, WHERE FALSE
        // FIXME(zhangwen): how does WHERE FALSE get handled?
        if (sqlSelect.getWhere() != null && sqlSelect.getWhere().getKind() != SqlKind.LITERAL) {
            SqlBasicCall mainTheta = (SqlBasicCall) sqlSelect.getWhere();
            if (mainTheta != null) {
                addTheta(mainTheta);
            }
        }
    }

    private String getRelationNameForAlias(String alias) {
        return relations.get(relAliasToIdx.get(alias));
    }

    private void addProjectColumn(String column) {
        projectColumns.add(column);
        if (resultBitmap != null) {
            resultBitmap.add(true);
        }
    }

    private void extractRelationNames(SqlJoin join) {
        if (join.getJoinType() != JoinType.COMMA && join.getJoinType() != JoinType.INNER) {
            throw new RuntimeException("unhandled join type: " + join.getJoinType() + ", " + join.getCondition());
        }
        SqlNode left = join.getLeft();
        SqlNode right = join.getRight();
        if (left.getKind() == SqlKind.JOIN) {
            extractRelationNames((SqlJoin) left);
        } else {
            addRelationName(left);
        }
        if (right.getKind() == SqlKind.JOIN) {
            extractRelationNames((SqlJoin) right);
        } else {
            addRelationName(right);
        }

        if (join.getCondition() != null && join.getCondition().getKind() != SqlKind.LITERAL) {
            addTheta((SqlBasicCall) join.getCondition());
        }
    }

    private void addRelationName(SqlNode node) {
        String alias = null;
        if (node.getKind() == SqlKind.AS) {
            SqlBasicCall call = (SqlBasicCall) node;
            SqlNode rhs = call.operand(1);
            ImmutableList<String> names = ((SqlIdentifier) rhs).names;
            if (names.size() > 1) {
                throw new RuntimeException("not supported: multipart table alias: " + rhs);
            }
            alias = names.get(0);
            node = call.operand(0);
            hasRelAlias = true;
        }

        SqlIdentifier identifier = (SqlIdentifier) node;
        String relationName = identifier.names.get(identifier.names.size() - 1).toUpperCase();
        relations.add(relationName);
        if (alias != null) {
            relAliasToIdx.put(alias.toUpperCase(), relations.size() - 1);
        } else {
            if (relAliasToIdx.containsKey(relationName)) {
                throw new RuntimeException("duplicate relation name: " + relationName);
            }
            relAliasToIdx.put(relationName.toUpperCase(), relations.size() - 1);
        }
    }

    private void addTheta(SqlBasicCall predicate) {
        addTheta(predicate, true);
    }

    private void addTheta(SqlBasicCall predicate, boolean addToList) {
        if (addToList) {
            theta.add(predicate);
        }

        SqlNode left = predicate.operand(0);
        SqlNode right = predicate.operand(1);
        if (left instanceof SqlBasicCall) {
            addTheta((SqlBasicCall) left, false);
        } else if (left instanceof SqlIdentifier) {
            String name = quantifyName((SqlIdentifier) left);
            if (!name.startsWith("!")) {
                thetaColumns.add(name);
            }
        }
        if (right instanceof SqlBasicCall) {
            addTheta((SqlBasicCall) right, false);
        } else if (right instanceof SqlIdentifier) {
            String name = quantifyName((SqlIdentifier) right);
            if (!name.startsWith("!")) {
                thetaColumns.add(name);
            }
        }
    }

    private Expr getPredicate(SqlNode theta, Map<String, Expr> symbolMap, List<Object> params, List<String> paramNames, Schema schema) {
        Context context = schema.getContext();
        if (theta instanceof SqlIdentifier) {
            String name = quantifyName((SqlIdentifier) theta);
            if (symbolMap.containsKey(name)) {
                return symbolMap.get(name);
            } else if (!name.startsWith("!")) {
                String[] parts = name.split("\\.", 2);
                checkArgument(parts.length == 2, "not a two-part name: %s", name);
                String relationName = getRelationNameForAlias(parts[0]);
                List<Column> columns = schema.getColumns(relationName);
                for (Column column : columns) {
                    if (column.name.toUpperCase().equals(parts[1])) {
                        return context.mkConst(context.mkSymbol(name), column.type);
                    }
                }

                throw new RuntimeException("unknown type for column " + name);
            } else {
                return context.mkConst(name, context.getIntSort());
            }
        } else if (theta instanceof SqlLiteral) {
            SqlLiteral literal = (SqlLiteral) theta;
            if (literal.getTypeName() == SqlTypeName.BOOLEAN) {
                return context.mkBool(literal.booleanValue());
            } else if (literal.getTypeName() == SqlTypeName.INTEGER || literal.getTypeName() == SqlTypeName.DECIMAL) {
                return context.mkInt(literal.intValue(true));
            } else if (literal.getTypeName() == SqlTypeName.CHAR) {
                return context.mkString(literal.getValueAs(String.class));
            }
            throw new UnsupportedOperationException("unhandled literal type: " + literal.getTypeName());
        } else if (theta instanceof SqlBasicCall) {
            Expr left = getPredicate(((SqlBasicCall) theta).operand(0), symbolMap, params, paramNames, schema);

            if (theta.getKind() == SqlKind.IN || theta.getKind() == SqlKind.NOT_IN) {
                final Expr left1 = left;
                SqlNodeList values = ((SqlBasicCall) theta).operand(1);
                BoolExpr[] exprs = values.getList().stream()
                        .map(n -> context.mkEq(left1, getPredicate(n, symbolMap, params, paramNames, schema)))
                        .toArray(BoolExpr[]::new);
                BoolExpr expr = context.mkOr(exprs);
                if (theta.getKind() == SqlKind.NOT_IN) {
                    expr = context.mkNot(expr);
                }
                return expr;
            }

            Expr right = getPredicate(((SqlBasicCall) theta).operand(1), symbolMap, params, paramNames, schema);
            if (left instanceof ArithExpr && right instanceof SeqExpr) {
                try {
                    System.out.println("!!!*** " + theta);
                    System.out.println("\t" + right);
                    right = context.mkInt(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(right.getString()).getTime());
                } catch (ParseException e) {
                    // do nothing
                }
            } else if (right instanceof ArithExpr && left instanceof SeqExpr) {
                try {
                    left = context.mkInt(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(left.getString()).getTime());
                } catch (ParseException e) {
                    // do nothing
                }
            }
            if (theta.getKind() == SqlKind.AND) {
                return context.mkAnd((BoolExpr) left, (BoolExpr) right);
            } else if (theta.getKind() == SqlKind.OR) {
                return context.mkOr((BoolExpr) left, (BoolExpr) right);
            } else if (theta.getKind() == SqlKind.EQUALS) {
                return context.mkEq(left, right);
            } else if (theta.getKind() == SqlKind.NOT_EQUALS) {
                return context.mkNot(context.mkEq(left, right));
            } else if (theta.getKind() == SqlKind.LESS_THAN) {
                return context.mkLt((ArithExpr) left, (ArithExpr) right);
            } else if (theta.getKind() == SqlKind.LESS_THAN_OR_EQUAL) {
                return context.mkLe((ArithExpr) left, (ArithExpr) right);
            } else if (theta.getKind() == SqlKind.GREATER_THAN) {
                return context.mkGt((ArithExpr) left, (ArithExpr) right);
            } else if (theta.getKind() == SqlKind.GREATER_THAN_OR_EQUAL) {
                return context.mkGe((ArithExpr) left, (ArithExpr) right);
            }
        } else if (theta instanceof SqlDynamicParam) {
            Object param = params.get(params.size() - 1);
            params.remove(params.size() - 1);
            String name = "!" + paramNames.get(paramNames.size() - 1);
            paramNames.remove(paramNames.size() - 1);
            if (name.equals("!?")) {
                if (param == null) {
                    throw new UnsupportedOperationException("null parameter is not supported (yet)");
                }
                return Tuple.getExprFromObject(context, param);
            } else {
                if (symbolMap.containsKey(name)) {
                    return symbolMap.get(name);
                } else {
                    return context.mkConst(context.mkSymbol(name), Tuple.getSortFromObject(context, param));
                }
            }
        }
        throw new UnsupportedOperationException("where clause uses unsupported operations: " + theta);
    }

    private String quantifyName(SqlIdentifier identifier) {
        List<String> names = identifier.names;
        if (names.size() == 1) {
            if (names.get(0).startsWith("_")) {
                return "!" + names.get(0);
            }
            if (relations.size() != 1) {
                throw new UnsupportedOperationException("joins must only use fully-specified column names");
            }
            return (relations.get(0) + "." + identifier.names.get(0)).toUpperCase();
        } else {
            return (names.get(0) + "." + names.get(1)).toUpperCase();
        }
    }

    public List<String> getProjectColumns() {
        return projectColumns;
    }

    public List<String> getThetaColumns() {
        return thetaColumns;
    }

    public List<String> getRelations() {
        return relations;
    }

    private BoolExpr getPredicate(Map<String, Expr> symbolMap, Schema schema, String prefix, int parameterOffset) {
        Context context = schema.getContext();
        if (theta != null && theta.size() > 0) {
            List<Object> params = new ArrayList<>(parameters);
            Collections.reverse(params);
            List<String> names = new ArrayList<>(paramNames);
            if (prefix != null) {
                for (int i = 0; i < names.size(); ++i) {
                    if (names.get(i).equals("?")) {
                        names.set(i, prefix + "!" + (i + parameterOffset));
                    }
                }
            }
            Collections.reverse(names);
            BoolExpr[] exprs = new BoolExpr[theta.size()];
            for (int i = 0; i < theta.size(); ++i) {
                exprs[i] = (BoolExpr) getPredicate(theta.get(i), symbolMap, params, names, schema);
            }
            return context.mkAnd(exprs);
        } else {
            return context.mkTrue();
        }
    }

    public BoolExpr getPredicate(Schema schema) {
        return getPredicate(Collections.emptyMap(), schema, null, 0);
    }

    public Query getSolverQuery(Schema schema) {
        return getSolverQuery(schema, null, 0);
    }

    public Query getSolverQuery(Schema schema, String prefix, int offset) {
        return new SolverQuery(schema, prefix, offset);
    }

    public List<Boolean> getResultBitmap() {
        return resultBitmap == null ? Collections.emptyList() : resultBitmap;
    }

    /**
     * Checks if this policy has no `WHERE` clause, i.e., returns all rows.
     * @return true if this policy has no `WHERE` clause.
     */
    public boolean hasNoTheta() {
        return theta.isEmpty();
    }

    private class SolverQuery extends PSJ {
        int[] projectRelationIndex;
        int[] projectColumnIndex;
        int[] thetaRelationIndex;
        int[] thetaColumnIndex;
        Schema schema;
        String prefix;
        int parameterOffset;

        public SolverQuery(Schema schema, String prefix, int parameterOffset) {
            super(schema, relations);

            projectRelationIndex = new int[projectColumns.size()];
            projectColumnIndex = new int[projectColumns.size()];
            thetaRelationIndex = new int[thetaColumns.size()];
            thetaColumnIndex = new int[thetaColumns.size()];

            this.schema = schema;
            this.prefix = prefix;
            this.parameterOffset = parameterOffset;

            mapIndices(schema, projectColumns, projectRelationIndex, projectColumnIndex);
            mapIndices(schema, thetaColumns, thetaRelationIndex, thetaColumnIndex);
        }

        private void mapIndices(Schema schema, List<String> columns, int[] relationIndex, int[] columnIndex) {
            Iterator<String> iter = columns.iterator();
            for (int i = 0; i < columns.size(); ++i) {
                String[] parts = iter.next().split("\\.");
                String quantifier = parts[0]; // A quantifier can either be a relation name or an alias.

                int currIdx = relAliasToIdx.get(quantifier);
                relationIndex[i] = currIdx;

                String relationName = relations.get(currIdx);
                List<String> columnNames = schema.getColumnNames(relationName);
                columnIndex[i] = columnNames.indexOf(parts[1]);
                if (relationIndex[i] == -1 || columnIndex[i] == -1) {
                    throw new RuntimeException("column not found: " + relationName + "." + parts[1]
                            + " in columns: " + columnNames);
                }
            }
        }

        @Override
        protected BoolExpr predicateGenerator(Tuple... tuples) {
            Map<String, Expr> map = new HashMap<>();
            for (int i = 0; i < thetaColumnIndex.length; ++i) {
                map.put(thetaColumns.get(i), tuples[thetaRelationIndex[i]].get(thetaColumnIndex[i]));
            }
            return getPredicate(map, schema, prefix, parameterOffset);
        }

        @Override
        protected Tuple headSelector(Tuple... tuples) {
            Expr[] parts = new Expr[projectRelationIndex.length];
            for (int i = 0; i < parts.length; ++i) {
                parts[i] = tuples[projectRelationIndex[i]].get(projectColumnIndex[i]);
            }
            return new Tuple(schema, parts);
        }

        @Override
        protected Sort[] headTypeSelector(Sort[]... types) {
            Sort[] parts = new Sort[projectRelationIndex.length];
            for (int i = 0; i < parts.length; ++i) {
                parts[i] = types[projectRelationIndex[i]][projectColumnIndex[i]];
            }
            return parts;
        }
    }
}
