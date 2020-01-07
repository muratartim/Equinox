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

import equinox.data.ui.RfortPilotPoint;
import equinox.dataServer.remote.data.FatigueMaterial;
import equinox.dataServer.remote.data.LinearMaterial;
import equinox.dataServer.remote.data.PreffasMaterial;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TableCell;

/**
 * Utility class for custom combo box table cell.
 *
 * @author Murat Artim
 * @param <S>
 *            The type of the TableView generic type. This should also match with the first generic type in TableColumn.
 * @param <T>
 *            The type of the item contained within the Cell.
 * @date Mar 3, 2016
 * @time 9:58:12 AM
 */
public class RfortMaterialTableCell<S, T> extends TableCell<S, T> {

	/** Combo box component. */
	private final ComboBox<T> comboBox_;

	/**
	 * Creates custom combobox table cell.
	 *
	 * @param items
	 *            Items of combobox.
	 * @param promtText
	 *            Prompt text.
	 */
	public RfortMaterialTableCell(ObservableList<T> items, String promtText) {

		// create combobox
		comboBox_ = new ComboBox<>(items);

		// set prompt text
		comboBox_.setPromptText(promtText);

		// set maximum width
		comboBox_.setMaxWidth(Double.MAX_VALUE);

		// always show graphics
		setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

		// set change listener
		comboBox_.valueProperty().addListener((ChangeListener<T>) (obs, oldValue, newValue) -> {
			RfortPilotPoint item = (RfortPilotPoint) getTableView().getItems().get(getIndex());
			if (newValue instanceof FatigueMaterial) {
				item.setFatigueMaterial((FatigueMaterial) newValue);
			}
			else if (newValue instanceof PreffasMaterial) {
				item.setPreffasMaterial((PreffasMaterial) newValue);
			}
			else if (newValue instanceof LinearMaterial) {
				item.setLinearMaterial((LinearMaterial) newValue);
			}
		});
	}

	@Override
	public void updateItem(T item, boolean empty) {
		super.updateItem(item, empty);
		if (empty) {
			setGraphic(null);
		}
		else {
			comboBox_.setValue(item);
			setGraphic(comboBox_);
		}
	}
}
