package sql;

import org.apache.calcite.sql.*;
import solver.Query;
import solver.Schema;

import java.util.*;

public class PrivacyQuerySelect extends PrivacyQuery {
    private ParsedPSJ parsedPSJ;

    public PrivacyQuerySelect(ParserResult parsedSql, SchemaPlusWithKey schema) {
        this(parsedSql, schema, new Object[0], Collections.emptyList());
    }

    public PrivacyQuerySelect(ParserResult parsedSql, SchemaPlusWithKey schema, Object[] parameters, List<String> paramNames) {
        super(parsedSql, parameters, paramNames);
        parsedPSJ = new ParsedPSJ(getSelectNode(parsedSql), schema, Arrays.asList(parameters), paramNames);
    }

    private SqlSelect getSelectNode(ParserResult result) {
        if (result.getSqlNode() instanceof SqlOrderBy) {
            return (SqlSelect) ((SqlOrderBy) result.getSqlNode()).query;
        } else {
            return (SqlSelect) result.getSqlNode();
        }
    }

    @Override
    public List<String> getProjectColumns() {
        return new ArrayList<>(parsedPSJ.getProjectColumns());
    }

    @Override
    public List<String> getThetaColumns() {
        return new ArrayList<>(parsedPSJ.getThetaColumns());
    }

    @Override
    public Query getSolverQuery(Schema schema) {
        return parsedPSJ.getSolverQuery(schema);
    }

    @Override
    public Query getSolverQuery(Schema schema, String prefix, int offset) {
        return parsedPSJ.getSolverQuery(schema, prefix, offset);
    }

    @Override
    public List<Boolean> getResultBitmap() {
        return parsedPSJ.getResultBitmap();
    }
}
