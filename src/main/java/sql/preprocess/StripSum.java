package sql.preprocess;

import org.apache.calcite.sql.*;
import sql.ParserResult;
import sql.PrivacyQuery;
import sql.PrivacyQueryFactory;
import sql.SchemaPlusWithKey;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class StripSum implements Preprocessor {
    public static final StripSum INSTANCE = new StripSum();

    private StripSum() {}

    public Optional<PrivacyQuery> perform(ParserResult result, SchemaPlusWithKey schema, Object[] parameters,
                                          List<String> paramNames, Map<Integer, String> revConstMap) {
        if (!result.getParsedSql().contains("SUM")) { return Optional.empty(); }

        if (result.getKind() != SqlKind.SELECT) { return Optional.empty(); }

        SqlSelect sqlSelect = (SqlSelect) result.getSqlNode();
        SqlNode fromClause = sqlSelect.getFrom();
        if (fromClause.getKind() != SqlKind.IDENTIFIER) { return Optional.empty(); }

        SqlNodeList l = sqlSelect.getSelectList();
        if (l.size() != 1) { return Optional.empty(); }

        SqlNode node = l.get(0);
        if (!(node instanceof SqlBasicCall)) { return Optional.empty(); }

        SqlBasicCall call = (SqlBasicCall) node;
        if (!call.getOperator().getName().equals("SUM")) { return Optional.empty(); }
        SqlNode operand = call.getOperands()[0];
        if (operand.getKind() != SqlKind.IDENTIFIER) { return Optional.empty(); }
        SqlIdentifier opId = (SqlIdentifier) operand;
        if (opId.names.size() != 2) { return Optional.empty(); }

        SqlSelect newSelect = (SqlSelect) sqlSelect.clone(sqlSelect.getParserPosition());
        newSelect.setSelectList(SqlNodeList.SINGLETON_STAR);
        ParserResult newPR = new ParserResult(newSelect.toString(), newSelect.getKind(), newSelect,
                false, false) {};
        PrivacyQuery pq = new PrivacyQueryEmptyRBWrapper(
                PrivacyQueryFactory.createPrivacyQuery(newPR, schema, parameters, paramNames, revConstMap));
        return Optional.of(pq);
    }
}
