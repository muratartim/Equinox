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

import equinox.serverUtilities.Permission;
import equinox.task.EditUserPermissions;
import equinox.task.SerializableTask;
import javafx.scene.control.TreeItem;

/**
 * Class for the serializable form of edit user permissions task.
 *
 * @author Murat Artim
 * @date 6 Apr 2018
 * @time 12:01:19
 */
public class SerializableEditUserPermissions implements SerializableTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** User info. */
	private final String alias;

	/** User permissions. */
	private final Permission[] permissions;

	/**
	 * Creates serializable form of the add new user task.
	 *
	 * @param alias
	 *            User alias.
	 * @param permissions
	 *            User permissions.
	 */
	public SerializableEditUserPermissions(String alias, Permission[] permissions) {
		this.alias = alias;
		this.permissions = permissions;
	}

	@Override
	public EditUserPermissions getTask(TreeItem<String> fileTreeRoot) {
		return new EditUserPermissions(alias, permissions);
	}
}
