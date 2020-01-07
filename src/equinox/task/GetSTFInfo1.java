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
package equinox.task;

import java.io.ByteArrayInputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

import equinox.Equinox;
import equinox.controller.InfoViewPanel;
import equinox.controller.ViewPanel;
import equinox.data.fileType.STFFile;
import equinox.data.ui.STFTableItem;
import equinox.data.ui.TableItem;
import equinox.dataServer.remote.data.PilotPointImageType;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;

/**
 * Class for get STF info task. This used for displaying STF information on info screen.
 *
 * @author Murat Artim
 * @date Aug 1, 2014
 * @time 1:02:15 PM
 */
public class GetSTFInfo1 extends InternalEquinoxTask<Void> implements ShortRunningTask {

	/** STF file. */
	private final STFFile stfFile_;

	/** File content. */
	private ArrayList<STFTableItem> content_ = null;

	/** Info. */
	private ArrayList<TreeItem<TableItem>> info_ = null;

	/** Pilot point images. */
	private HashMap<PilotPointImageType, Image> images_;

	/** Decimal format. */
	private final DecimalFormat format_ = new DecimalFormat("#.###");

	/**
	 * Creates get STF info task.
	 *
	 * @param stfFile
	 *            STF file.
	 */
	public GetSTFInfo1(STFFile stfFile) {
		stfFile_ = stfFile;
	}

	@Override
	public String getTaskTitle() {
		return "Get STF info for '" + stfFile_.getName() + "'";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	protected Void call() throws Exception {

		// update progress info
		updateTitle("Getting STF info from database");

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// create statement
			try (Statement statement = connection.createStatement()) {

				// get info
				getInfo(statement);

				// get content
				getContent(statement, connection);
			}
		}

		// return
		return null;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// get panel
		InfoViewPanel panel = (InfoViewPanel) taskPanel_.getOwner().getOwner().getViewPanel().getSubPanel(ViewPanel.INFO_VIEW);

		// set content
		panel.getSTFView().setContent(content_, stfFile_.is2D());

		// set info
		panel.getSTFView().setInfo(stfFile_, images_, info_);

		// show panel
		panel.showSTFView(stfFile_);
		taskPanel_.getOwner().getOwner().getViewPanel().showSubPanel(ViewPanel.INFO_VIEW);
	}

	/**
	 * Returns info.
	 *
	 * @param statement
	 *            Database statement.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void getInfo(Statement statement) throws Exception {

		// create info
		info_ = new ArrayList<>();

		// get info
		String sql = "select description, element_type, frame_rib_position, stringer_position, data_source, ";
		sql += "generation_source, delivery_ref_num, issue, eid, fatigue_material, preffas_material, linear_material from stf_files where file_id = " + stfFile_.getID();
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {

				// pilot point name
				info_.add(new TreeItem<>(new TableItem("Pilot point name", stfFile_.getName())));

				// spectrum info
				TreeItem<TableItem> spectrumInfo = new TreeItem<>(new TableItem("Spectrum info", ""));
				spectrumInfo.getChildren().add(new TreeItem<>(new TableItem("Spectrum", stfFile_.getParentItem().getName())));
				spectrumInfo.getChildren().add(new TreeItem<>(new TableItem("A/C program", stfFile_.getParentItem().getProgram())));
				spectrumInfo.getChildren().add(new TreeItem<>(new TableItem("A/C section", stfFile_.getParentItem().getSection())));
				String mission = stfFile_.getMission();
				spectrumInfo.getChildren().add(new TreeItem<>(new TableItem("Mission", mission)));
				info_.add(spectrumInfo);

				// pilot point info
				TreeItem<TableItem> ppInfo = new TreeItem<>(new TableItem("Pilot point info", ""));
				String info = resultSet.getString("description");
				ppInfo.getChildren().add(new TreeItem<>(new TableItem("Description", info == null || info.isEmpty() ? "-" : info)));
				info = resultSet.getString("data_source");
				ppInfo.getChildren().add(new TreeItem<>(new TableItem("Data source", info == null || info.isEmpty() ? "-" : info)));
				info = resultSet.getString("generation_source");
				ppInfo.getChildren().add(new TreeItem<>(new TableItem("Gen. source", info == null || info.isEmpty() ? "-" : info)));
				info = resultSet.getString("delivery_ref_num");
				ppInfo.getChildren().add(new TreeItem<>(new TableItem("Delivery ref.", info == null || info.isEmpty() ? "-" : info)));
				info = resultSet.getString("issue");
				ppInfo.getChildren().add(new TreeItem<>(new TableItem("Issue", info == null || info.isEmpty() ? "-" : info)));
				info_.add(ppInfo);

				// location info
				TreeItem<TableItem> locationInfo = new TreeItem<>(new TableItem("Location info", ""));
				info = resultSet.getString("eid");
				locationInfo.getChildren().add(new TreeItem<>(new TableItem("EID/LIQ/SG", info == null || info.isEmpty() ? "-" : info)));
				info = resultSet.getString("element_type");
				locationInfo.getChildren().add(new TreeItem<>(new TableItem("Element type", info == null || info.isEmpty() ? "-" : info)));
				info = resultSet.getString("frame_rib_position");
				locationInfo.getChildren().add(new TreeItem<>(new TableItem("Frame/Rib", info == null || info.isEmpty() ? "-" : info)));
				info = resultSet.getString("stringer_position");
				locationInfo.getChildren().add(new TreeItem<>(new TableItem("Stringer", info == null || info.isEmpty() ? "-" : info)));
				info_.add(locationInfo);

				// material info
				TreeItem<TableItem> materialInfo = new TreeItem<>(new TableItem("Material info", ""));
				info = resultSet.getString("fatigue_material");
				materialInfo.getChildren().add(new TreeItem<>(new TableItem("Fatigue", info == null || info.isEmpty() ? "-" : info)));
				info = resultSet.getString("preffas_material");
				materialInfo.getChildren().add(new TreeItem<>(new TableItem("Preffas", info == null || info.isEmpty() ? "-" : info)));
				info = resultSet.getString("linear_material");
				materialInfo.getChildren().add(new TreeItem<>(new TableItem("Linear", info == null || info.isEmpty() ? "-" : info)));
				info_.add(materialInfo);
			}
		}

		// get mission parameters of STF file
		TreeItem<TableItem> missionParameters = new TreeItem<>(new TableItem("Mission params", ""));
		sql = "select name, val from stf_mission_parameters where stf_id = " + stfFile_.getID() + " order by name";
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				missionParameters.getChildren().add(new TreeItem<>(new TableItem(resultSet.getString("name"), format_.format(resultSet.getDouble("val")))));
			}
		}

		// no parameter found
		if (missionParameters.getChildren().isEmpty()) {

			// get parameters of the owner spectrum
			sql = "select name, val from cdf_mission_parameters where cdf_id = " + stfFile_.getParentItem().getID() + " order by name";
			try (ResultSet resultSet = statement.executeQuery(sql)) {
				while (resultSet.next()) {
					missionParameters.getChildren().add(new TreeItem<>(new TableItem(resultSet.getString("name"), format_.format(resultSet.getDouble("val")))));
				}
			}
		}

		// add mission parameters
		if (!missionParameters.getChildren().isEmpty()) {
			info_.add(missionParameters);
		}

		// get pilot point images
		images_ = new HashMap<>();
		for (PilotPointImageType imageType : PilotPointImageType.values()) {
			sql = "select image from " + imageType.getTableName() + " where id = " + stfFile_.getID();
			try (ResultSet resultSet = statement.executeQuery(sql)) {
				while (resultSet.next()) {
					Blob blob = resultSet.getBlob("image");
					if (blob != null) {
						byte[] imageBytes = blob.getBytes(1L, (int) blob.length());
						Image image = imageBytes == null ? null : new Image(new ByteArrayInputStream(imageBytes));
						images_.put(imageType, image);
						blob.free();
					}
				}
			}
		}
	}

	/**
	 * Returns STF content.
	 *
	 * @param statement
	 *            Database statement.
	 * @param connection
	 *            Database connection.
	 * @return STF content.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private ArrayList<STFTableItem> getContent(Statement statement, Connection connection) throws Exception {

		// create content list
		content_ = new ArrayList<>();

		// prepare statement for getting event name and comments from conversion table
		String sql = "select fue_translated, comment from xls_comments where file_id = " + stfFile_.getParentItem().getConversionTableID() + " and issy_code = ?";
		try (PreparedStatement getEventInfo = connection.prepareStatement(sql)) {

			// prepare statement for getting load case type from TXT file content
			sql = "select dp_case, increment_num from txt_codes where file_id = " + stfFile_.getParentItem().getTXTFileID() + " and issy_code = ?";
			try (PreparedStatement getLCType = connection.prepareStatement(sql)) {
				getLCType.setMaxRows(1);

				// get STF file content
				sql = "select * from stf_stresses_" + stfFile_.getStressTableID() + " where file_id = " + stfFile_.getID() + " order by issy_code";
				try (ResultSet resultSet = statement.executeQuery(sql)) {
					while (resultSet.next()) {

						// get columns
						String issyCode = resultSet.getString("issy_code");
						int loadcase = Integer.parseInt(issyCode);
						double sx = resultSet.getDouble("stress_x");
						double sy = resultSet.getDouble("stress_y");
						double sxy = resultSet.getDouble("stress_xy");

						// create and set values
						STFTableItem item = new STFTableItem();
						item.setLoadcase(loadcase);
						item.setSx(sx);
						item.setSy(sy);
						item.setSxy(sxy);

						// get load case type
						getLCType.setString(1, issyCode);
						try (ResultSet resultSet1 = getLCType.executeQuery()) {
							while (resultSet1.next()) {
								boolean isDP = resultSet1.getBoolean("dp_case");
								boolean isInc = resultSet1.getInt("increment_num") != 0;
								item.setType(isDP ? STFTableItem.DELTA_P : isInc ? STFTableItem.INCREMENT : STFTableItem.STEADY);
							}
						}

						// get event info
						getEventInfo.setString(1, issyCode);
						try (ResultSet resultSet1 = getEventInfo.executeQuery()) {
							while (resultSet1.next()) {
								item.setEventname(resultSet1.getString("fue_translated"));
								item.setEventcomment(resultSet1.getString("comment"));
							}
						}

						// add to content
						content_.add(item);
					}
				}
			}
		}

		// return content
		return content_;
	}
}
