package execution;

import com.google.common.cache.Cache;

import planner.DataSourceSchema;

import sql.ParserResult;
import plugin.Executor;
import plugin.jdbc.JdbcDB;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sql.PrivacyParser;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Created by adeshr on 5/24/16.P
 */
public class PrivacyDMLExecutor implements PrivacyExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(PrivacyQueryExecutor.class);

    public static final int QUERY_TIMEOUT = 60;

    private final Cache<String, Connection> connectionCache;

    public PrivacyDMLExecutor(Cache<String, Connection> connectionCache) {
        this.connectionCache = connectionCache;
    }


    public PreparedStatement prepare(ParserResult parserResult) throws Exception {
        final DataSourceSchema dataSourceSchema =
                ((PrivacyParser.PrivacyParserResult) parserResult).getDataSource();
        PreparedStatement p = null;
        if (dataSourceSchema != null) {
            Connection conn;
            final String id = dataSourceSchema.getName();
            Executor executor = (Executor) dataSourceSchema.getDataSource();
            String parsedSql = parserResult.getParsedSql();

            if (executor instanceof JdbcDB) {
                conn = getExecutorConnection(id, executor);
                p = conn.prepareStatement(parsedSql);
            }
        }
        return p;
    }

    public Object execute(ParserResult parserResult) throws Exception {
        Object object = null;
        final DataSourceSchema dataSourceSchema =
                ((PrivacyParser.PrivacyParserResult) parserResult).getDataSource();

        if (dataSourceSchema != null) {
            Connection conn;
            final String id = dataSourceSchema.getName();
            Executor executor = (Executor) dataSourceSchema.getDataSource();
            String parsedSql = parserResult.getParsedSql();

            LOG.info("Execute query[" + parsedSql + "]");

            if (executor instanceof JdbcDB) {
                conn = getExecutorConnection(id, executor);
                Statement statement = conn.createStatement();
                try {
                    statement.setQueryTimeout(QUERY_TIMEOUT);
                } catch (Exception e) {
                    LOG.warn("Could not set Query Timeout to " + QUERY_TIMEOUT + " seconds", e);
                }

                object = statement.executeUpdate(parsedSql);
            }
        }
        return object;
    }

    private Connection getExecutorConnection(String id, Executor executor)
            throws SQLException, ClassNotFoundException {
        Connection conn;
        if (this.connectionCache.asMap().containsKey(id)) {
            conn = this.connectionCache.getIfPresent(id);
            if (conn.isClosed()) {
                conn = ((JdbcDB) executor).getConnection();
                this.connectionCache.put(id, conn);
            }
        } else {
            conn = ((JdbcDB) executor).getConnection();
            this.connectionCache.put(id, conn);
        }

        return conn;
    }
}

