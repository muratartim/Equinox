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
package equinox.task;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.ExecutionException;

import com.itextpdf.xmp.impl.Base64;

import equinox.Equinox;
import equinox.data.User;
import equinox.data.UserAuthentication;
import equinox.task.InternalEquinoxTask.ShortRunningTask;

/**
 * Class for save user authentication task.
 *
 * @author Murat Artim
 * @date 2 Jul 2018
 * @time 01:35:11
 */
public class SaveUserAuthentication extends InternalEquinoxTask<UserAuthentication> implements ShortRunningTask {

	/** User. */
	private final User user;

	/**
	 * Creates save user authentication task.
	 *
	 * @param user
	 *            User.
	 */
	public SaveUserAuthentication(User user) {
		this.user = user;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Save user authentication";
	}

	@Override
	protected UserAuthentication call() throws Exception {

		// encrypt user attributes
		String alias = Base64.encode(user.getAlias());
		String username = Base64.encode(user.getUsername());

		// create user authentication
		UserAuthentication userAuth = new UserAuthentication(alias, username);

		// encrypt and add permissions
		user.getPermissions().forEach(x -> userAuth.addPermissionName(Base64.encode(x.toString())));

		// write to disk
		try (ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(Equinox.USER_AUTHENTICATION_FILE.toFile())))) {
			out.writeObject(userAuth);
		}

		// return authentication
		return userAuth;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set authentication end date to UI
		try {
			UserAuthentication userAuth = get();
			if (userAuth != null) {
				taskPanel_.getOwner().getOwner().getInputPanel().getAuthenticationButton().setUserData(userAuth);
				taskPanel_.getOwner().getOwner().getInputPanel().authenticationStatusChanged(userAuth.isExpired());
			}
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}
}