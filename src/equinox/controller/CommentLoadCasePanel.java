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

import equinox.data.EquinoxTheme;
import equinox.data.fileType.AircraftLoadCase;
import equinox.task.GetLoadCaseComments;
import equinox.task.SetLoadCaseComments;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.WindowEvent;

/**
 * Class for comment load case panel controller.
 *
 * @author Murat Artim
 * @date Sep 18, 2015
 * @time 3:50:13 PM
 */
public class CommentLoadCasePanel implements Initializable {

	/** The owner panel. */
	private InputPanel owner_;

	/** The owner pop-over. */
	private PopOver popOver_;

	/** Load case. */
	private AircraftLoadCase loadCase_;

	@FXML
	private VBox root_;

	@FXML
	private TextField comments_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// no implementation
	}

	/**
	 * Sets load case comments to this panel.
	 *
	 * @param comments
	 *            Comments.
	 */
	public void setComments(String comments) {
		comments_.setText(comments);
	}

	@FXML
	private void onOkClicked() {

		// get comments
		String comments = comments_.getText();

		// hide pop-over
		popOver_.hide();

		// set comments
		owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new SetLoadCaseComments(loadCase_, comments));
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
	 * @param loadCase
	 *            Load case.
	 * @return The newly loaded file CDF set panel.
	 */
	public static VBox load(PopOver popOver, InputPanel owner, AircraftLoadCase loadCase) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("CommentLoadCasePanel.fxml"));
			fxmlLoader.load();

			// get controller
			CommentLoadCasePanel controller = (CommentLoadCasePanel) fxmlLoader.getController();

			// set attributes
			controller.popOver_ = popOver;
			controller.owner_ = owner;
			controller.loadCase_ = loadCase;

			// get load case comments
			controller.popOver_.setOnShowing(new EventHandler<WindowEvent>() {

				@Override
				public void handle(WindowEvent event) {
					controller.owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new GetLoadCaseComments(controller.loadCase_, controller));
				}
			});

			// return controller
			return controller.root_;
		}

		// exception occurred during loading
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
