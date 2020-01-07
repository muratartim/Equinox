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

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.time.DurationFormatUtils;

/**
 * Class for user authentication.
 *
 * @author Murat Artim
 * @date 2 Jul 2018
 * @time 00:40:08
 */
public final class UserAuthentication implements Serializable {

	/** Serial ID. */
	private static final long serialVersionUID = 2L;

	/** Number of days to expiry. */
	private static final long NUMBER_OF_DAYS_EXPIRY = 10L;

	/** User alias. */
	private final String alias, username;

	/** List of non-administrative permission names. */
	private final List<String> permissionNames = new ArrayList<>();

	/** Expiry date. */
	private final Instant expiry;

	/**
	 * Creates user authentication.
	 *
	 * @param alias
	 *            User alias. Note that user alias must be 64-bit encoded.
	 * @param username
	 *            Username. Note that username must be 64-bit encoded.
	 */
	public UserAuthentication(String alias, String username) {
		this.alias = alias;
		this.username = username;
		this.expiry = Instant.now().plus(Duration.ofDays(NUMBER_OF_DAYS_EXPIRY));
	}

	/**
	 * Returns the user alias. Note that it is 64-bit encoded.
	 *
	 * @return The user alias. Note that it is 64-bit encoded.
	 */
	public String getAlias() {
		return alias;
	}

	/**
	 * Returns the username. Note that it is 64-bit encoded.
	 *
	 * @return The username. Note that it is 64-bit encoded.
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Returns the date of expiry for this authentication.
	 *
	 * @return The date of expiry for this authentication.
	 */
	public Instant getExpiryDate() {
		return expiry;
	}

	/**
	 * Returns user non-administrative permission names. Note that all permission names are 64-bit encoded.
	 *
	 * @return User non-administrative permission names. Note that all permission names are 64-bit encoded.
	 */
	public List<String> getPermissionNames() {
		return permissionNames;
	}

	/**
	 * Computes and returns formatted user authentication expiry duration.
	 *
	 * @return Formatted user authentication expiry duration.
	 */
	public String getAuthenticationExpiryDuration() {

		// compute duration
		Duration duration = Duration.between(Instant.now(), expiry);

		// expired
		if (duration.isZero() || duration.isNegative())
			return "Expired";

		// get duration milliseconds
		long millis = duration.toMillis();

		// smaller than a second
		if (millis < 1000)
			return millis + " milliseconds";

		// longer than a second
		return DurationFormatUtils.formatDurationWords(duration.toMillis(), true, true);
	}

	/**
	 * Returns true if this authentication is expired.
	 *
	 * @return True if this authentication is expired.
	 */
	public boolean isExpired() {
		return expiry.isBefore(Instant.now());
	}

	/**
	 * Adds given non-administrative user permission name. Note that the permission name must be 64-bit encoded.
	 *
	 * @param permissionName
	 *            Non-administrative user permission name. Note that the permission name must be 64-bit encoded.
	 */
	public void addPermissionName(String permissionName) {
		permissionNames.add(permissionName);
	}
}