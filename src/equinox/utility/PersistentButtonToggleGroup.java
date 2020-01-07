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
package equinox.utility;

import javafx.collections.ListChangeListener;
import javafx.event.EventHandler;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseEvent;

/**
 * Utility class for persistent toggle group.
 *
 * @author Murat Artim
 * @date Sep 16, 2015
 * @time 1:37:08 PM
 */
public class PersistentButtonToggleGroup extends ToggleGroup {

	/**
	 * Creates persistent toggle group.
	 *
	 */
	public PersistentButtonToggleGroup() {

		// create toggle group
		super();

		// add change listener
		getToggles().addListener(new ListChangeListener<Toggle>() {

			@Override
			public void onChanged(Change<? extends Toggle> c) {

				// loop over toggles
				while (c.next()) {

					// loop over added toggles
					for (final Toggle addedToggle : c.getAddedSubList()) {

						// add event handler
						((ToggleButton) addedToggle).addEventFilter(MouseEvent.MOUSE_RELEASED, new EventHandler<MouseEvent>() {

							@Override
							public void handle(MouseEvent mouseEvent) {
								if (addedToggle.equals(getSelectedToggle()))
									mouseEvent.consume();
							}
						});
					}
				}
			}
		});
	}
}
