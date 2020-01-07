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

import equinox.data.EquinoxTheme;
import equinox.data.fileType.Spectrum;
import equinox.dataServer.remote.data.PilotPointInfo;
import equinox.dataServer.remote.data.PilotPointInfo.PilotPointInfoType;
import equinox.task.DownloadPilotPoint;
import equinox.task.DownloadPilotPoints;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.VBox;

/**
 * Class for add STF panel controller.
 *
 * @author Murat Artim
 * @date Feb 17, 2016
 * @time 1:30:17 PM
 */
public class AddSTFPanel implements Initializable {

	/** Owner panel. */
	private MainScreen owner_;

	/** Pilot point info. */
	private ArrayList<PilotPointInfo> info_;

	/** The owner pop-over. */
	private PopOver popOver_;

	@FXML
	private VBox root_;

	@FXML
	private ComboBox<Spectrum> spectra_;

	@FXML
	private Button ok_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// no implementation
	}

	@FXML
	private void onOkClicked() {

		// get selected spectrum
		Spectrum spectrum = spectra_.getSelectionModel().getSelectedItem();

		// no spectrum selected
		if (spectrum == null) {
			String message = "Please select a spectrum to add the pilot point(s).";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_CENTER);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(spectra_);
			return;
		}

		// hide
		popOver_.hide();

		// add pilot point
		ActiveTasksPanel tm = owner_.getActiveTasksPanel();

		// single pilot point1
		if (info_.size() == 1) {
			tm.runTaskInParallel(new DownloadPilotPoint(info_.get(0), null, spectrum));
		}

		// multiple pilot points
		else {
			tm.runTaskInParallel(new DownloadPilotPoints(info_, spectrum));
		}
	}

	@FXML
	private void onCancelClicked() {
		popOver_.hide();
	}

	/**
	 * Loads and returns file CDF set panel.
	 *
	 * @param info
	 *            Pilot point info.
	 * @param owner
	 *            The owner panel.
	 * @param popOver
	 *            Pop over.
	 * @return The newly loaded file CDF set panel.
	 */
	public static VBox load(ArrayList<PilotPointInfo> info, MainScreen owner, PopOver popOver) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("AddSTFPanel.fxml"));
			fxmlLoader.load();

			// get controller
			AddSTFPanel controller = (AddSTFPanel) fxmlLoader.getController();

			// set attributes
			controller.owner_ = owner;
			controller.info_ = info;
			controller.popOver_ = popOver;

			// get spectra
			String spectrumName = (String) info.get(0).getInfo(PilotPointInfoType.SPECTRUM_NAME);
			Spectrum selected = null;
			TreeItem<String> root = controller.owner_.getInputPanel().getFileTreeRoot();
			for (TreeItem<String> item : root.getChildren())
				if (item instanceof Spectrum) {
					Spectrum spectrum = (Spectrum) item;
					controller.spectra_.getItems().add(spectrum);
					if (spectrum.getName().equals(spectrumName)) {
						selected = spectrum;
					}
				}
			if (selected != null) {
				controller.spectra_.getSelectionModel().select(selected);
			}

			// return controller
			return controller.root_;
		}

		// exception occurred during loading
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
