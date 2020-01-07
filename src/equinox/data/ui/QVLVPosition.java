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

import javafx.beans.property.SimpleStringProperty;

/**
 * Class for frame/stringer position data.
 *
 * @author Murat Artim
 * @date Aug 4, 2015
 * @time 11:37:37 AM
 */
public class QVLVPosition {

	/** Element ID, frame and stringer positions. */
	private final SimpleStringProperty framepos = new SimpleStringProperty(), stringerpos = new SimpleStringProperty();

	/**
	 * Creates frame/stringer position.
	 *
	 * @param framepos
	 *            Frame position.
	 * @param stringerpos
	 *            Stringer position.
	 */
	public QVLVPosition(String framepos, String stringerpos) {
		this.framepos.set(framepos);
		this.stringerpos.set(stringerpos);
	}

	/**
	 * Returns frame position.
	 *
	 * @return Frame position.
	 */
	public String getFramepos() {
		return this.framepos.get();
	}

	/**
	 * Returns stringer position.
	 *
	 * @return Stringer position.
	 */
	public String getStringerpos() {
		return this.stringerpos.get();
	}

	/**
	 * Sets frame position.
	 *
	 * @param framepos
	 *            Frame position.
	 */
	public void setFramepos(String framepos) {
		this.framepos.set(framepos);
	}

	/**
	 * Sets stringer position.
	 *
	 * @param stringerpos
	 *            Stringer position.
	 */
	public void setStringerpos(String stringerpos) {
		this.stringerpos.set(stringerpos);
	}

	@Override
	public String toString() {
		return this.framepos.get() + " - " + this.stringerpos.get();
	}
}
