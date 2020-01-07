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
package equinox.data.fileType;

import equinox.data.ActionHandler;
import equinox.font.IconicFont;
import javafx.collections.ObservableList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TreeItem;

/**
 * Class for aircraft model.
 *
 * @author Murat Artim
 * @date Jul 6, 2015
 * @time 9:47:51 AM
 */
public class AircraftModel extends SpectrumItem {

	/** A/C program and model name. */
	private String program_, name_;

	/**
	 * Creates A/C model.
	 *
	 * @param program
	 *            A/C program.
	 * @param modelName
	 *            A/C model name.
	 * @param id
	 *            ID of item.
	 */
	public AircraftModel(String program, String modelName, int id) {

		// create spectrum item
		super(createName(program, modelName), id);
		program_ = program;
		name_ = modelName;

		// create icon label
		Label iconLabel = new Label("\uec02");
		iconLabel.getStylesheets().add(IconicFont.ICOMOON.getStyleSheet());
		setGraphic(iconLabel);
	}

	@Override
	public SpectrumItem getParentItem() {
		return null;
	}

	@Override
	public IconicFont getIconFont() {
		return IconicFont.ICOMOON;
	}

	/**
	 * Returns the load cases folder of this model.
	 *
	 * @return The load cases folder of this model.
	 */
	public AircraftLoadCases getLoadCases() {
		for (TreeItem<String> item : getChildren()) {
			if (item instanceof AircraftLoadCases)
				return (AircraftLoadCases) item;
		}
		return null;
	}

	/**
	 * Returns the pilot points folder of this model.
	 *
	 * @return The pilot points folder of this model.
	 */
	public PilotPoints getPilotPoints() {
		for (TreeItem<String> item : getChildren()) {
			if (item instanceof PilotPoints)
				return (PilotPoints) item;
		}
		return null;
	}

	/**
	 * Returns the aircraft model equivalent stresses folder of this model.
	 *
	 * @return The aircraft model equivalent stresses folder of this model.
	 */
	public AircraftEquivalentStresses getEquivalentStresses() {
		for (TreeItem<String> item : getChildren()) {
			if (item instanceof AircraftEquivalentStresses)
				return (AircraftEquivalentStresses) item;
		}
		return null;
	}

	/**
	 * Returns A/C program.
	 *
	 * @return A/C program.
	 */
	public String getProgram() {
		return program_;
	}

	/**
	 * Returns A/C model name.
	 *
	 * @return A/C model name.
	 */
	public String getModelName() {
		return name_;
	}

	/**
	 * Sets A/C program.
	 *
	 * @param program
	 *            A/C program.
	 */
	public void setProgram(String program) {
		program_ = program;
	}

	/**
	 * Sets model name.
	 *
	 * @param name
	 *            Model name.
	 */
	public void setModelName(String name) {
		name_ = name;
	}

	/**
	 * Creates name for A/C model.
	 *
	 * @param program
	 *            A/C program.
	 * @param modelName
	 *            A/C model name.
	 * @return Name.
	 */
	public static String createName(String program, String modelName) {
		return program + ", " + modelName;
	}

	/**
	 * Creates and returns context menu for ANA files.
	 *
	 * @param multipleSelection
	 *            True if multiple ANA files are selected.
	 * @param handler
	 *            Action handler.
	 * @param selected
	 *            Selected A/C models.
	 * @param is3dEnabled
	 *            True if 3D viewer is enabled.
	 * @return Context menu.
	 */
	public static ContextMenu createContextMenu(boolean multipleSelection, ActionHandler handler, ObservableList<TreeItem<String>> selected,
			boolean is3dEnabled) {

		// create menu
		ContextMenu contextMenu = new ContextMenu();

		// single selection
		if (!multipleSelection) {

			// plot model
			Label plotIcon = new Label("\uf1d8");
			plotIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
			MenuItem plot = new MenuItem("Plot A/C Structure", plotIcon);
			plot.setId("plotACStructure");
			plot.setOnAction(handler);
			plot.setDisable(!is3dEnabled);
			contextMenu.getItems().add(plot);

			// add separator
			contextMenu.getItems().add(new SeparatorMenuItem());
		}

		// find
		Label findIcon = new Label("\uf002");
		findIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
		MenuItem find = new MenuItem("Find Similar Files", findIcon);
		find.setId("find");
		find.setOnAction(handler);
		contextMenu.getItems().add(find);

		// edit info
		if (!multipleSelection) {
			Label editIcon = new Label("\uf044");
			editIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
			MenuItem editInfo = new MenuItem("Edit A/C Model Info", editIcon);
			editInfo.setId("editACInfo");
			editInfo.setOnAction(handler);
			contextMenu.getItems().add(editInfo);
			contextMenu.getItems().add(new SeparatorMenuItem());
		}

		// element groups
		if (!multipleSelection) {

			// rename element groups
			Label groupIcon = new Label("\uf247");
			groupIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
			MenuItem rename = new MenuItem("Rename Element Groups", groupIcon);
			rename.setId("renameElementGroups");
			rename.setOnAction(handler);
			contextMenu.getItems().add(rename);

			// delete element groups
			MenuItem deleteGroups = new MenuItem("Delete Element Groups");
			deleteGroups.setId("deleteElementGroups");
			deleteGroups.setOnAction(handler);
			contextMenu.getItems().add(deleteGroups);

			// create new element group
			Menu createGroup = new Menu("Create New Element Groups...");
			contextMenu.getItems().add(createGroup);
			MenuItem fromFile = new MenuItem("From Element Groups File...");
			fromFile.setId("createElementGroupsFromFile");
			fromFile.setOnAction(handler);
			createGroup.getItems().add(fromFile);
			MenuItem fromEIDs = new MenuItem("From Element IDs");
			fromEIDs.setId("createElementGroupFromEIDs");
			fromEIDs.setOnAction(handler);
			createGroup.getItems().add(fromEIDs);
			MenuItem fromCoordinates = new MenuItem("From X/Y/Z Coordinates");
			fromCoordinates.setId("createElementGroupFromCoordinates");
			fromCoordinates.setOnAction(handler);
			createGroup.getItems().add(fromCoordinates);
			MenuItem fromFrameStringerPos = new MenuItem("From Frame/Stringer Positions");
			fromFrameStringerPos.setId("createElementGroupFromQVLV");
			fromFrameStringerPos.setOnAction(handler);
			createGroup.getItems().add(fromFrameStringerPos);
			MenuItem fromGroups = new MenuItem("From Existing Groups");
			fromGroups.setId("createElementGroupFromGroups");
			fromGroups.setOnAction(handler);
			createGroup.getItems().add(fromGroups);

			// from linked pilot points
			if (((AircraftModel) selected.get(0)).getPilotPoints().getPilotPoints() != null) {
				MenuItem fromLinkedPilotPoints = new MenuItem("From Linked Pilot Points");
				fromLinkedPilotPoints.setId("createElementGroupFromLinkedPilotPoints");
				fromLinkedPilotPoints.setOnAction(handler);
				createGroup.getItems().add(fromLinkedPilotPoints);
			}

			// download sample element groups file
			createGroup.getItems().add(new SeparatorMenuItem());
			MenuItem sampleGroupsFile = new MenuItem("Download Sample Element Groups File");
			sampleGroupsFile.setId("downloadSampleElementGroupsFile");
			sampleGroupsFile.setOnAction(handler);
			createGroup.getItems().add(sampleGroupsFile);
		}

		// share
		if (!multipleSelection) {

			// share model
			contextMenu.getItems().add(new SeparatorMenuItem());
			Label shareIcon = new Label("\uf064");
			shareIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
			MenuItem share = new MenuItem("Share A/C Model", shareIcon);
			share.setId("share");
			share.setOnAction(handler);
			contextMenu.getItems().add(share);

			// share model file
			Menu shareMenu = new Menu("Share Model File...");
			contextMenu.getItems().add(shareMenu);
			MenuItem shareF07 = new MenuItem("Share F07 Grid Data File");
			shareF07.setId("shareGridFile");
			shareF07.setOnAction(handler);
			shareMenu.getItems().add(shareF07);
			MenuItem shareF06 = new MenuItem("Share F06 Element Data File");
			shareF06.setId("shareElementFile");
			shareF06.setOnAction(handler);
			shareMenu.getItems().add(shareF06);
			MenuItem shareGRP = new MenuItem("Share GRP Element Groups File");
			shareGRP.setId("shareGRP");
			shareGRP.setOnAction(handler);
			shareMenu.getItems().add(shareGRP);
		}

		// save model
		contextMenu.getItems().add(new SeparatorMenuItem());
		Label saveIcon = new Label("\uf0c7");
		saveIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
		MenuItem saveModel = new MenuItem("Save A/C Model As...", saveIcon);
		saveModel.setId("saveModel");
		saveModel.setOnAction(handler);
		contextMenu.getItems().add(saveModel);

		// save model file
		Menu saveMenu = new Menu("Save Model File...");
		contextMenu.getItems().add(saveMenu);
		MenuItem saveF07 = new MenuItem("Save F07 Grid Data File As...");
		saveF07.setId("saveGridFile");
		saveF07.setOnAction(handler);
		saveMenu.getItems().add(saveF07);
		MenuItem saveF06 = new MenuItem("Save F06 Element Data File As...");
		saveF06.setId("saveElementFile");
		saveF06.setOnAction(handler);
		saveMenu.getItems().add(saveF06);
		MenuItem saveGRP = new MenuItem("Save GRP Element Groups File As...");
		saveGRP.setId("saveGRP");
		saveGRP.setOnAction(handler);
		saveMenu.getItems().add(saveGRP);

		// export
		contextMenu.getItems().add(new SeparatorMenuItem());
		Label exportIcon = new Label("\uea98");
		exportIcon.getStylesheets().add(IconicFont.ICOMOON.getStyleSheet());
		MenuItem export = new MenuItem(multipleSelection ? "Export A/C Models..." : "Export A/C Model...", exportIcon);
		export.setId(multipleSelection ? "exportMultipleModels" : "exportModel");
		export.setOnAction(handler);
		contextMenu.getItems().add(export);

		// hide
		contextMenu.getItems().add(new SeparatorMenuItem());
		Label hideIcon = new Label("\uf070");
		hideIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
		MenuItem hide = new MenuItem("Hide", hideIcon);
		hide.setId("hide");
		hide.setOnAction(handler);
		contextMenu.getItems().add(hide);

		// delete
		Label deleteIcon = new Label("\uf014");
		deleteIcon.getStylesheets().add(IconicFont.FONTAWESOME.getStyleSheet());
		MenuItem delete = new MenuItem("Delete", deleteIcon);
		delete.setId("delete");
		delete.setOnAction(handler);
		contextMenu.getItems().add(delete);

		// return menu
		return contextMenu;
	}
}
