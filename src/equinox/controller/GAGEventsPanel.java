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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ResourceBundle;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;

import equinox.data.EquinoxTheme;
import equinox.data.GAGEvent;
import equinox.plugin.FileType;
import equinox.task.SaveImage;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.WindowEvent;

/**
 * Class for GAG events panel controller.
 *
 * @author Murat Artim
 * @date Jun 2, 2015
 * @time 11:49:22 AM
 */
public class GAGEventsPanel implements Initializable, ListChangeListener<GAGEvent> {

	/** The owner panel. */
	private DamageContributionViewPanel owner_;

	/** Pop-over. */
	private PopOver popOver_;

	/** Showing indicator. */
	private boolean isShown_ = false;

	/** Decimal format. */
	private final DecimalFormat format_ = new DecimalFormat("0.##");

	@FXML
	private VBox root_, container_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// no implementation
	}

	@Override
	public void onChanged(javafx.collections.ListChangeListener.Change<? extends GAGEvent> c) {

		// remove all current events
		container_.getChildren().clear();

		// add new events
		ObservableList<? extends GAGEvent> list = c.getList();
		int size = list.size();
		for (int i = 0; i < size; i++) {

			// get event
			GAGEvent event = list.get(i);

			// add header
			HBox row = new HBox();
			row.setAlignment(Pos.CENTER_LEFT);
			row.setSpacing(10);
			row.setPadding(new Insets(5, 5, 5, 5));
			if ((i % 2) != 0) {
				row.setStyle("-fx-background-color:gainsboro");
			}
			container_.getChildren().add(row);

			StackPane stack = new StackPane();
			row.getChildren().add(stack);
			ProgressBar rating1 = new ProgressBar(event.getRating());
			rating1.setPrefWidth(55);
			rating1.setMinWidth(Label.USE_PREF_SIZE);
			rating1.setMaxWidth(Label.USE_PREF_SIZE);
			stack.getChildren().add(rating1);
			Label rating2 = new Label(format_.format(event.getRating() * 100.0) + "%");
			rating2.setStyle("-fx-font-size:11; -fx-text-fill:orange");
			stack.getChildren().add(rating2);

			Label eventName1 = new Label(event.getEvent());
			eventName1.setPrefWidth(150);
			eventName1.setMinWidth(Label.USE_PREF_SIZE);
			eventName1.setMaxWidth(Label.USE_PREF_SIZE);
			row.getChildren().add(eventName1);

			Label loadCase1 = new Label(event.getIssyCode());
			loadCase1.setPrefWidth(65);
			loadCase1.setMinWidth(Label.USE_PREF_SIZE);
			loadCase1.setMaxWidth(Label.USE_PREF_SIZE);
			row.getChildren().add(loadCase1);

			Label segment = new Label(event.getSegment().toString());
			segment.setPrefWidth(65);
			segment.setMinWidth(Label.USE_PREF_SIZE);
			segment.setMaxWidth(Label.USE_PREF_SIZE);
			row.getChildren().add(segment);

			Label comment1 = new Label(event.getComment());
			comment1.setPrefWidth(300);
			comment1.setMinWidth(Label.USE_PREF_SIZE);
			comment1.setMaxWidth(Label.USE_PREF_SIZE);
			row.getChildren().add(comment1);

			Label type = new Label(event.getType() ? "Maximum" : "Minimum");
			type.setPrefWidth(95);
			type.setMinWidth(Label.USE_PREF_SIZE);
			type.setMaxWidth(Label.USE_PREF_SIZE);
			row.getChildren().add(type);
		}
	}

	/**
	 * Shows chat panel.
	 *
	 * @param button
	 *            Button.
	 */
	public void show(Hyperlink button) {

		// not shown
		if (!isShown_) {

			// create pop-over
			popOver_ = new PopOver();
			popOver_.setArrowLocation(ArrowLocation.TOP_RIGHT);
			popOver_.setDetachable(false);
			popOver_.setContentNode(root_);
			popOver_.setHideOnEscape(true);
			popOver_.setAutoHide(true);

			// set showing handler
			popOver_.setOnShowing(new EventHandler<WindowEvent>() {

				@Override
				public void handle(WindowEvent event) {
					isShown_ = true;
				}
			});

			// set hidden handler
			popOver_.setOnHidden(new EventHandler<WindowEvent>() {

				@Override
				public void handle(WindowEvent event) {
					isShown_ = false;
				}
			});

			// show
			popOver_.show(button);
		}
	}

	@FXML
	private void onSaveClicked() {

		// get chart title
		String title = "GAG Events";

		// get file chooser
		FileChooser fileChooser = owner_.getOwner().getOwner().getFileChooser(FileType.PNG.getExtensionFilter());

		// show save dialog
		fileChooser.setInitialFileName(title + FileType.PNG.getExtension());
		File selectedFile = fileChooser.showSaveDialog(owner_.getOwner().getOwner().getOwner().getStage());

		// no file selected
		if (selectedFile == null)
			return;

		// set initial directory
		owner_.getOwner().getOwner().setInitialDirectory(selectedFile);

		// append extension if necessary
		File file = FileType.appendExtension(selectedFile, FileType.PNG);

		// take snapshot of the scene
		WritableImage snapshot = container_.snapshot(null, null);

		// create and start task in progress panel
		owner_.getOwner().getOwner().getActiveTasksPanel().runTaskInParallel(new SaveImage(file, snapshot));
	}

	/**
	 * Loads and returns plot column panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded plot column panel.
	 */
	public static GAGEventsPanel load(DamageContributionViewPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("GAGEventsPanel.fxml"));
			fxmlLoader.load();

			// get controller
			GAGEventsPanel controller = (GAGEventsPanel) fxmlLoader.getController();
			controller.owner_ = owner;

			// return controller
			return controller;
		}

		// exception occurred during loading
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
