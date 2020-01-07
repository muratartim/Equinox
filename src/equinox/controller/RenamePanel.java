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
package equinox.controller;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;

import equinox.data.EquinoxTheme;
import equinox.data.fileType.SpectrumItem;
import equinox.task.RenameFile;
import equinox.utility.Utility;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.VBox;

/**
 * Class for rename panel controller.
 *
 * @author Murat Artim
 * @date Jul 13, 2014
 * @time 5:53:56 PM
 */
public class RenamePanel implements Initializable {

	/** The owner panel. */
	private InputPanel owner_;

	/** The owner pop-over. */
	private PopOver popOver_;

	@FXML
	private VBox root_;

	@FXML
	private TextField rename_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// no implementation
	}

	@FXML
	private void onOkClicked() {

		// get name
		String name = rename_.getText().trim();

		// no name
		if ((name == null) || name.isEmpty()) {
			String message = "Please enter a name to continue.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(rename_);
			return;
		}

		// invalid name
		String warning = Utility.isValidFileName(name);
		if (warning != null) {
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(warning, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(rename_);
			return;
		}

		// hide pop-over
		popOver_.hide();

		// loop over selected files
		ObservableList<TreeItem<String>> selected = owner_.getSelectedFiles();
		for (TreeItem<String> item : selected) {
			owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new RenameFile((SpectrumItem) item, name));
		}
	}

	@FXML
	private void onCancelClicked() {
		popOver_.hide();
	}

	/**
	 * Loads and returns file CDF set panel.
	 *
	 * @param popOver
	 *            The owner pop-over.
	 * @param owner
	 *            The owner panel.
	 * @param name
	 *            Name of item.
	 * @return The newly loaded file CDF set panel.
	 */
	public static VBox load(PopOver popOver, InputPanel owner, String name) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("RenamePanel.fxml"));
			fxmlLoader.load();

			// get controller
			RenamePanel controller = (RenamePanel) fxmlLoader.getController();

			// set attributes
			controller.popOver_ = popOver;
			controller.owner_ = owner;
			controller.rename_.setText(name);
			controller.rename_.selectAll();

			// return controller
			return controller.root_;
		}

		// exception occurred during loading
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
