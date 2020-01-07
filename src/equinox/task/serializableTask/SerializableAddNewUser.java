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

import java.io.File;

import equinox.serverUtilities.Permission;
import equinox.task.AddNewUser;
import equinox.task.SerializableTask;
import javafx.scene.control.TreeItem;

/**
 * Class for the serializable form of the add new user task.
 *
 * @author Murat Artim
 * @date 4 Apr 2018
 * @time 16:21:24
 */
public class SerializableAddNewUser implements SerializableTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** User info. */
	private final String alias, name, organization, email;

	/** User permissions. */
	private final Permission[] permissions;

	/** Profile image. */
	private final File image;

	/**
	 * Creates serializable form of the add new user task.
	 *
	 * @param alias
	 *            User alias.
	 * @param name
	 *            User name.
	 * @param organization
	 *            User organization.
	 * @param email
	 *            User email.
	 * @param permissions
	 *            User permissions.
	 * @param image
	 *            Profile image (can be null).
	 */
	public SerializableAddNewUser(String alias, String name, String organization, String email, Permission[] permissions, File image) {
		this.alias = alias;
		this.name = name;
		this.organization = organization;
		this.email = email;
		this.permissions = permissions;
		this.image = image;
	}

	@Override
	public AddNewUser getTask(TreeItem<String> fileTreeRoot) {
		return new AddNewUser(alias, name, organization, email, permissions, image == null ? null : image.toPath());
	}
}
