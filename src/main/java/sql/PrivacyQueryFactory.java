package sql;

import org.apache.calcite.schema.SchemaPlus;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PrivacyQueryFactory {

    public static PrivacyQuery createPrivacyQuery(ParserResult result, SchemaPlusWithKey schema)
    {
        return createPrivacyQuery(result, schema, new Object[0], Collections.emptyList());
    }

    public static PrivacyQuery createPrivacyQuery(ParserResult result, SchemaPlusWithKey schema, Object[] parameters, List<String> paramNames)
    {
        if (result == null){
            return null;
        }
        switch(result.getKind()) {
            case SELECT:
            case ORDER_BY:
                return new PrivacyQuerySelect(result, schema, parameters, paramNames);
            default:
                throw new AssertionError("unexpected");
        }
    }
}
