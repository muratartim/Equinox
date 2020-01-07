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
package equinox.plugin;

import java.time.Duration;
import java.time.Instant;

import org.apache.commons.lang3.time.DurationFormatUtils;

import equinox.Equinox;
import equinox.serverUtilities.Permission;
import equinox.utility.exception.PermissionDeniedException;
import javafx.concurrent.Task;

/**
 * Class for Equinox task.
 *
 * @author Murat Artim
 * @param <V>
 *            Task output class.
 * @date Mar 27, 2015
 * @time 2:37:52 PM
 */
public abstract class EquinoxTask<V> extends Task<V> {

	/** Start time. */
	private Instant start_;

	/** Task duration. */
	private String duration_;

	/**
	 * Creates Equinox task.
	 */
	public EquinoxTask() {

		// set start time when task is running
		setOnRunning((p) -> start_ = Instant.now());

		// compute duration when task completed (in any way)
		setOnCancelled((p) -> computeAndFormatDuration());
		setOnFailed((p) -> computeAndFormatDuration());
		setOnSucceeded((p) -> computeAndFormatDuration());
	}

	@Override
	public void updateTitle(String title) {
		super.updateTitle(title);
	}

	@Override
	public void updateMessage(String message) {
		super.updateMessage(message);
	}

	@Override
	public void updateProgress(long workDone, long max) {
		super.updateProgress(workDone, max);
	}

	/**
	 * Returns duration or null if timer hasn't been started.
	 *
	 * @return Duration or null if timer hasn't been started.
	 */
	public String getDuration() {
		return duration_;
	}

	/**
	 * Checks whether the current user has the given permission to run this task. This method should be called at the beginning of the <code>call</code> method of this task.
	 *
	 * @param permission
	 *            Permission to be checked.
	 * @throws PermissionDeniedException
	 *             If user does not have sufficient privileges to run this task.
	 */
	protected void checkPermission(Permission permission) throws PermissionDeniedException {
		if (!Equinox.USER.hasPermission(permission, false, null))
			throw new PermissionDeniedException(permission);
	}

	/**
	 * Computes and formats task duration.
	 */
	private void computeAndFormatDuration() {

		// compute duration
		Duration duration = start_ == null ? null : Duration.between(start_, Instant.now());

		// no duration available
		if (duration == null) {
			duration_ = "N/A";
		}

		// duration available
		else {

			// get duration milliseconds
			long millis = duration.toMillis();

			// smaller than a second
			if (millis < 1000) {
				duration_ = millis + " milliseconds";
			}

			// longer than a second
			else {
				duration_ = DurationFormatUtils.formatDurationWords(duration.toMillis(), true, true);
			}
		}
	}
}
