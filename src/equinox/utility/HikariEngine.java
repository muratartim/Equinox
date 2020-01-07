/*
 * Copyright 2018 Murat Artim (muratartim@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package equinox.utility;

import java.sql.Connection;
import java.sql.SQLException;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Class for Hikari database connection pool.
 *
 * @author Murat Artim
 * @date 13 May 2018
 * @time 22:03:09
 */
public class HikariEngine extends HikariDataSource {

	/** Engine name. */
	public static final String NAME = "Hikari";

	/**
	 * Creates Hikari database connection pool.
	 *
	 * @param config
	 *            Hikari configuration object.
	 */
	public HikariEngine(HikariConfig config) {
		super(config);
	}

	@Override
	public Connection getConnection() throws SQLException {

		// allow dirty reads
		Connection conn = super.getConnection();
		conn.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
		return conn;
	}
}
