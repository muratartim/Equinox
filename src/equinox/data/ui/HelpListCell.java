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
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TreeItem;

/**
 * Class for help list cell.
 *
 * @author Murat Artim
 * @date Mar 26, 2016
 * @time 7:38:21 PM
 */
public class HelpListCell extends ListCell<TreeItem<String>> {

	@Override
	public void updateItem(TreeItem<String> item, boolean empty) {

		// update item
		super.updateItem(item, empty);

		// empty cell
		if (empty || item == null) {
			setText(null);
			setGraphic(null);
		}

		// valid cell
		else {

			// set text
			HelpItem helpItem = (HelpItem) getItem();
			setText(helpItem.toString());

			// set icon
			Label gr = helpItem.getIcon();
			Label iconLabel = new Label(gr.getText());
			iconLabel.getStylesheets().add(IconicFont.ICOMOON.getStyleSheet());
			setGraphic(iconLabel);
		}
	}
}
