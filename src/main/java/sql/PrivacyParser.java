package sql;

import org.apache.calcite.adapter.java.JavaTypeFactory;
import org.apache.calcite.avatica.util.Casing;
import org.apache.calcite.avatica.util.Quoting;
import org.apache.calcite.config.CalciteConnectionConfig;
import org.apache.calcite.jdbc.CalcitePrepare;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.util.SqlShuttle;
import org.apache.calcite.sql.SqlIdentifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import planner.DataSourceSchema;


import java.sql.SQLException;
import java.util.Properties;

public class PrivacyParser implements Parser {

    private static final Logger LOG = LoggerFactory.getLogger(PrivacyParser.class);
    private final QueryContext context;

    public PrivacyParser(Properties info) throws PrivacyException
    {
        System.out.println("inside constructor");
        this.context = new QueryContext(info);
        System.out.println("in query context");
    }

    public PrivacyParserResult parse(String sql) throws SQLException {
        //System.out.println("Parsing in privacyparser");
        //SqlParser parser = SqlParser.create(sql,
        //        SqlParser.configBuilder()
        //                .setQuotedCasing(Casing.UNCHANGED)
        //                .setUnquotedCasing(Casing.UNCHANGED)
        //                .setQuoting(Quoting.DOUBLE_QUOTE)
        //                .build());

        DataSourceSchema dataSource = this.context.getDefaultDataSource();

        final CalciteConnectionConfig config = context.getCfg();
        SqlParser parser = SqlParser.create(sql,
                SqlParser.configBuilder()
                        .setQuotedCasing(config.quotedCasing())
                        .setUnquotedCasing(config.unquotedCasing())
                        .setQuoting(config.quoting())
                        .build());
        System.out.println("context schema is " + context.getRootSchema().getSubSchemaNames());
        SqlNode sqlNode;
        try {
            sqlNode = parser.parseStmt();
        } catch (SqlParseException e){
                throw new RuntimeException("parse failed: " + e.getMessage(), e);
        }

        System.out.println("context default schema is " + this.context.getDefaultDataSource().getSubSchemaNames());

        try{
        return new PrivacyParserResult(stripNamespace(sql, dataSource),
                dataSource,
                sqlNode.getKind(),
                sqlNode,
                determineCheckability(sqlNode),
                true
                );
        } catch (Exception e) {
            throw new SQLException(e);
        }

    }

    public SqlParser getSqlParser(String sql) {
        try {
            final CalciteConnectionConfig config = context.getCfg();
            return SqlParser.create(sql,
                    SqlParser.configBuilder()
                            .setQuotedCasing(config.quotedCasing())
                            .setUnquotedCasing(config.unquotedCasing())
                            .setQuoting(config.quoting())
                            .build());
        } catch (Exception e) {
            return SqlParser.create(sql);
        }
    }

    private String stripNamespace(final String query,
                                  final DataSourceSchema dataSource)
            throws PrivacyException {
        String result = query.replace("\n", " ");
        if (dataSource != null) {
            try {
                final SqlParser parser = getSqlParser(query);
                SqlNode node = parser.parseQuery();
                result = stripNamespace(node, dataSource.getName(),
                        dataSource.getDataSource().getSqlDialect());
            } catch (Exception e) {
                LOG.warn("Exception while parsing the input query: " + e.getMessage());
            }
        }
        return result;
    }

    private String stripNamespace(final SqlNode node,
                                  final String namespace,
                                  final SqlDialect dialect) {
        final SqlNode transformedNode = node.accept(
                new SqlShuttle() {
                    @Override
                    public SqlNode visit(SqlIdentifier id) {
                        if (id.names.size() > 1
                                && id.names.get(0).toUpperCase().equals(namespace.toUpperCase())) {
                            return id.getComponent(1, id.names.size());
                        } else {
                            return id;
                        }
                    }
                });
        String result = transformedNode.toSqlString(dialect).toString();
        return result.replace("\n", " ");
    }

    public SchemaPlus getRootSchma(){
        return context.getRootSchema();
    }

    public JavaTypeFactory getTypeFactory() {
        return context.getTypeFactory();
    }

    //Black Box for what is checkable at the moment in the query
    public boolean determineCheckability(SqlNode sqlNode){
        return true;
    }

    public class PrivacyParserResult extends ParserResult{
        private final DataSourceSchema datasource;
        public PrivacyParserResult(String parsedSql,
                                   DataSourceSchema datasource,
                                   SqlKind kind,
                                   SqlNode sqlNode,
                                   boolean isCheckable,
                                   boolean parseResult){
            super(parsedSql, kind, sqlNode, isCheckable, parseResult);
            this.datasource = datasource;
        }

        public DataSourceSchema getDataSource() {
            return datasource;
        }
    }

}



