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

import catalog.db.pojo.View;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Mapper for {@link View}
 */
public class ViewMapper implements ResultSetMapper<View> {
  public View map(int index, ResultSet r, StatementContext ctx) throws SQLException {
    return new View(r.getLong("id"), r.getString("name"), r.getString("description"),
        r.getString("query"), r.getLong("cost"), r.getString("table_name"),
        r.getString("schema_name"), r.getLong("destination_id"), r.getString("destination"),
        r.getLong("ds_set_id"));
  }
}
