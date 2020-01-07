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
package equinox.process;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import equinox.Equinox;
import equinox.data.ProgramArguments.ArgumentType;
import equinox.data.fileType.AircraftEquivalentStresses;
import equinox.data.fileType.AircraftFatigueEquivalentStress;
import equinox.data.fileType.AircraftLinearEquivalentStress;
import equinox.data.fileType.AircraftLoadCase;
import equinox.data.fileType.AircraftLoadCases;
import equinox.data.fileType.AircraftModel;
import equinox.data.fileType.AircraftPreffasEquivalentStress;
import equinox.data.fileType.DamageAngle;
import equinox.data.fileType.ExternalFatigueEquivalentStress;
import equinox.data.fileType.ExternalFlight;
import equinox.data.fileType.ExternalFlights;
import equinox.data.fileType.ExternalLinearEquivalentStress;
import equinox.data.fileType.ExternalPreffasEquivalentStress;
import equinox.data.fileType.ExternalStressSequence;
import equinox.data.fileType.FastFatigueEquivalentStress;
import equinox.data.fileType.FastLinearEquivalentStress;
import equinox.data.fileType.FastPreffasEquivalentStress;
import equinox.data.fileType.FatigueEquivalentStress;
import equinox.data.fileType.Flight;
import equinox.data.fileType.FlightDamageContributions;
import equinox.data.fileType.Flights;
import equinox.data.fileType.LinearEquivalentStress;
import equinox.data.fileType.LoadcaseDamageContributions;
import equinox.data.fileType.PilotPoint;
import equinox.data.fileType.PilotPoints;
import equinox.data.fileType.PreffasEquivalentStress;
import equinox.data.fileType.Rfort;
import equinox.data.fileType.STFFile;
import equinox.data.fileType.STFFileBucket;
import equinox.data.fileType.Spectrum;
import equinox.data.fileType.SpectrumItem;
import equinox.data.fileType.StressSequence;
import equinox.data.input.AircraftEquivalentStressType;
import equinox.task.InternalEquinoxTask;

/**
 * Class for load all files process.
 *
 * @author Murat Artim
 * @date Dec 4, 2014
 * @time 11:11:33 AM
 */
public class LoadAllFilesProcess implements EquinoxProcess<ArrayList<SpectrumItem>> {

	/** The owner task. */
	private final InternalEquinoxTask<?> task_;

	/**
	 * Creates load all files process.
	 *
	 * @param task
	 *            The owner task.
	 */
	public LoadAllFilesProcess(InternalEquinoxTask<?> task) {
		task_ = task;
	}

	@Override
	public ArrayList<SpectrumItem> start(Connection connection, PreparedStatement... preparedStatements) throws Exception {

		// create files list
		ArrayList<SpectrumItem> files = new ArrayList<>();

		// get maximum visible STF files per spectrum
		int maxVisibleSTFs = Integer.parseInt(Equinox.ARGUMENTS.getArgument(ArgumentType.MAX_VISIBLE_STFS_PER_SPECTRUM));

		// create statement
		try (Statement statement = connection.createStatement()) {

			// get spectra
			task_.updateMessage("Loading spectra...");
			getSpectra(connection, statement, files, maxVisibleSTFs);

			// get external stress sequences
			task_.updateMessage("Loading external stress sequences...");
			getExternalStressSequences(connection, statement, files);

			// get aircraft models
			task_.updateMessage("Loading aircraft models...");
			getAircraftModels(connection, statement, files);

			// get RFORT files
			task_.updateMessage("Loading RFORT files...");
			getRfortFiles(connection, statement, files);
		}
		return files;
	}

	/**
	 * Gets RFORT files from the database.
	 *
	 * @param connection
	 *            Database connection.
	 * @param statement
	 *            Database statement.
	 * @param files
	 *            List to store the spectrum files.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static void getRfortFiles(Connection connection, Statement statement, ArrayList<SpectrumItem> files) throws Exception {

		// check if RFORT tables exist
		boolean analysisTableExists = false;
		DatabaseMetaData dbmtadta = connection.getMetaData();
		try (ResultSet resultSet = dbmtadta.getTables(null, "AURORA", "RFORT_%", null)) {
			while (resultSet.next()) {

				// get table name
				String tableName = resultSet.getString(3);

				// analysis table
				if (tableName.equalsIgnoreCase("rfort_analyses")) {
					analysisTableExists = true;
				}
			}
		}

		// analysis table doesn't exist
		if (!analysisTableExists)
			return;

		// get RFORT files
		String sql = "select all id, input_spectrum_name, fatigue_analysis, preffas_analysis, linear_analysis from rfort_analyses order by input_spectrum_name";
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				int id = resultSet.getInt("id");
				String name = resultSet.getString("input_spectrum_name");
				boolean fatigue = resultSet.getBoolean("fatigue_analysis");
				boolean preffas = resultSet.getBoolean("preffas_analysis");
				boolean linear = resultSet.getBoolean("linear_analysis");
				files.add(new Rfort(name, id, fatigue, preffas, linear));
			}
		}
	}

	/**
	 * Gets aircraft models from the database.
	 *
	 * @param connection
	 *            Database connection.
	 * @param statement
	 *            Database statement.
	 * @param files
	 *            List to store the spectrum files.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static void getAircraftModels(Connection connection, Statement statement, ArrayList<SpectrumItem> files) throws Exception {

		// get database meta data
		DatabaseMetaData dbmtadta = connection.getMetaData();

		// get A/C models
		String sql = "select all * from ac_models order by ac_program, name";
		try (ResultSet getModel = statement.executeQuery(sql)) {
			while (getModel.next()) {

				// create A/C model
				int id = getModel.getInt("model_id");
				String program = getModel.getString("ac_program");
				String name = getModel.getString("name");
				AircraftModel model = new AircraftModel(program, name, id);

				// create load cases folder
				AircraftLoadCases loadCases = new AircraftLoadCases(id);

				// check if there are load cases
				try (ResultSet resultSet = dbmtadta.getTables(null, "AURORA", "LOAD_CASE_NAMES_" + id, null)) {
					while (resultSet.next()) {

						// add load cases
						sql = "select lc_id, lc_name, lc_num from load_case_names_" + id + " order by lc_name, lc_num";
						try (Statement statement2 = connection.createStatement()) {
							try (ResultSet getLoadCases = statement2.executeQuery(sql)) {
								while (getLoadCases.next()) {
									int loadCaseID = getLoadCases.getInt("lc_id");
									String loadCaseName = getLoadCases.getString("lc_name");
									int loadCaseNum = getLoadCases.getInt("lc_num");
									loadCases.getChildren().add(new AircraftLoadCase(loadCaseID, loadCaseName, loadCaseNum));
								}
							}
						}
					}
				}

				// add load cases to model
				model.getChildren().add(loadCases);

				// create equivalent stresses folder
				AircraftEquivalentStresses equivalentStresses = new AircraftEquivalentStresses(id);

				// check if there are equivalent stresses
				try (ResultSet resultSet = dbmtadta.getTables(null, "AURORA", "AC_EQ_STRESS_NAMES_" + id, null)) {
					while (resultSet.next()) {

						// add equivalent stresses
						sql = "select id, name, stress_type from AC_EQ_STRESS_NAMES_" + id + " order by name";
						try (Statement statement2 = connection.createStatement()) {
							try (ResultSet getEquivalentStresses = statement2.executeQuery(sql)) {
								while (getEquivalentStresses.next()) {

									// get attributes
									int stressID = getEquivalentStresses.getInt("id");
									String stressName = getEquivalentStresses.getString("name");
									String stressType = getEquivalentStresses.getString("stress_type");

									// fatigue equivalent stress
									if (stressType.equals(AircraftEquivalentStressType.FATIGUE_EQUIVALENT_STRESS.toString())) {
										equivalentStresses.getChildren().add(new AircraftFatigueEquivalentStress(stressID, stressName));
									}
									else if (stressType.equals(AircraftEquivalentStressType.PREFFAS_PROPAGATION_EQUIVALENT_STRESS.toString())) {
										equivalentStresses.getChildren().add(new AircraftPreffasEquivalentStress(stressID, stressName));
									}
									else if (stressType.equals(AircraftEquivalentStressType.LINEAR_PROPAGATION_EQUIVALENT_STRESS.toString())) {
										equivalentStresses.getChildren().add(new AircraftLinearEquivalentStress(stressID, stressName));
									}
								}
							}
						}
					}
				}

				// add equivalent stresses to model
				model.getChildren().add(equivalentStresses);

				// create pilot points folder
				PilotPoints pilotPoints = new PilotPoints(id);

				// check if there are pilot points
				try (ResultSet resultSet = dbmtadta.getTables(null, "AURORA", "PILOT_POINTS_" + id, null)) {
					while (resultSet.next()) {

						// add pilot points
						sql = "select pp_id, stf_id, stf_files.name from pilot_points_" + id;
						sql += " inner join stf_files on pilot_points_" + id + ".stf_id = stf_files.file_id";
						sql += " order by stf_files.name";
						try (Statement statement2 = connection.createStatement()) {
							try (ResultSet getPilotPoints = statement2.executeQuery(sql)) {
								while (getPilotPoints.next()) {
									String stfFileName = getPilotPoints.getString("name");
									int ppID = getPilotPoints.getInt("pp_id");
									int stfID = getPilotPoints.getInt("stf_id");
									pilotPoints.getChildren().add(new PilotPoint(ppID, stfFileName, stfID));
								}
							}
						}
					}
				}

				// add pilot points to model
				model.getChildren().add(pilotPoints);

				// add model to files
				files.add(model);
			}
		}
	}

	/**
	 * Gets external stress sequences from the database.
	 *
	 * @param connection
	 *            Database connection.
	 * @param statement
	 *            Database statement.
	 * @param files
	 *            List to store the spectrum files.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static void getExternalStressSequences(Connection connection, Statement statement, ArrayList<SpectrumItem> files) throws Exception {

		// get external sequences
		String sql = "select all * from ext_sth_files order by name";
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				String name = resultSet.getString("name");
				int id = resultSet.getInt("file_id");
				String program = resultSet.getString("ac_program");
				String section = resultSet.getString("ac_section");
				String mission = resultSet.getString("fat_mission");
				ExternalStressSequence sequence = new ExternalStressSequence(name, id);
				sequence.setMission(mission);
				sequence.setProgram(program);
				sequence.setSection(section);
				files.add(sequence);
			}
		}

		// prepare statement for getting external fatigue equivalent stresses
		sql = "select id, name, omission_level, material_name, material_specification, material_orientation, material_configuration ";
		sql += "from ext_fatigue_equivalent_stresses where sth_id = ? order by material_name, material_specification";
		try (PreparedStatement getFatigueEqStresses = connection.prepareStatement(sql)) {

			// prepare statement for getting external preffas equivalent stresses
			sql = "select id, name, omission_level, material_name, material_specification, material_orientation, material_configuration ";
			sql += "from ext_preffas_equivalent_stresses where sth_id = ? ";
			sql += "order by material_name, material_specification";
			try (PreparedStatement getPreffasEqStresses = connection.prepareStatement(sql)) {

				// prepare statement for getting external linear equivalent stresses
				sql = "select id, name, omission_level, material_name, material_specification, material_orientation, material_configuration ";
				sql += "from ext_linear_equivalent_stresses where sth_id = ? ";
				sql += "order by material_name, material_specification";
				try (PreparedStatement getLinearEqStresses = connection.prepareStatement(sql)) {

					// prepare statement for getting external STH flights
					sql = "select flight_id, name from ext_sth_flights where file_id = ? order by flight_num";
					try (PreparedStatement getFlights = connection.prepareStatement(sql)) {

						// loop over files
						for (SpectrumItem file : files) {

							// not external stress sequence
							if ((file instanceof ExternalStressSequence) == false) {
								continue;
							}

							// cast to external stress sequence
							ExternalStressSequence sequence = (ExternalStressSequence) file;

							// get external typical flights
							getFlights.setInt(1, sequence.getID());
							try (ResultSet flights = getFlights.executeQuery()) {

								// create external flights folder
								ExternalFlights flightFolder = new ExternalFlights(sequence.getID());

								// add external flights to folder
								while (flights.next()) {
									flightFolder.getChildren().add(new ExternalFlight(flights.getString("name"), flights.getInt("flight_id")));
								}

								// add external folder to external sequence
								sequence.getChildren().add(flightFolder);
							}

							// get external fatigue equivalent stresses
							getFatigueEqStresses.setInt(1, sequence.getID());
							try (ResultSet fatigueEqStresses = getFatigueEqStresses.executeQuery()) {

								// loop over external fatigue equivalent stresses
								while (fatigueEqStresses.next()) {

									// create external fatigue equivalent stress
									double omissionLevel = fatigueEqStresses.getDouble("omission_level");
									String materialName = fatigueEqStresses.getString("material_name");
									materialName += "/" + fatigueEqStresses.getString("material_specification");
									materialName += "/" + fatigueEqStresses.getString("material_orientation");
									materialName += "/" + fatigueEqStresses.getString("material_configuration");
									sequence.getChildren().add(new ExternalFatigueEquivalentStress(fatigueEqStresses.getString("name"), fatigueEqStresses.getInt("id"), omissionLevel, materialName));
								}
							}

							// get external preffas equivalent stresses
							getPreffasEqStresses.setInt(1, sequence.getID());
							try (ResultSet preffasEqStresses = getPreffasEqStresses.executeQuery()) {

								// loop over external preffas equivalent stresses
								while (preffasEqStresses.next()) {

									// create external preffas equivalent stress
									double omissionLevel = preffasEqStresses.getDouble("omission_level");
									String materialName = preffasEqStresses.getString("material_name");
									materialName += "/" + preffasEqStresses.getString("material_specification");
									materialName += "/" + preffasEqStresses.getString("material_orientation");
									materialName += "/" + preffasEqStresses.getString("material_configuration");
									sequence.getChildren().add(new ExternalPreffasEquivalentStress(preffasEqStresses.getString("name"), preffasEqStresses.getInt("id"), omissionLevel, materialName));
								}
							}

							// get external linear equivalent stresses
							getLinearEqStresses.setInt(1, sequence.getID());
							try (ResultSet linearEqStresses = getLinearEqStresses.executeQuery()) {

								// loop over external linear equivalent stresses
								while (linearEqStresses.next()) {

									// create external linear equivalent stress
									double omissionLevel = linearEqStresses.getDouble("omission_level");
									String materialName = linearEqStresses.getString("material_name");
									materialName += "/" + linearEqStresses.getString("material_specification");
									materialName += "/" + linearEqStresses.getString("material_orientation");
									materialName += "/" + linearEqStresses.getString("material_configuration");
									sequence.getChildren().add(new ExternalLinearEquivalentStress(linearEqStresses.getString("name"), linearEqStresses.getInt("id"), omissionLevel, materialName));
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Gets spectra from the database.
	 *
	 * @param connection
	 *            Database connection.
	 * @param statement
	 *            Database statement.
	 * @param files
	 *            List to store the spectrum files.
	 * @param maxVisibleSTFs
	 *            Maximum visible STF files per spectrum.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static void getSpectra(Connection connection, Statement statement, ArrayList<SpectrumItem> files, int maxVisibleSTFs) throws Exception {

		// get spectra
		String sql = "select all set_id, name, ac_program, ac_section, fat_mission from cdf_sets order by name";
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				String name = resultSet.getString("name");
				int id = resultSet.getInt("set_id");
				String program = resultSet.getString("ac_program");
				String section = resultSet.getString("ac_section");
				String mission = resultSet.getString("fat_mission");
				Spectrum spectrum = new Spectrum(name, id);
				spectrum.setMission(mission);
				spectrum.setProgram(program);
				spectrum.setSection(section);
				files.add(spectrum);
			}
		}

		// no spectrum found
		if (files.isEmpty())
			return;

		// get ANA files
		sql = "select file_id from ana_files where cdf_id = ?";
		try (PreparedStatement statement2 = connection.prepareStatement(sql)) {
			for (SpectrumItem item : files) {
				Spectrum spectrum = (Spectrum) item;
				statement2.setInt(1, spectrum.getID());
				try (ResultSet resultSet = statement2.executeQuery()) {
					while (resultSet.next()) {
						spectrum.setANAFileID(resultSet.getInt("file_id"));
					}
				}
			}
		}

		// load TXT files
		sql = "select file_id from txt_files where cdf_id = ?";
		try (PreparedStatement statement2 = connection.prepareStatement(sql)) {
			for (SpectrumItem item : files) {
				Spectrum spectrum = (Spectrum) item;
				statement2.setInt(1, spectrum.getID());
				try (ResultSet resultSet = statement2.executeQuery()) {
					while (resultSet.next()) {
						spectrum.setTXTFileID(resultSet.getInt("file_id"));
					}
				}
			}
		}

		// load FLS files
		sql = "select file_id from fls_files where cdf_id = ?";
		try (PreparedStatement statement2 = connection.prepareStatement(sql)) {
			for (SpectrumItem item : files) {
				Spectrum spectrum = (Spectrum) item;
				statement2.setInt(1, spectrum.getID());
				try (ResultSet resultSet = statement2.executeQuery()) {
					while (resultSet.next()) {
						spectrum.setFLSFileID(resultSet.getInt("file_id"));
					}
				}
			}
		}

		// load CVT files
		sql = "select file_id from cvt_files where cdf_id = ?";
		try (PreparedStatement statement2 = connection.prepareStatement(sql)) {
			for (SpectrumItem item : files) {
				Spectrum spectrum = (Spectrum) item;
				statement2.setInt(1, spectrum.getID());
				try (ResultSet resultSet = statement2.executeQuery()) {
					while (resultSet.next()) {
						spectrum.setCVTFileID(resultSet.getInt("file_id"));
					}
				}
			}
		}

		// load conversion tables
		sql = "select file_id from xls_files where cdf_id = ?";
		try (PreparedStatement statement2 = connection.prepareStatement(sql)) {
			for (SpectrumItem item : files) {
				Spectrum spectrum = (Spectrum) item;
				statement2.setInt(1, spectrum.getID());
				try (ResultSet resultSet = statement2.executeQuery()) {
					while (resultSet.next()) {
						spectrum.setConvTableID(resultSet.getInt("file_id"));
					}
				}
			}
		}

		// prepare statement for checking number of STF files
		sql = "select count(file_id) as stfcount from stf_files where cdf_id = ?";
		try (PreparedStatement countSTFFiles = connection.prepareStatement(sql)) {

			// prepare statement for getting STF files
			sql = "select file_id, stress_table_id, name, is_2d, fat_mission, eid from stf_files where cdf_id = ? order by name";
			try (PreparedStatement getSTFFiles = connection.prepareStatement(sql)) {

				// set maximum number of visible STF files
				getSTFFiles.setMaxRows(maxVisibleSTFs);

				// prepare statement for getting stress sequences
				sql = "select file_id, name from sth_files where stf_id = ? order by name";
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

															// loop over spectra
															spectrumLoop: for (SpectrumItem spectrum : files) {

																// too many STF files to show (add bucket)
																countSTFFiles.setInt(1, spectrum.getID());
																try (ResultSet stfFileCount = countSTFFiles.executeQuery()) {
																	while (stfFileCount.next()) {
																		int stfCount = stfFileCount.getInt("stfcount");
																		if (stfCount > maxVisibleSTFs) {
																			spectrum.getChildren().add(new STFFileBucket(spectrum.getID(), stfCount));
																			continue spectrumLoop;
																		}
																	}
																}

																// get STF files
																getSTFFiles.setInt(1, spectrum.getID());
																try (ResultSet stfFiles = getSTFFiles.executeQuery()) {

																	// loop over STF files
																	while (stfFiles.next()) {

																		// create STF file
																		STFFile stfFile = new STFFile(stfFiles.getString("name"), stfFiles.getInt("file_id"), stfFiles.getBoolean("is_2d"), stfFiles.getInt("stress_table_id"));

																		// set fatigue mission
																		stfFile.setMission(stfFiles.getString("fat_mission"));

																		// set element ID
																		stfFile.setEID(stfFiles.getString("eid"));

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

																		// add STF files to spectrum
																		spectrum.getChildren().add(stfFile);
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
		}
	}
}
