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
package equinox.task.serializableTask;

import equinox.task.ExecuteSQLStatement;
import equinox.task.SerializableTask;
import javafx.scene.control.TreeItem;

/**
 * Class for the serializable form of execute SQL statement task.
 *
 * @author Murat Artim
 * @date Oct 13, 2015
 * @time 2:26:40 PM
 */
public class SerializableExecuteSQLStatement implements SerializableTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** SQL statement. */
	private final String sql_;

	/**
	 * Creates execute SQL statement task.
	 *
	 * @param sql
	 *            SQL statement.
	 */
	public SerializableExecuteSQLStatement(String sql) {
		sql_ = sql;
	}

	@Override
	public ExecuteSQLStatement getTask(TreeItem<String> fileTreeRoot) {
		return new ExecuteSQLStatement(sql_);
	}
}
