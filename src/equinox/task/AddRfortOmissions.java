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
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;

import equinox.Equinox;
import equinox.data.AnalysisEngine;
import equinox.data.IsamiSubVersion;
import equinox.data.IsamiVersion;
import equinox.data.fileType.Rfort;
import equinox.data.input.RfortExtendedInput;
import equinox.data.ui.RfortOmission;
import equinox.task.InternalEquinoxTask.ShortRunningTask;

/**
 * Class for add RFORT omissions task.
 *
 * @author Murat Artim
 * @date Mar 14, 2016
 * @time 11:33:31 AM
 */
public class AddRfortOmissions extends InternalEquinoxTask<RfortExtendedInput> implements ShortRunningTask {

	/** RFORT file. */
	private final Rfort rfort_;

	/** Omissions to be added. */
	private final ArrayList<RfortOmission> omissions_;

	/** Stress amplitudes. */
	private HashMap<String, Double> stressAmplitudes_;

	/** Analysis engine. */
	private final AnalysisEngine analysisEngine_;

	/** ISAMI version. */
	private IsamiVersion isamiVersion_;

	/** ISAMI sub version. */
	private IsamiSubVersion isamiSubVersion_;

	/** True compression should be applied in propagation analysis. */
	private boolean applyCompression_;

	/**
	 * Creates add RFORT omissions task.
	 *
	 * @param rfort
	 *            RFORT file.
	 * @param omissions
	 *            Omissions to be added.
	 * @param analysisEngine
	 *            Analysis engine.
	 */
	public AddRfortOmissions(Rfort rfort, ArrayList<RfortOmission> omissions, AnalysisEngine analysisEngine) {
		rfort_ = rfort;
		omissions_ = omissions;
		analysisEngine_ = analysisEngine;
	}

	/**
	 * Sets ISAMI engine inputs.
	 *
	 * @param isamiVersion
	 *            ISAMI version.
	 * @param isamiSubVersion
	 *            ISAMI sub version.
	 * @param applyCompression
	 *            True to apply compression for propagation analyses.
	 * @return This object.
	 */
	public AddRfortOmissions setIsamiEngineInputs(IsamiVersion isamiVersion, IsamiSubVersion isamiSubVersion, boolean applyCompression) {
		isamiVersion_ = isamiVersion;
		isamiSubVersion_ = isamiSubVersion;
		applyCompression_ = applyCompression;
		return this;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Add RFORT omissions";
	}

	@Override
	protected RfortExtendedInput call() throws Exception {

		// initialize input data
		RfortExtendedInput input = null;

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// create statement
			try (Statement statement = connection.createStatement()) {

				// check omissions
				checkOmissions(statement);

				// get input data
				input = getInputData(statement);

				// update input data
				input = updateInputData(connection, input);

				// get stress amplitudes
				stressAmplitudes_ = getStressAmplitudes(statement);
			}
		}

		// return input data
		return input;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// run RFORT processes
		try {

			// get stress amplitudes
			RfortExtendedInput input = get();

			// run
			if ((input != null) && (stressAmplitudes_ != null)) {
				runRfort(input);
			}
		}

		// exception occurred
		catch (Exception e) {
			handleResultRetrievalException(e);
		}
	}

	/**
	 * Updates the RFORT input with new omissions and saves it back to database.
	 *
	 * @param connection
	 *            Database connection.
	 * @param input
	 *            RFORT input data.
	 * @return The updated input data.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private RfortExtendedInput updateInputData(Connection connection, RfortExtendedInput input) throws Exception {

		// add new omissions
		for (RfortOmission omission : omissions_) {
			input.addOmission(omission);
		}

		// update input data in database
		String sql = "update rfort_analyses set input_data = ? where id = " + rfort_.getID();
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
				try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
					oos.writeObject(input);
					oos.flush();
					byte[] bytes = bos.toByteArray();
					try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
						statement.setBinaryStream(1, bais, bytes.length);
					}
				}
			}
			statement.executeUpdate();
		}

		// return updated input
		return input;
	}

	/**
	 * Creates and runs RFORT analyses.
	 *
	 * @param input
	 *            RFORT input data.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void runRfort(RfortExtendedInput input) throws Exception {
		for (RfortOmission omission : omissions_) {
			taskPanel_.getOwner().runTaskInParallel(new RfortAnalysis(input, omission, rfort_.getID(), stressAmplitudes_, analysisEngine_).setIsamiEngineInputs(isamiVersion_, isamiSubVersion_, applyCompression_));
		}
	}

	/**
	 * Checks omissions to add whether any of them already computed.
	 *
	 * @param statement
	 *            Database statement.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void checkOmissions(Statement statement) throws Exception {

		// create array to store omissions to be removed
		ArrayList<RfortOmission> toBeRemoved = new ArrayList<>();

		// get omissions
		String sql = "select distinct omission_name from rfort_outputs where analysis_id = " + rfort_.getID() + " order by omission_name asc";
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {

				// get omission name
				String omissionName = resultSet.getString("omission_name");

				// get omissions to be removed
				for (RfortOmission omission : omissions_) {
					if (omissionName.equals(omission.toString())) {
						toBeRemoved.add(omission);
					}
				}
			}
		}

		// remove
		omissions_.removeAll(toBeRemoved);
	}

	/**
	 * Returns maximum stress amplitudes of all pilot points.
	 *
	 * @param statement
	 *            Database statement.
	 * @return Maximum stress amplitudes of all pilot points.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private HashMap<String, Double> getStressAmplitudes(Statement statement) throws Exception {

		// progress info
		updateMessage("Getting maximum stress amplitudes of all pilot points...");

		// initialize stress amplitudes
		HashMap<String, Double> stressAmplitudes = null;

		// get pilot point name and maximum stress amplitudes for initial run
		String sql = "select pp_name, stress_amp from rfort_outputs ";
		sql += "where analysis_id = " + rfort_.getID() + " and omission_name = '" + RfortOmission.INITIAL_ANALYSIS + "' and stress_type = '" + SaveRfortInfo.FATIGUE + "'";
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				if (stressAmplitudes == null) {
					stressAmplitudes = new HashMap<>();
				}
				stressAmplitudes.put(resultSet.getString("pp_name"), resultSet.getDouble("stress_amp"));
			}
		}

		// return amplitudes
		return stressAmplitudes;
	}

	/**
	 * Reads and returns RFORT input data.
	 *
	 * @param statement
	 *            Database statement.
	 * @return RFORT input data.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private RfortExtendedInput getInputData(Statement statement) throws Exception {

		// progress info
		updateMessage("Getting RFORT input data from database...");

		// initialize input data
		RfortExtendedInput input = null;

		// create and execute query
		String sql = "select input_data from rfort_analyses where id = " + rfort_.getID();
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				Blob blob = resultSet.getBlob("input_data");
				byte[] bytes = blob.getBytes(1L, (int) blob.length());
				blob.free();
				try (ByteArrayInputStream bos = new ByteArrayInputStream(bytes)) {
					try (ObjectInputStream ois = new ObjectInputStream(bos)) {
						input = (RfortExtendedInput) ois.readObject();
					}
				}
			}
		}

		// return input data
		return input;
	}
}
