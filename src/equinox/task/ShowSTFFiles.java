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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import equinox.Equinox;
import equinox.data.fileType.DamageAngle;
import equinox.data.fileType.FastFatigueEquivalentStress;
import equinox.data.fileType.FastLinearEquivalentStress;
import equinox.data.fileType.FastPreffasEquivalentStress;
import equinox.data.fileType.FatigueEquivalentStress;
import equinox.data.fileType.Flight;
import equinox.data.fileType.FlightDamageContributions;
import equinox.data.fileType.Flights;
import equinox.data.fileType.LinearEquivalentStress;
import equinox.data.fileType.LoadcaseDamageContributions;
import equinox.data.fileType.PreffasEquivalentStress;
import equinox.data.fileType.STFFile;
import equinox.data.fileType.STFFileBucket;
import equinox.data.fileType.Spectrum;
import equinox.data.fileType.StressSequence;
import equinox.task.InternalEquinoxTask.ShortRunningTask;

/**
 * Class for show STF files task.
 *
 * @author Murat Artim
 * @date 23 Jan 2017
 * @time 13:13:07
 */
public class ShowSTFFiles extends InternalEquinoxTask<ArrayList<STFFile>> implements ShortRunningTask {

	/** File bucket to show its STF files. */
	private final STFFileBucket bucket_;

	/**
	 * Creates show STF files task.
	 *
	 * @param bucket
	 *            File bucket to show its STF files.
	 */
	public ShowSTFFiles(STFFileBucket bucket) {
		bucket_ = bucket;
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	public String getTaskTitle() {
		return "Show STF files of '" + bucket_.getName() + "'";
	}

	@Override
	protected ArrayList<STFFile> call() throws Exception {

		// update message
		updateMessage("Loading STF files from datbase...");

		// initialize list
		ArrayList<STFFile> stfFiles = new ArrayList<>();

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// create statement
			try (Statement statement = connection.createStatement()) {

				// load STF files
				loadSTFFiles(stfFiles, statement, connection);
			}
		}

		// return STF files
		return stfFiles;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		try {

			// clear file selection
			taskPanel_.getOwner().getOwner().getInputPanel().clearFileSelection();

			// get STF files
			ArrayList<STFFile> stfFiles = get();

			// get spectrum
			Spectrum spectrum = bucket_.getParentItem();

			// remove file bucket from its spectrum
			spectrum.getChildren().remove(bucket_);

			// add all STF files to spectrum
			spectrum.getChildren().addAll(stfFiles);
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	/**
	 * Loads all STF files for the given spectrum.
	 *
	 * @param stfFiles
	 *            STF files to load.
	 * @param statement
	 *            Database statement.
	 * @param connection
	 *            Database connection.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void loadSTFFiles(ArrayList<STFFile> stfFiles, Statement statement, Connection connection) throws Exception {

		// prepare statement for getting stress sequences
		String sql = "select file_id, name from sth_files where stf_id = ? order by name";
		try (PreparedStatement getStressSequences = connection.prepareStatement(sql)) {

			// prepare statement for getting fatigue equivalent stresses
			sql = "select id, name, omission_level, material_name, material_specification, material_orientation, ";
			sql += "material_configuration from fatigue_equivalent_stresses where sth_id = ? ";
			sql += "order by material_name, material_specification";
			try (PreparedStatement getFatigueEqStresses = connection.prepareStatement(sql)) {

				// prepare statement for getting preffas equivalent stresses
				sql = "select id, name, omission_level, material_name, material_specification, material_orientation, material_configuration ";
				sql += "from preffas_equivalent_stresses where sth_id = ? ";
				sql += "order by material_name, material_specification";
				try (PreparedStatement getPreffasEqStresses = connection.prepareStatement(sql)) {

					// prepare statement for getting linear equivalent stresses
					sql = "select id, name, omission_level, material_name, material_specification, material_orientation, material_configuration ";
					sql += "from linear_equivalent_stresses where sth_id = ? ";
					sql += "order by material_name, material_specification";
					try (PreparedStatement getLinearEqStresses = connection.prepareStatement(sql)) {

						// prepare statement for getting typical flights
						sql = "select flight_id, name from sth_flights where file_id = ? order by flight_num";
						try (PreparedStatement getFlights = connection.prepareStatement(sql)) {

							// prepare statement for getting damage angles
							sql = "select angle_id, name, material_name, material_specification, material_orientation, ";
							sql += "material_configuration from maxdam_angles where stf_id = ? ";
							sql += "order by material_name, material_specification";
							try (PreparedStatement getDamageAngles = connection.prepareStatement(sql)) {

								// prepare statement for getting damage contributions
								sql = "select contributions_id, name, material_name, material_specification, material_orientation, ";
								sql += "material_configuration from dam_contributions where stf_id = ? ";
								sql += "order by material_name, material_specification";
								try (PreparedStatement getDamageContributions = connection.prepareStatement(sql)) {

									// prepare statement for getting typical flight damage contributions
									sql = "select id, name, material_name, material_specification, material_orientation, ";
									sql += "material_configuration from flight_dam_contributions where stf_id = ? ";
									sql += "order by material_name, material_specification";
									try (PreparedStatement getFlightDamageContributions = connection.prepareStatement(sql)) {

										// prepare statement for getting fast fatigue equivalent stresses
										sql = "select id, name, omission_level, material_name, material_specification, material_orientation, ";
										sql += "material_configuration from fast_fatigue_equivalent_stresses where stf_id = ? ";
										sql += "order by material_name, material_specification";
										try (PreparedStatement getFastFatigueEquivalentStresses = connection.prepareStatement(sql)) {

											// prepare statement for getting fast preffas equivalent stresses
											sql = "select id, name, omission_level, material_name, material_specification, material_orientation, material_configuration ";
											sql += "from fast_preffas_equivalent_stresses where stf_id = ? ";
											sql += "order by material_name, material_specification";
											try (PreparedStatement getFastPreffasEquivalentStresses = connection.prepareStatement(sql)) {

												// prepare statement for getting fast linear equivalent stresses
												sql = "select id, name, omission_level, material_name, material_specification, material_orientation, material_configuration ";
												sql += "from fast_linear_equivalent_stresses where stf_id = ? ";
												sql += "order by material_name, material_specification";
												try (PreparedStatement getFastLinearEquivalentStresses = connection.prepareStatement(sql)) {

													// get STF files
													sql = "select file_id, stress_table_id, name, is_2d, fat_mission, eid from stf_files where cdf_id = ";
													sql += bucket_.getParentItem().getID() + " order by name";
													try (ResultSet getSTFFiles = statement.executeQuery(sql)) {

														// loop over STF files
														int loadCount = 0;
														while (getSTFFiles.next()) {

															// task cancelled
															if (isCancelled())
																return;

															// increment load count
															loadCount++;

															// update progress
															updateProgress(loadCount, bucket_.getNumberOfSTFs());

															// create STF file
															STFFile stfFile = new STFFile(getSTFFiles.getString("name"), getSTFFiles.getInt("file_id"), getSTFFiles.getBoolean("is_2d"), getSTFFiles.getInt("stress_table_id"));

															// set fatigue mission
															stfFile.setMission(getSTFFiles.getString("fat_mission"));

															// set element ID
															stfFile.setEID(getSTFFiles.getString("eid"));

															// get damage angles
															getDamageAngles.setInt(1, stfFile.getID());
															try (ResultSet damageAngles = getDamageAngles.executeQuery()) {
																while (damageAngles.next()) {
																	String materialName = damageAngles.getString("material_name");
																	materialName += "/" + damageAngles.getString("material_specification");
																	materialName += "/" + damageAngles.getString("material_orientation");
																	materialName += "/" + damageAngles.getString("material_configuration");
																	stfFile.getChildren().add(new DamageAngle(damageAngles.getString("name"), damageAngles.getInt("angle_id"), materialName));
																}
															}

															// get damage contributions
															getDamageContributions.setInt(1, stfFile.getID());
															try (ResultSet damageContributions = getDamageContributions.executeQuery()) {
																while (damageContributions.next()) {
																	String materialName = damageContributions.getString("material_name");
																	materialName += "/" + damageContributions.getString("material_specification");
																	materialName += "/" + damageContributions.getString("material_orientation");
																	materialName += "/" + damageContributions.getString("material_configuration");
																	stfFile.getChildren().add(new LoadcaseDamageContributions(damageContributions.getString("name"), damageContributions.getInt("contributions_id"), materialName));
																}
															}

															// get typical flight damage contributions
															getFlightDamageContributions.setInt(1, stfFile.getID());
															try (ResultSet damageContributions = getFlightDamageContributions.executeQuery()) {
																while (damageContributions.next()) {
																	String materialName = damageContributions.getString("material_name");
																	materialName += "/" + damageContributions.getString("material_specification");
																	materialName += "/" + damageContributions.getString("material_orientation");
																	materialName += "/" + damageContributions.getString("material_configuration");
																	stfFile.getChildren().add(new FlightDamageContributions(damageContributions.getString("name"), damageContributions.getInt("id"), materialName));
																}
															}

															// get fast fatigue equivalent stresses
															getFastFatigueEquivalentStresses.setInt(1, stfFile.getID());
															try (ResultSet fastFatigueEquivalentStresses = getFastFatigueEquivalentStresses.executeQuery()) {
																while (fastFatigueEquivalentStresses.next()) {
																	double omissionLevel = fastFatigueEquivalentStresses.getDouble("omission_level");
																	String materialName = fastFatigueEquivalentStresses.getString("material_name");
																	materialName += "/" + fastFatigueEquivalentStresses.getString("material_specification");
																	materialName += "/" + fastFatigueEquivalentStresses.getString("material_orientation");
																	materialName += "/" + fastFatigueEquivalentStresses.getString("material_configuration");
																	stfFile.getChildren().add(new FastFatigueEquivalentStress(fastFatigueEquivalentStresses.getString("name"), fastFatigueEquivalentStresses.getInt("id"), omissionLevel, materialName));
																}
															}

															// get fast preffas equivalent stresses
															getFastPreffasEquivalentStresses.setInt(1, stfFile.getID());
															try (ResultSet fastPreffasEquivalentStresses = getFastPreffasEquivalentStresses.executeQuery()) {
																while (fastPreffasEquivalentStresses.next()) {
																	double omissionLevel = fastPreffasEquivalentStresses.getDouble("omission_level");
																	String materialName = fastPreffasEquivalentStresses.getString("material_name");
																	materialName += "/" + fastPreffasEquivalentStresses.getString("material_specification");
																	materialName += "/" + fastPreffasEquivalentStresses.getString("material_orientation");
																	materialName += "/" + fastPreffasEquivalentStresses.getString("material_configuration");
																	stfFile.getChildren().add(new FastPreffasEquivalentStress(fastPreffasEquivalentStresses.getString("name"), fastPreffasEquivalentStresses.getInt("id"), omissionLevel, materialName));
																}
															}

															// get fast linear equivalent stresses
															getFastLinearEquivalentStresses.setInt(1, stfFile.getID());
															try (ResultSet fastLinearEquivalentStresses = getFastLinearEquivalentStresses.executeQuery()) {
																while (fastLinearEquivalentStresses.next()) {
																	double omissionLevel = fastLinearEquivalentStresses.getDouble("omission_level");
																	String materialName = fastLinearEquivalentStresses.getString("material_name");
																	materialName += "/" + fastLinearEquivalentStresses.getString("material_specification");
																	materialName += "/" + fastLinearEquivalentStresses.getString("material_orientation");
																	materialName += "/" + fastLinearEquivalentStresses.getString("material_configuration");
																	stfFile.getChildren().add(new FastLinearEquivalentStress(fastLinearEquivalentStresses.getString("name"), fastLinearEquivalentStresses.getInt("id"), omissionLevel, materialName));
																}
															}

															// set STF file ID to stress sequence query
															getStressSequences.setInt(1, stfFile.getID());

															// get stress sequences
															try (ResultSet stressSequences = getStressSequences.executeQuery()) {

																// loop over stress sequences
																while (stressSequences.next()) {

																	// create stress sequence
																	StressSequence stressSequence = new StressSequence(stressSequences.getString("name"), stressSequences.getInt("file_id"));

																	// get typical flights
																	getFlights.setInt(1, stressSequence.getID());
																	try (ResultSet flights = getFlights.executeQuery()) {

																		// create flight folder
																		Flights flightFolder = new Flights(stressSequence.getID());

																		// add flights to folder
																		while (flights.next()) {
																			flightFolder.getChildren().add(new Flight(flights.getString("name"), flights.getInt("flight_id")));
																		}

																		// add flight folder to stress sequence
																		stressSequence.getChildren().add(flightFolder);
																	}

																	// get fatigue equivalent stresses
																	getFatigueEqStresses.setInt(1, stressSequence.getID());
																	try (ResultSet fatigueEqStresses = getFatigueEqStresses.executeQuery()) {

																		// loop over fatigue equivalent stresses
																		while (fatigueEqStresses.next()) {

																			// create fatigue equivalent stress
																			double omissionLevel = fatigueEqStresses.getDouble("omission_level");
																			String materialName = fatigueEqStresses.getString("material_name");
																			materialName += "/" + fatigueEqStresses.getString("material_specification");
																			materialName += "/" + fatigueEqStresses.getString("material_orientation");
																			materialName += "/" + fatigueEqStresses.getString("material_configuration");
																			stressSequence.getChildren().add(new FatigueEquivalentStress(fatigueEqStresses.getString("name"), fatigueEqStresses.getInt("id"), omissionLevel, materialName));
																		}
																	}

																	// get preffas equivalent stresses
																	getPreffasEqStresses.setInt(1, stressSequence.getID());
																	try (ResultSet preffasEqStresses = getPreffasEqStresses.executeQuery()) {

																		// loop over preffas equivalent stresses
																		while (preffasEqStresses.next()) {

																			// create preffas equivalent stress
																			double omissionLevel = preffasEqStresses.getDouble("omission_level");
																			String materialName = preffasEqStresses.getString("material_name");
																			materialName += "/" + preffasEqStresses.getString("material_specification");
																			materialName += "/" + preffasEqStresses.getString("material_orientation");
																			materialName += "/" + preffasEqStresses.getString("material_configuration");
																			stressSequence.getChildren().add(new PreffasEquivalentStress(preffasEqStresses.getString("name"), preffasEqStresses.getInt("id"), omissionLevel, materialName));
																		}
																	}

																	// get linear equivalent stresses
																	getLinearEqStresses.setInt(1, stressSequence.getID());
																	try (ResultSet linearEqStresses = getLinearEqStresses.executeQuery()) {

																		// loop over linear equivalent stresses
																		while (linearEqStresses.next()) {

																			// create linear equivalent stress
																			double omissionLevel = linearEqStresses.getDouble("omission_level");
																			String materialName = linearEqStresses.getString("material_name");
																			materialName += "/" + linearEqStresses.getString("material_specification");
																			materialName += "/" + linearEqStresses.getString("material_orientation");
																			materialName += "/" + linearEqStresses.getString("material_configuration");
																			stressSequence.getChildren().add(new LinearEquivalentStress(linearEqStresses.getString("name"), linearEqStresses.getInt("id"), omissionLevel, materialName));
																		}
																	}

																	// add STH file to STF file
																	stfFile.getChildren().add(stressSequence);
																}
															}

															// add STF file to the list
															stfFiles.add(stfFile);
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}
}
