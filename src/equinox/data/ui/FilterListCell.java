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

import javafx.scene.control.ListCell;

/**
 * Class for file filter list cell.
 *
 * @author Murat Artim
 * @date May 7, 2016
 * @time 12:02:59 AM
 */
public class FilterListCell extends ListCell<FilterItem> {

	@Override
	public void updateItem(FilterItem item, boolean empty) {

		// update item
		super.updateItem(item, empty);

		// empty cell
		if (empty || item == null) {
			setText(null);
			setGraphic(null);
			setOnDragOver(null);
			setOnDragEntered(null);
			setOnDragExited(null);
			setOnDragDropped(null);
		}

		// valid cell
		else {

			// set text
			FilterItem file = getItem();
			setText(file.getName());

			// set icon
			setGraphic(file.getIcon());
		}
	}
}
