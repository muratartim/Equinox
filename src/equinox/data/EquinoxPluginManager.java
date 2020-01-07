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
package equinox.data;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.Future;

import equinox.controller.InputPanel;
import equinox.controller.MainScreen;
import equinox.controller.ViewPanel;
import equinox.controller.WebViewPanel;
import equinox.data.ui.NotificationPanel;
import equinox.font.IconicFont;
import equinox.plugin.Plugin;
import equinox.plugin.PluginIcon;
import equinox.plugin.PluginManager;
import equinox.plugin.PluginProcess;
import equinox.task.PluginTask;
import equinox.task.RemovePlugin;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

/**
 * Class for plugin manager.
 *
 * @author Murat Artim
 * @date Mar 26, 2015
 * @time 4:18:44 PM
 */
public class EquinoxPluginManager implements PluginManager {

	/** The owner of plugin manager. */
	private final MainScreen owner_;

	/** Plugins. */
	private final HashMap<Plugin, PluginPanelIndex> plugins_;

	/**
	 * Creates plugin manager.
	 *
	 * @param owner
	 *            The owner of plugin manager.
	 */
	public EquinoxPluginManager(MainScreen owner) {
		owner_ = owner;
		plugins_ = new HashMap<>();
	}

	/**
	 * Returns the owner of plugin manager.
	 *
	 * @return The owner of plugin manager.
	 */
	public MainScreen getOwner() {
		return owner_;
	}

	/**
	 * Returns the plugin with the given name.
	 *
	 * @param pluginName
	 *            Plugin name.
	 * @return Plugin name.
	 */
	public Plugin getPlugin(String pluginName) {
		Iterator<Plugin> iterator = plugins_.keySet().iterator();
		while (iterator.hasNext()) {
			Plugin plugin = iterator.next();
			if (plugin.getName().equals(pluginName))
				return plugin;
		}
		return null;
	}

	/**
	 * Adds given plugin.
	 *
	 * @param plugin
	 *            Plugin to add.
	 */
	public void addPlugin(Plugin plugin) {

		// create and add plugin input panel
		int inputPanelIndex = owner_.getInputPanel().addInputPanel(plugin.createInputPanel());

		// create and add plugin view panel
		int viewPanelIndex = owner_.getViewPanel().addViewPanel(plugin.createViewPanel());

		// create and add plugin menu item
		int menuItemIndex = owner_.getMenuBarPanel().addPluginMenuItem(createMenuItem(plugin, inputPanelIndex, viewPanelIndex));

		// add plugin to manager
		plugins_.put(plugin, new PluginPanelIndex(inputPanelIndex, viewPanelIndex, menuItemIndex));
	}

	/**
	 * Removes plugin.
	 *
	 * @param plugin
	 *            Plugin to remove.
	 */
	public void removePlugin(Plugin plugin) {

		// get plugin panel indices
		PluginPanelIndex index = plugins_.get(plugin);

		// plugin not found
		if (index == null)
			return;

		// remove plugin menu item
		owner_.getMenuBarPanel().removePluginMenuItem(index.getMenuItemIndex());

		// remove plugin input panel
		owner_.getInputPanel().removeInputPanel(index.getInputPanelIndex());

		// remove plugin view panel
		if (index.getViewPanelIndex() != -1)
			owner_.getViewPanel().removeViewPanel(index.getViewPanelIndex());

		// remove plugin from manager
		plugins_.remove(plugin);
	}

	@Override
	public void showPluginInputPanel(Plugin plugin) {
		if (plugin == null)
			owner_.getInputPanel().showSubPanel(InputPanel.FILE_VIEW_PANEL);
		else
			owner_.getInputPanel().showSubPanel(plugins_.get(plugin).getInputPanelIndex());
	}

	@Override
	public void showPluginViewPanel(Plugin plugin) {
		if (plugin == null)
			owner_.getViewPanel().showSubPanel(ViewPanel.INFO_VIEW);
		else
			owner_.getViewPanel().showSubPanel(plugins_.get(plugin).getViewPanelIndex());
	}

	@Override
	public Future<?> runProcessSequentially(PluginProcess process) {
		return owner_.getActiveTasksPanel().runTaskSequentially(new PluginTask(process));
	}

	@Override
	public Future<?> runProcessInParallel(PluginProcess process) {
		return owner_.getActiveTasksPanel().runTaskInParallel(new PluginTask(process));
	}

	@Override
	public void runProcessesSequentially(PluginProcess... processes) {
		PluginTask[] tasks = new PluginTask[processes.length];
		for (int i = 0; i < tasks.length; i++)
			tasks[i] = new PluginTask(processes[i]);
		owner_.getActiveTasksPanel().runTasksSequentially(tasks);
	}

	@Override
	public Future<?> runProcessSilently(PluginProcess process, boolean isSequential) {
		return owner_.getActiveTasksPanel().runTaskSilently(new PluginTask(process), isSequential);
	}

	@Override
	public Stage getStage() {
		return owner_.getOwner().getStage();
	}

	@Override
	public FileChooser getFileChooser(ExtensionFilter... filters) {
		return owner_.getFileChooser(filters);
	}

	@Override
	public DirectoryChooser getDirectoryChooser() {
		return owner_.getDirectoryChooser();
	}

	@Override
	public void setInitialDirectory(File initialDirectory) {
		owner_.setInitialDirectory(initialDirectory);
	}

	@Override
	public void showHelp(String address) {
		WebViewPanel panel = (WebViewPanel) owner_.getViewPanel().getSubPanel(ViewPanel.WEB_VIEW);
		panel.showAddress(address, null, "Help", "transparent");
		owner_.getViewPanel().showSubPanel(ViewPanel.WEB_VIEW);
	}

	@Override
	public void downloadSampleInput(String name) {
		owner_.downloadSampleInput(name);
	}

	/**
	 * Creates and returns plugin menu item.
	 *
	 * @param plugin
	 *            Plugin to create the menu item for.
	 * @param inputPanelIndex
	 *            Plugin input panel index.
	 * @param viewPanelIndex
	 *            Plugin view panel index.
	 * @return Plugin menu item.
	 */
	private MenuItem createMenuItem(Plugin plugin, int inputPanelIndex, int viewPanelIndex) {

		// create menu item pane
		HBox pane = new HBox();
		pane.setAlignment(Pos.CENTER_LEFT);
		pane.setPrefWidth(220.0);

		// create plugin icon
		PluginIcon icon = plugin.getIcon();
		Label pluginIcon = new Label(icon.getIconKey());
		pluginIcon.getStylesheets().add(IconicFont.values()[icon.getFontIndex()].getStyleSheet());

		// create and add header label
		Label label = new Label(plugin.getName(), pluginIcon);
		label.setMaxWidth(Double.MAX_VALUE);
		HBox.setHgrow(label, Priority.ALWAYS);
		pane.getChildren().add(label);

		// create remove button
		Label deleteIcon = new Label("\uf056");
		deleteIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
		Button remove = new Button();
		remove.setTooltip(new Tooltip("Remove plugin"));
		remove.setGraphic(deleteIcon);
		remove.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
		remove.setStyle("-fx-background-color:transparent;");
		remove.setCursor(Cursor.HAND);
		remove.setPrefHeight(22.0);
		remove.setPrefWidth(22.0);
		remove.setMinHeight(Button.USE_PREF_SIZE);
		remove.setMinWidth(Button.USE_PREF_SIZE);
		remove.setMaxHeight(Button.USE_PREF_SIZE);
		remove.setMaxWidth(Button.USE_PREF_SIZE);
		HBox.setHgrow(remove, Priority.NEVER);

		// add remove button to pane
		pane.getChildren().add(remove);

		// create menu item
		CustomMenuItem menuItem = new CustomMenuItem(pane, true);

		// set action
		menuItem.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				owner_.getInputPanel().showSubPanel(inputPanelIndex);
			}
		});

		// set remove action
		remove.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {

				// hide plugins menu
				owner_.getMenuBarPanel().getPluginsMenu().hide();

				// consume event
				event.consume();

				// setup notification message
				String message = "Are you sure you want to remove the plugin " + plugin.getName() + "?";

				// get notification pane
				NotificationPanel np = owner_.getNotificationPane();

				// setup notification title and message
				String title = "Remove plugin";

				// show notification
				np.showQuestion(title, message, "Yes", "No", new EventHandler<ActionEvent>() {

					@Override
					public void handle(ActionEvent event) {

						// hide input panel (if shown)
						if (owner_.getInputPanel().getCurrentSubPanelIndex() == inputPanelIndex)
							owner_.getInputPanel().showSubPanel(InputPanel.FILE_VIEW_PANEL);

						// hide view panel (if there is and shown)
						if (viewPanelIndex != -1 && owner_.getViewPanel().getCurrentSubPanelIndex() == viewPanelIndex)
							owner_.getViewPanel().showSubPanel(ViewPanel.INFO_VIEW);

						// remove plugin
						owner_.getActiveTasksPanel().runTaskInParallel(new RemovePlugin(plugin));

						// hide notification
						np.hide();
					}
				}, new EventHandler<ActionEvent>() {

					@Override
					public void handle(ActionEvent event) {
						np.hide();
					}
				});
			}
		});

		// return menu item
		return menuItem;
	}

	/**
	 * Inner class for plugin panel indices.
	 *
	 * @author Murat Artim
	 * @date Mar 26, 2015
	 * @time 4:50:09 PM
	 */
	private class PluginPanelIndex {

		/** Panel indices. */
		private final int inputPanelIndex_, viewPanelIndex_, menuItemIndex_;

		/**
		 * Creates plugin panel index.
		 *
		 * @param inputPanelIndex
		 *            Input panel index.
		 * @param viewPanelIndex
		 *            View panel index.
		 * @param menuItemIndex
		 *            Menu item index.
		 */
		public PluginPanelIndex(int inputPanelIndex, int viewPanelIndex, int menuItemIndex) {
			inputPanelIndex_ = inputPanelIndex;
			viewPanelIndex_ = viewPanelIndex;
			menuItemIndex_ = menuItemIndex;
		}

		/**
		 * Returns input panel index.
		 *
		 * @return Input panel index.
		 */
		public int getInputPanelIndex() {
			return inputPanelIndex_;
		}

		/**
		 * Returns view panel index.
		 *
		 * @return View panel index.
		 */
		public int getViewPanelIndex() {
			return viewPanelIndex_;
		}

		/**
		 * Returns menu item index.
		 *
		 * @return Menu item index.
		 */
		public int getMenuItemIndex() {
			return menuItemIndex_;
		}
	}
}
