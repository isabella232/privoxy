package sql;

import org.apache.calcite.sql.*;
import solver.Query;
import solver.Schema;
import solver.UnionQuery;

import java.util.*;

public class PrivacyQueryUnion extends PrivacyQuery {
    private List<PrivacyQuery> queries;

    public PrivacyQueryUnion(ParserResult parsedSql, SchemaPlusWithKey schema) {
        this(parsedSql, schema, new Object[0], Collections.emptyList());
    }

    public PrivacyQueryUnion(ParserResult parsedSql, SchemaPlusWithKey schema, Object[] parameters, List<String> paramNames) {
        super(parsedSql, parameters, paramNames);
        assert parsedSql.getSqlNode() instanceof SqlBasicCall;
        SqlBasicCall unionNode = (SqlBasicCall) parsedSql.getSqlNode();
        queries = new ArrayList<>();
        int paramOffset = 0;
        for (int i = 0; i < unionNode.operandCount(); ++i) {
            SqlNode operand = unionNode.operand(i);
            int paramCount = (" " + operand.toString() + " ").split("\\?").length - 1;
            Object[] partParameters = Arrays.copyOfRange(parameters, paramOffset, paramOffset + paramCount);
            List<String> partParamNames = paramNames.subList(paramOffset, paramOffset + paramCount);
            PrivacyQuery query = PrivacyQueryFactory.createPrivacyQuery(new UnionPartParserResult(operand), schema, partParameters, partParamNames);
            queries.add(query);

            paramOffset += paramCount;
        }
    }

    @Override
    public Set<String> getProjectColumns() {
        Set<String> result = new HashSet<>();
        for (PrivacyQuery query : queries) {
            result.addAll(query.getProjectColumns());
        }
        return result;
    }

    @Override
    public Set<String> getThetaColumns() {
        Set<String> result = new HashSet<>();
        for (PrivacyQuery query : queries) {
            result.addAll(query.getThetaColumns());
        }
        return result;
    }

    @Override
    public Query getSolverQuery(Schema schema) {
        return new UnionQuery(queries.stream().map(q -> q.getSolverQuery(schema)).toArray(Query[]::new));
    }

    @Override
    public List<Boolean> getResultBitmap() {
        if (queries.isEmpty()) {
            return Collections.emptyList();
        }
        List<Boolean> bitmap = queries.get(0).getResultBitmap();
        for (PrivacyQuery query : queries) {
            List<Boolean> bitmap1 = query.getResultBitmap();
            if (bitmap1.size() < bitmap.size()) {
                List<Boolean> temp = bitmap;
                bitmap = bitmap1;
                bitmap1 = temp;
            }
            for (int i = 0; i < bitmap.size(); ++i) {
                bitmap.set(i, bitmap.get(i) && bitmap1.get(i));
            }
        }
        return bitmap;
    }

    private class UnionPartParserResult extends ParserResult {
        private UnionPartParserResult(SqlNode node) {
            super(node.toString(), node.getKind(), node, false, false);
        }
    }
}