/*
 * Copyright (c) 2015. Qubole Inc
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package catalog.db.mapper;

import catalog.db.pojo.DataSource;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Mapper for {@link DataSource}
 */
public class DataSourceMapper implements ResultSetMapper<DataSource> {
  public DataSource map(int index, ResultSet r, StatementContext ctx) throws SQLException {
    return new DataSource(r.getInt("id"), r.getString("type"), r.getString("name"),
        r.getString("datasource_type"), r.getString("url"), r.getInt("ds_set_id"));
  }
}
