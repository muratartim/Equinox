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
import java.util.ArrayList;
import java.util.ResourceBundle;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;

import equinox.Equinox;
import equinox.data.EquinoxTheme;
import equinox.exchangeServer.remote.data.ExchangeUser;
import equinox.exchangeServer.remote.message.StatusChange;
import equinox.plugin.ViewSubPanel;
import equinox.task.ShareView;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;

/**
 * Class for share view panel controller.
 *
 * @author Murat Artim
 * @date Sep 19, 2014
 * @time 4:32:40 PM
 */
public class ShareViewPanel implements Initializable, ListChangeListener<ExchangeUser> {

	/** The owner panel. */
	private ViewPanel owner_;

	/** Pop-over. */
	private PopOver popOver_;

	/** Showing indicator. */
	private boolean isShown_ = false;

	@FXML
	private VBox root_, container_;

	@FXML
	private Button share_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// no implementation
	}

	@Override
	public void onChanged(javafx.collections.ListChangeListener.Change<? extends ExchangeUser> c) {

		// get currently selected recipients
		ArrayList<ExchangeUser> selected = getSelectedRecipients();

		// remove all current recipients
		container_.getChildren().clear();

		// add new recipients
		ObservableList<? extends ExchangeUser> list = c.getList();
		int size = list.size();
		for (int i = 0; i < size; i++) {
			ExchangeUser recipient = list.get(i);
			ToggleButton button = new ToggleButton(recipient.toString());
			button.setUserData(recipient);
			button.setMaxWidth(Double.MAX_VALUE);
			if (size == 1) {
				button.getStylesheets().add(Equinox.class.getResource("css/ToggleButton2.css").toString());
			}
			else {
				if (i == size - 1) {
					button.getStylesheets().add(Equinox.class.getResource("css/ToggleButton2.css").toString());
				}
				else {
					button.getStylesheets().add(Equinox.class.getResource("css/ToggleButton.css").toString());
				}
			}
			if (selected.contains(recipient)) {
				button.setSelected(true);
			}
			container_.getChildren().add(button);
		}
	}

	/**
	 * Shows chat panel.
	 *
	 */
	public void show() {

		// not shown
		if (!isShown_) {

			// no recipient
			if (container_.getChildren().isEmpty()) {
				String message = "There is no available user to share your view.";
				PopOver popOver = new PopOver();
				popOver.setArrowLocation(ArrowLocation.TOP_RIGHT);
				popOver.setDetachable(false);
				popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.INFO));
				popOver.setHideOnEscape(true);
				popOver.setAutoHide(true);
				popOver.show(owner_.getShareButton());
				return;
			}

			// create pop-over
			popOver_ = new PopOver();
			popOver_.setArrowLocation(ArrowLocation.TOP_CENTER);
			popOver_.setDetachable(false);
			popOver_.setContentNode(root_);
			popOver_.setHideOnEscape(true);
			popOver_.setAutoHide(true);

			// set showing handler
			popOver_.setOnShowing(event -> isShown_ = true);

			// set hidden handler
			popOver_.setOnHidden(event -> isShown_ = false);

			// clear all selections
			for (Node recipient : container_.getChildren()) {
				((ToggleButton) recipient).setSelected(false);
			}

			// show
			popOver_.show(owner_.getShareButton());
		}
	}

	/**
	 * Returns true if this panel is shown.
	 *
	 * @return True if this panel is shown.
	 */
	public boolean isShown() {
		return isShown_;
	}

	@FXML
	private void onShareClicked() {

		// get inputs
		ArrayList<ExchangeUser> selected = getSelectedRecipients();

		// check inputs
		if (!checkInputs(selected))
			return;

		// create share view task
		ViewSubPanel panel = owner_.getSubPanel(owner_.getCurrentSubPanelIndex());
		String name = panel.getViewName();
		WritableImage image = panel.getViewImage();
		ShareView task = new ShareView(name, image, selected);

		// add to progress panel and start task
		owner_.getOwner().getActiveTasksPanel().runTaskInParallel(task);

		// hide pop-over
		popOver_.hide();
	}

	/**
	 * Checks message inputs and displays warning message if needed.
	 *
	 * @param selected
	 *            Selected recipients to share.
	 * @return True if message is acceptable.
	 */
	private boolean checkInputs(ArrayList<ExchangeUser> selected) {

		// this user is not available
		if (!owner_.getOwner().isAvailable()) {

			// create confirmation action
			PopOver popOver = new PopOver();
			EventHandler<ActionEvent> handler = event -> {
				owner_.getOwner().getExchangeServerManager().sendMessage(new StatusChange(Equinox.USER.createExchangeUser(), true));
				popOver.hide();
			};

			// show question
			String warning = "Your status is currently set to 'Busy'. Would you like to set it to 'Available' to share view?";
			popOver.setArrowLocation(ArrowLocation.TOP_RIGHT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel2.load(popOver, warning, 50, "Yes", handler, NotificationPanel2.QUESTION));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(share_);
			return false;
		}

		// no recipients
		else if (selected.isEmpty()) {
			String warning = "Please select at least 1 recipient to share your view.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_RIGHT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(warning, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(share_);
			return false;
		}

		// acceptable inputs
		return true;
	}

	/**
	 * Returns selected recipients.
	 *
	 * @return Selected recipients.
	 */
	private ArrayList<ExchangeUser> getSelectedRecipients() {
		ArrayList<ExchangeUser> selected = new ArrayList<>();
		for (Node node : container_.getChildren()) {
			ToggleButton recipient = (ToggleButton) node;
			if (recipient.isSelected()) {
				selected.add((ExchangeUser) recipient.getUserData());
			}
		}
		return selected;
	}

	/**
	 * Loads and returns plot column panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded plot column panel.
	 */
	public static ShareViewPanel load(ViewPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("ShareViewPanel.fxml"));
			fxmlLoader.load();

			// get controller
			ShareViewPanel controller = (ShareViewPanel) fxmlLoader.getController();

			// set attributes
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
