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

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

import decoder.Base64Decoder;
import equinox.Equinox;
import equinox.data.UserAuthentication;
import equinox.task.InternalEquinoxTask.ShortRunningTask;

/**
 * Class for load user authentication task.
 *
 * @author Murat Artim
 * @date 2 Jul 2018
 * @time 01:12:30
 */
public class LoadUserAuthentication extends InternalEquinoxTask<UserAuthentication> implements ShortRunningTask {

	@Override
	public String getTaskTitle() {
		return "Loading user authentication...";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	protected UserAuthentication call() throws Exception {

		// file doesn't exist or cannot read
		if (!Files.exists(Equinox.USER_AUTHENTICATION_FILE))
			return null;

		// read and return user authentication
		try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(Equinox.USER_AUTHENTICATION_FILE.toFile())))) {
			return (UserAuthentication) in.readObject();
		}

		// cannot read due to version
		catch (Exception e) {
			Equinox.LOGGER.log(Level.WARNING, "Exception occurred during reading user authentication.", e);
			return null;
		}
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set user permissions and user name
		try {

			// get authentication
			UserAuthentication userAuth = get();

			// no authentication found
			if (userAuth == null)
				return;

			// aliases don't match
			if (!Equinox.USER.getAlias().equals(Base64Decoder.decodeString(userAuth.getAlias())))
				return;

			// already expired
			if (userAuth.isExpired())
				return;

			// set username
			Equinox.USER.setUsername(Base64Decoder.decodeString(userAuth.getUsername()));

			// add user permissions
			List<String> permissions = userAuth.getPermissionNames();
			for (String permission : permissions) {
				Equinox.USER.addPermission(Base64Decoder.decodeString(permission));
			}

			// set authentication to UI
			taskPanel_.getOwner().getOwner().getInputPanel().getAuthenticationButton().setUserData(userAuth);
			taskPanel_.getOwner().getOwner().getInputPanel().authenticationStatusChanged(userAuth.isExpired());
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}
}