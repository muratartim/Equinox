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
package equinox.data;

import java.util.ArrayList;
import java.util.List;

import equinox.controller.MainScreen;
import equinox.exchangeServer.remote.data.ExchangeUser;
import equinox.serverUtilities.Permission;

/**
 * Class for Equinox user.
 *
 * @author Murat Artim
 * @date 3 Aug 2016
 * @time 14:29:56
 */
public class User {

	/** User alias. This is the OS user account name. */
	private final String alias;

	/** Username. */
	private String username;

	/** List of permissions. */
	private final List<Permission> permissions;

	/** True if this user is an administrator. */
	private boolean isAdministrator, isLoggedAsAdministrator;

	/**
	 * Creates Equinox user.
	 *
	 * @param alias
	 *            User alias. This is the OS user account name.
	 */
	public User(String alias) {
		this.alias = alias.toUpperCase();
		username = this.alias;
		permissions = new ArrayList<>();
		isAdministrator = false;
		isLoggedAsAdministrator = false;
	}

	/**
	 * Creates and returns exchange user.
	 * 
	 * @return Newly created exchange user.
	 */
	public ExchangeUser createExchangeUser() {
		return new ExchangeUser(alias, username);
	}

	/**
	 * Returns user alias. This is the OS user account name.
	 *
	 * @return User alias.
	 */
	public String getAlias() {
		return alias;
	}

	/**
	 * Returns username. This will be the same as user alias if the user is not a registered user.
	 *
	 * @return Username.
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Returns permissions of this client.
	 *
	 * @return Permissions of this client.
	 */
	public List<Permission> getPermissions() {
		return permissions;
	}

	/**
	 * Returns true if this user is an administrator.
	 *
	 * @return True if this user is an administrator.
	 */
	public boolean isAdministrator() {
		return isAdministrator;
	}

	/**
	 * Returns true if this user is logged in as administrator.
	 *
	 * @return True if this user is logged in as administrator.
	 */
	public boolean isLoggedAsAdministrator() {
		return isLoggedAsAdministrator;
	}

	/**
	 * Returns true if this client has given permission.
	 *
	 * @param permission
	 *            Permission to check.
	 * @param showWarning
	 *            True if warning message should be shown in case the user doesn't have the given permission.
	 * @param mainScreen
	 *            The owner main screen. Null can be given if no warning message is needed.
	 * @return True if this client has given permission.
	 */
	public boolean hasPermission(Permission permission, boolean showWarning, MainScreen mainScreen) {

		// has permission
		if (permissions.contains(permission))
			return true;

		// show warning
		if (showWarning) {
			mainScreen.getNotificationPane().showPermissionDenied(permission);
		}

		// cannot perform
		return false;
	}

	/**
	 * Sets username.
	 *
	 * @param username
	 *            Username.
	 */
	public void setUsername(String username) {
		this.username = username.toUpperCase();
	}

	/**
	 * Sets this user as administrator.
	 *
	 * @param isAdministrator
	 *            True to set this user as administrator.
	 */
	public void setAsAdministrator(boolean isAdministrator) {
		this.isAdministrator = isAdministrator;
	}

	/**
	 * Sets this user logged in as administrator.
	 */
	public void loginAsAdministrator() {
		isLoggedAsAdministrator = true;
	}

	/**
	 * Sets this user logged out as administrator.
	 */
	public void logoutAsAdministrator() {

		// set logged out
		isLoggedAsAdministrator = false;

		// remove administrator permissions
		ArrayList<Permission> toBeRemoved = new ArrayList<>();
		permissions.forEach(x -> {
			if (x.isAdminPermission()) {
				toBeRemoved.add(x);
			}
		});
		permissions.removeAll(toBeRemoved);
	}

	/**
	 * Adds given permission to list of permissions.
	 *
	 * @param permissionName
	 *            Name of permission to add.
	 * @return If the permission is added.
	 */
	public boolean addPermission(String permissionName) {

		try {

			// get permission
			Permission permission = Permission.valueOf(permissionName);

			// not found
			if (permission == null)
				return false;

			// add permission
			if (!permissions.contains(permission)) {
				permissions.add(permission);
			}
			return true;
		}

		// permission not recognized
		catch (Exception e) {
			return false;
		}
	}
}
