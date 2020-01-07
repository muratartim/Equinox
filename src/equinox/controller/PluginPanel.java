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

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;

import equinox.Equinox;
import equinox.data.ClientPluginInfo;
import equinox.data.EquinoxTheme;
import equinox.dataServer.remote.data.ServerPluginInfo.PluginInfoType;
import equinox.plugin.Plugin;
import equinox.serverUtilities.Permission;
import equinox.task.DeletePluginFromGlobalDB;
import equinox.task.InstallPlugin;
import equinox.task.RemovePlugin;
import equinox.utility.Utility;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

/**
 * Class for plugin panel controller.
 *
 * @author Murat Artim
 * @date Mar 31, 2015
 * @time 10:54:24 AM
 */
public class PluginPanel implements Initializable {

	/** Owner panel. */
	private PluginViewPanel owner_;

	/** Plugin info. */
	private ClientPluginInfo plugin_;

	@FXML
	private VBox root_, buttonPane_;

	@FXML
	private ImageView image_;

	@FXML
	private Label title_, description_, version_, size_;

	@FXML
	private Button install_, delete_;

	@FXML
	private Hyperlink developer_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// no implementation
	}

	/**
	 * Returns the owner panel of this sub panel.
	 *
	 * @return The owner panel of this sub panel.
	 */
	public PluginViewPanel getOwner() {
		return owner_;
	}

	/**
	 * Returns the root of this controller.
	 *
	 * @return The root of this controller.
	 */
	public VBox getRoot() {
		return root_;
	}

	/**
	 * Sets plugin to this panel.
	 *
	 * @param plugin
	 *            Plugin to set.
	 */
	public void setPlugin(ClientPluginInfo plugin) {

		// set plugin
		plugin_ = plugin;

		// set components
		title_.setText((String) plugin_.getInfo(PluginInfoType.NAME));
		description_.setText((String) plugin_.getInfo(PluginInfoType.DESCRIPTION));
		version_.setText("Version: " + (double) plugin_.getInfo(PluginInfoType.VERSION_NUMBER));
		size_.setText("Size: " + Utility.readableFileSize((long) plugin_.getInfo(PluginInfoType.DATA_SIZE)));
		image_.setImage(plugin_.getImage());
		developer_.setText((String) plugin_.getInfo(PluginInfoType.DEVELOPER_NAME));

		// setup install button
		Plugin existingPlugin = owner_.getOwner().getOwner().getPluginManager().getPlugin((String) plugin_.getInfo(PluginInfoType.NAME));

		// plugin doesn't exist
		if (existingPlugin == null) {
			install_.setText("Install");
		}
		else if (existingPlugin.getVersion() == (double) plugin_.getInfo(PluginInfoType.VERSION_NUMBER)) {
			install_.setText("Install");
			install_.setDisable(true);
		}

		// update
		else if (existingPlugin.getVersion() < (double) plugin_.getInfo(PluginInfoType.VERSION_NUMBER)) {
			install_.setText("Update");
		}

		// remove delete button if not administrator or logged in
		if (!Equinox.USER.hasPermission(Permission.DELETE_EQUINOX_PLUGIN, false, null)) {
			buttonPane_.getChildren().remove(delete_);
		}
	}

	public static void main(String[] args) {
		String text = "Question about Equinox Plugin Extract Excel Worksheet";
		text = text.replaceAll(" ", "%20");
		System.out.println(text);
	}

	@FXML
	private void onDeveloperClicked() {

		// open default mail application
		try {

			// desktop is not supported
			if (!Desktop.isDesktopSupported()) {
				String message = "Cannot open default mail application. Desktop class is not supported.";
				owner_.getOwner().getOwner().getNotificationPane().showWarning(message, null);
				return;
			}

			// get desktop
			Desktop desktop = Desktop.getDesktop();

			// open action is not supported
			if (!desktop.isSupported(Desktop.Action.MAIL)) {
				String message = "Cannot open default mail application. Mail action is not supported.";
				owner_.getOwner().getOwner().getNotificationPane().showWarning(message, null);
				return;
			}

			// open main application
			String developerEmail = (String) plugin_.getInfo(PluginInfoType.DEVELOPER_EMAIL);
			String subject = "Question about Equinox Plugin " + title_.getText();
			subject = subject.replaceAll(" ", "%20");
			desktop.mail(new URI("mailto:" + developerEmail + "?subject=" + subject));
		}

		// exception occurred
		catch (Exception e) {
			String msg = "Exception occurred during mailing plugin developer: ";
			Equinox.LOGGER.log(Level.WARNING, msg, e);
			msg += e.getLocalizedMessage();
			msg += " Click 'Details' for more information.";
			owner_.getOwner().getOwner().getNotificationPane().showError("Problem encountered", msg, e);
		}
	}

	@FXML
	private void onInstallClicked() {

		// get task manager
		ActiveTasksPanel tm = owner_.getOwner().getOwner().getActiveTasksPanel();

		// install
		if (install_.getText().equals("Install")) {
			tm.runTaskSequentially(new InstallPlugin(plugin_));
		}

		// update
		else if (install_.getText().equals("Update")) {

			// get plugin to remove
			Plugin plugin = owner_.getOwner().getOwner().getPluginManager().getPlugin((String) plugin_.getInfo(PluginInfoType.NAME));

			// upgrade
			tm.runTasksSequentially(new RemovePlugin(plugin), new InstallPlugin(plugin_));
		}
	}

	@FXML
	private void onDeleteClicked() {

		// create confirmation action
		PopOver popOver = new PopOver();
		EventHandler<ActionEvent> handler = event -> {

			// get task panel
			ActiveTasksPanel tm = owner_.getOwner().getOwner().getActiveTasksPanel();

			// delete
			tm.runTaskInParallel(new DeletePluginFromGlobalDB(plugin_));

			// hide pop-over
			popOver.hide();
		};

		// show message
		String message = "Are you sure you want to delete the plugin from ESCSAS global database?";
		popOver.setArrowLocation(ArrowLocation.TOP_RIGHT);
		popOver.setDetachable(false);
		popOver.setContentNode(NotificationPanel2.load(popOver, message, 50, "Delete", handler, NotificationPanel2.QUESTION));
		popOver.setHideOnEscape(true);
		popOver.setAutoHide(true);

		// show popup
		popOver.show(delete_);
	}

	/**
	 * Loads and returns plot column panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded plot column panel.
	 */
	public static PluginPanel load(PluginViewPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("PluginPanel.fxml"));
			fxmlLoader.load();

			// get controller
			PluginPanel controller = (PluginPanel) fxmlLoader.getController();

			// set owner
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
