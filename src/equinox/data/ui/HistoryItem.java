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
package equinox.data.ui;

import equinox.font.IconicFont;
import javafx.concurrent.Worker.State;
import javafx.scene.control.Label;

/**
 * Class for task history item.
 *
 * @author Murat Artim
 * @date Nov 10, 2014
 * @time 2:42:40 PM
 */
public class HistoryItem {

	/** Item name and duration. */
	private final String name_, duration_;

	/** Item status. */
	private final Label status_;

	/**
	 * Creates history item.
	 *
	 * @param title
	 *            Title of item.
	 * @param duration
	 *            Duration of task.
	 * @param state
	 *            Final status of item.
	 */
	public HistoryItem(String title, String duration, State state) {

		// set task name
		name_ = title;

		// set task duration
		duration_ = duration;

		// set task state
		if (state.equals(State.SUCCEEDED)) {
			status_ = new Label("\uf046");
		}
		else if (state.equals(State.FAILED)) {
			status_ = new Label("\uf071");
		}
		else if (state.equals(State.CANCELLED)) {
			status_ = new Label("\uf05e");
		}
		else {
			status_ = null;
		}
		if (status_ != null) {
			status_.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
		}
	}

	/**
	 * Returns name of item.
	 *
	 * @return Name of item.
	 */
	public String getName() {
		return name_;
	}

	/**
	 * Returns duration of task.
	 *
	 * @return Duration.
	 */
	public String getDuration() {
		return duration_;
	}

	/**
	 * Returns status of item.
	 *
	 * @return Status of item.
	 */
	public Label getStatus() {
		return status_;
	}

	@Override
	public String toString() {
		return name_ + " (in " + duration_ + ")";
	}
}
