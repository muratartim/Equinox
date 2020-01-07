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

import javafx.scene.control.TreeCell;

/**
 * Class for customized help tree cell.
 *
 * @author Murat Artim
 * @date Jan 20, 2014
 * @time 1:32:11 PM
 */
public final class HelpTreeCell extends TreeCell<String> {

	@Override
	public void updateItem(String item, boolean empty) {

		// update item
		super.updateItem(item, empty);

		// empty cell
		if (empty) {
			setText(null);
			setGraphic(null);
		}

		// valid cell
		else {
			HelpItem helpItem = (HelpItem) getTreeItem();
			setText(helpItem.toString());
			setGraphic(helpItem.getIcon());
		}
	}
}
