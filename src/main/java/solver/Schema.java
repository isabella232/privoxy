package solver;

import com.microsoft.z3.*;

import java.sql.Types;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;

public class Schema {
    private final MyZ3Context context;
    private final Map<String, List<Column>> relations;
    private final List<Dependency> dependencies;

    public Schema(MyZ3Context context, Map<String, List<Column>> relations, List<Dependency> dependencies) {
        this.context = checkNotNull(context);
        this.relations = checkNotNull(relations);
        this.dependencies = checkNotNull(dependencies);
    }

    public MyZ3Context getContext() {
        return context;
    }

    public Instance makeFreshInstance() {
        Instance instance = new Instance(this);
        List<BoolExpr> constraints = new ArrayList<>();
        for (Map.Entry<String, List<Column>> relation : relations.entrySet()) {
            String relationName = relation.getKey();
            List<Column> columns = relation.getValue();

            Sort[] colTypes = columns.stream().map(column -> column.type).toArray(Sort[]::new);
            FuncDecl func = context.mkFreshFuncDecl("v", colTypes, context.getBoolSort());
            instance.put(relationName, new Relation(this, new Z3Function(func), colTypes));

            // Apply per-column constraints.
            Tuple tuple = this.makeFreshTuple(relationName);
            List<BoolExpr> thisConstraints = new ArrayList<>();
            for (int i = 0; i < tuple.size(); ++i) {
                Column column = columns.get(i);
                if (column.constraint == null) {
                    continue;
                }
                thisConstraints.add(column.constraint.apply(tuple.get(i)));
            }
            if (!thisConstraints.isEmpty()) {
                BoolExpr lhs = (BoolExpr) func.apply(tuple.toExprArray());
                BoolExpr rhs = context.mkAnd(thisConstraints.toArray(new BoolExpr[0]));
                BoolExpr body = context.mkImplies(lhs, rhs);
                constraints.add(context.mkForall(tuple.toExprArray(), body, 1, null, null, null, null));
            }
        }

        // Apply dependencies.
        for (Dependency d : dependencies) {
            constraints.add(d.apply(instance));
        }

        instance.constraint = context.mkAnd(constraints.toArray(new BoolExpr[0]));
        return instance;
    }

    public Instance makeConcreteInstance(Map<String, List<Tuple>> content) {
        Instance instance = this.makeFreshInstance();
        List<BoolExpr> constraints = new ArrayList<>();
        constraints.add(instance.constraint);
        for (Map.Entry<String, List<Tuple>> c : content.entrySet()) {
            String relationName = c.getKey();
            List<Tuple> tuples = c.getValue();

            Tuple tuple = this.makeFreshTuple(relationName);
            BoolExpr lhs = instance.get(relationName).apply(tuple);
            Stream<BoolExpr> rhsExprs = tuples.stream().map(tuple::tupleEqual);
            BoolExpr rhs = context.mkOr(rhsExprs.toArray(BoolExpr[]::new));
            BoolExpr body = context.mkEq(lhs, rhs);
            constraints.add(context.mkForall(tuple.toExprArray(), body, 1, null, null, null, null));
        }
        instance.constraint = context.mkAnd(constraints.toArray(new BoolExpr[0]));
        return instance;
    }

    public List<Column> getColumns(String relationName) {
        return relations.get(relationName.toUpperCase());
    }

    private final Map<String, List<String>> columnNamesCache = new HashMap<>();
    public List<String> getColumnNames(String relationName) {
        return columnNamesCache.computeIfAbsent(
                relationName,
                k -> relations.get(k).stream().map(c -> c.name).collect(Collectors.toList())
        );
    }

    public Tuple makeFreshTuple(String relationName) {
        List<Column> columns = relations.get(relationName.toUpperCase());
        return new Tuple(this, columns.stream().map(column -> context.mkFreshConst("v", column.type)));
    }

    public static Sort getSortFromSqlType(Context context, int type) {
        switch (type) {
            case Types.INTEGER:
            case Types.BIGINT:
            case Types.TINYINT:
                return context.getIntSort();
            case Types.DOUBLE:
                return context.getRealSort();
            case Types.BOOLEAN:
                return context.getBoolSort();
            case Types.VARCHAR:
            case Types.LONGVARCHAR:
            case Types.CLOB:
                return context.getStringSort();
            case Types.TIMESTAMP:
            case Types.DATE:
                return context.getIntSort();
            default:
                throw new UnsupportedOperationException("bad column type: " + type);
        }
    }
}
