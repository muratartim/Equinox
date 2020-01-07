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
import java.text.DecimalFormat;
import java.util.concurrent.ExecutionException;

import equinox.Equinox;
import equinox.controller.PlotViewPanel;
import equinox.data.fileType.Flight;
import equinox.data.fileType.Spectrum;
import equinox.task.InternalEquinoxTask.ShortRunningTask;

/**
 * Class for get peak info task.
 *
 * @author Murat Artim
 * @date Sep 3, 2014
 * @time 2:36:36 PM
 */
public class GetPeakInfo extends InternalEquinoxTask<String> implements ShortRunningTask {

	/** Data label option index. */
	public static final int PEAK_VALUE = 0, CLASS_CODE = 1, ONE_G_FLIGHT_PHASE = 2, ONE_G_ISSY_CODE = 3, ONE_G_STRESS = 4, ONE_G_COMMENT = 5, INCREMENT_FLIGHT_PHASE = 6, INCREMENT_ISSY_CODE = 7, INCREMENT_FACTOR = 8, INCREMENT_STRESS = 9, INCREMENT_COMMENT = 10, DELTA_P_PRESSURE = 11,
			DELTA_P_STRESS = 12, LINEARITY = 13, SEGMENT = 14, PEAK_NUMBER = 15, DELTA_T_TEMPERATURE = 16, DELTA_T_STRESS = 17;

	/** Decimal format. */
	private final DecimalFormat format_ = new DecimalFormat("#.###");

	/** Flights to plot. */
	private final Flight flight_;

	/** Peak info. */
	private final int info_, peakNum_;

	/** The owner panel. */
	private final PlotViewPanel panel_;

	/**
	 * Creates get peak info task.
	 *
	 * @param flight
	 *            Flight.
	 * @param peakNum
	 *            Peak number.
	 * @param info
	 *            Demanded info.
	 * @param panel
	 *            The owner panel.
	 */
	public GetPeakInfo(Flight flight, int peakNum, int info, PlotViewPanel panel) {
		flight_ = flight;
		peakNum_ = peakNum;
		info_ = info;
		panel_ = panel;
	}

	@Override
	public String getTaskTitle() {
		return "Get peak info";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	protected String call() throws Exception {

		// update progress info
		updateTitle("Getting peak info...");

		// initialize info
		String info = null;

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// create statement
			try (Statement statement = connection.createStatement()) {

				// stresses
				if ((info_ == PEAK_VALUE) || (info_ == ONE_G_STRESS) || (info_ == INCREMENT_STRESS) || (info_ == DELTA_P_STRESS) || (info_ == DELTA_T_STRESS) || (info_ == SEGMENT)) {
					info = getStressInfo(statement);
				}
				else if ((info_ == CLASS_CODE) || (info_ == DELTA_P_PRESSURE) || (info_ == DELTA_T_TEMPERATURE)) {
					info = getANAInfo(statement);
				}
				else if ((info_ == ONE_G_FLIGHT_PHASE) || (info_ == ONE_G_ISSY_CODE) || (info_ == ONE_G_COMMENT)) {
					info = get1GInfo(statement);
				}
				else if ((info_ == INCREMENT_FLIGHT_PHASE) || (info_ == INCREMENT_ISSY_CODE) || (info_ == INCREMENT_FACTOR) || (info_ == INCREMENT_COMMENT) || (info_ == LINEARITY)) {
					info = getIncrementInfo(connection, statement);
				}
			}
		}

		// return info
		return info;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set peak info
		try {
			panel_.setPeakInfo(get());
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	/**
	 * Retrieves increment info from database.
	 *
	 * @param connection
	 *            Database connection.
	 * @param statement
	 *            Database statement.
	 * @return Increment info.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private String getIncrementInfo(Connection connection, Statement statement) throws Exception {

		// get file IDs
		Spectrum cdfSet = flight_.getParentItem().getParentItem().getParentItem().getParentItem();
		int anaFileID = cdfSet.getANAFileID();
		int txtFileID = cdfSet.getTXTFileID();
		int convTableID = cdfSet.getConversionTableID();

		// get class code
		String classCode = null;
		int anaFlightID = getANAFlightID(anaFileID, statement);
		String sql = "select fourteen_digit_code";
		sql += " from ana_peaks_" + anaFileID;
		sql += " where flight_id = " + anaFlightID;
		sql += " and peak_num = " + peakNum_;
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				classCode = resultSet.getString("fourteen_digit_code");
				break;
			}
		}

		// class code couldn't be found
		if (classCode == null)
			throw new Exception("ANA class code couldn't be obtained from database.");

		// initialize info string
		String info = "";

		// prepare statement for getting increment info
		sql = "select flight_phase, issy_code, factor_1, factor_2, factor_3, factor_4, factor_5, factor_6, factor_7, factor_8, nl_factor_num from txt_codes where file_id = ? and one_g_code = ? and increment_num = ? and direction_num = ? and (nl_factor_num is null or nl_factor_num = ?)";
		try (PreparedStatement getIncrementInfo = connection.prepareStatement(sql)) {

			// prepare statement for getting comment
			sql = "select comment from xls_comments where file_id = ? and issy_code = ? and fue_translated like ?";
			try (PreparedStatement getComment = connection.prepareStatement(sql)) {

				// loop over increments
				for (int i = 0; i < 5; i++) {

					// get increment block
					String block = classCode.substring((2 * i) + 4, (2 * i) + 6);

					// no increment
					if (block.equals("00")) {
						continue;
					}

					// get flight phase, 1g code and factor
					String flightPhase = null;
					String issyCode = null;
					double factor = 0.0;
					boolean isNonlinear = false;
					getIncrementInfo.setInt(1, txtFileID); // file ID
					getIncrementInfo.setString(2, classCode.substring(0, 4)); // 1g code
					getIncrementInfo.setInt(3, i + 1); // increment number
					getIncrementInfo.setString(4, block.substring(1)); // direction number
					getIncrementInfo.setString(5, block.substring(0, 1)); // factor number
					try (ResultSet resultSet = getIncrementInfo.executeQuery()) {
						while (resultSet.next()) {
							flightPhase = resultSet.getString("flight_phase");
							issyCode = resultSet.getString("issy_code");
							factor = resultSet.getDouble("factor_" + block.substring(0, 1));
							isNonlinear = resultSet.getInt("nl_factor_num") != 0;
						}
					}

					// add increment info to data label
					if (info_ == INCREMENT_FLIGHT_PHASE) {
						info += "Increment-" + (i + 1) + " event: " + flightPhase + "\n";
					}
					else if (info_ == INCREMENT_ISSY_CODE) {
						info += "Increment-" + (i + 1) + " ISSY code: " + issyCode + "\n";
					}
					else if (info_ == INCREMENT_FACTOR) {
						info += "Increment-" + (i + 1) + " factor: " + format_.format(factor) + "\n";
					}
					else if (info_ == INCREMENT_COMMENT) {
						info += "Increment-" + (i + 1) + " comment: " + getIncrementComment(convTableID, getComment, issyCode, flightPhase) + "\n";
					}
					else if (info_ == LINEARITY) {
						info += "Increment-" + (i + 1) + " linearity: " + (isNonlinear ? "Nonlinear" : "Linear") + "\n";
					}
				}
			}
		}

		// increment info couldn't be found
		if (info.isEmpty())
			return "No increment found for peak.";

		// return info
		return info.substring(0, info.lastIndexOf("\n"));
	}

	/**
	 * Retrieves 1g info from database.
	 *
	 * @param statement
	 *            Database statement.
	 * @return 1G info.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private String get1GInfo(Statement statement) throws Exception {

		// get file IDs
		Spectrum cdfSet = flight_.getParentItem().getParentItem().getParentItem().getParentItem();
		int anaFileID = cdfSet.getANAFileID();
		int txtFileID = cdfSet.getTXTFileID();
		int convTableID = cdfSet.getConversionTableID();

		// get class code
		String classCode = null;
		int anaFlightID = getANAFlightID(anaFileID, statement);
		String sql = "select fourteen_digit_code";
		sql += " from ana_peaks_" + anaFileID;
		sql += " where flight_id = " + anaFlightID;
		sql += " and peak_num = " + peakNum_;
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				classCode = resultSet.getString("fourteen_digit_code");
				break;
			}
		}

		// class code couldn't be found
		if (classCode == null)
			throw new Exception("ANA class code couldn't be obtained from database.");

		// get 1G info
		String event = null, issyCode = null;
		sql = "select flight_phase, issy_code from txt_codes where file_id = " + txtFileID;
		sql += " and one_g_code = '" + classCode.substring(0, 4) + "'";
		sql += " and increment_num = 0";
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				event = resultSet.getString("flight_phase");
				issyCode = resultSet.getString("issy_code");
				break;
			}
		}

		// 1g info couldn't be found
		if ((event == null) || (issyCode == null))
			throw new Exception("1G info couldn't be obtained from database.");

		// return 1g info
		if (info_ == ONE_G_FLIGHT_PHASE)
			return "1g event: " + event;
		else if (info_ == ONE_G_ISSY_CODE)
			return "1g ISSY code: " + issyCode;
		else if (info_ == ONE_G_COMMENT)
			return "1g comment: " + get1GComment(convTableID, statement, issyCode, event);
		throw new Exception("1G info couldn't be obtained from database.");
	}

	/**
	 * Returns the conversion table comment for the given flight phase.
	 *
	 * @param convTableID
	 *            Conversion table ID.
	 * @param getComment
	 *            Database statement.
	 * @param issyCode
	 *            ISSY code.
	 * @param flightPhase
	 *            Flight phase (FUE translated).
	 * @return Conversion table comment.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static String getIncrementComment(int convTableID, PreparedStatement getComment, String issyCode, String flightPhase) throws Exception {

		// no conversion table ID given
		if ((convTableID == 0) || (issyCode == null))
			return null;

		// get comment for given issy code
		getComment.setInt(1, convTableID); // conversion table ID
		getComment.setString(2, issyCode); // issy code
		getComment.setString(3, "%" + flightPhase + "%"); // FUE translated
		try (ResultSet resultSet = getComment.executeQuery()) {
			while (resultSet.next())
				return resultSet.getString("comment");
		}

		// no comment found
		return null;
	}

	/**
	 * Returns the conversion table comment for the given flight phase.
	 *
	 * @param convTableID
	 *            Conversion table ID.
	 * @param statement
	 *            Database statement.
	 * @param issyCode
	 *            ISSY code.
	 * @param flightPhase
	 *            Flight phase (FUE translated).
	 * @return Conversion table comment.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static String get1GComment(int convTableID, Statement statement, String issyCode, String flightPhase) throws Exception {
		String sql = "select comment from xls_comments where file_id = " + convTableID;
		sql += " and issy_code = '" + issyCode + "' and fue_translated like '%" + flightPhase + "%'";
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next())
				return resultSet.getString("comment");
		}
		return "N/A";
	}

	/**
	 * Returns stress info.
	 *
	 * @param statement
	 *            Database statement.
	 * @return Stress info.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private String getStressInfo(Statement statement) throws Exception {

		// create query
		String sql = "select ";
		if (info_ == PEAK_VALUE) {
			sql += "peak_val";
		}
		else if (info_ == ONE_G_STRESS) {
			sql += "oneg_stress";
		}
		else if (info_ == INCREMENT_STRESS) {
			sql += "inc_stress";
		}
		else if (info_ == DELTA_P_STRESS) {
			sql += "dp_stress";
		}
		else if (info_ == DELTA_T_STRESS) {
			sql += "dt_stress";
		}
		else if (info_ == SEGMENT) {
			sql += "segment, segment_num";
		}
		sql += " from sth_peaks_" + flight_.getParentItem().getParentItem().getID();
		sql += " where flight_id = " + flight_.getID();
		sql += " and peak_num = " + peakNum_;

		// get stress info
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				if (info_ == PEAK_VALUE)
					return "Total stress: " + format_.format(resultSet.getDouble("peak_val"));
				else if (info_ == ONE_G_STRESS)
					return "1g stress: " + format_.format(resultSet.getDouble("oneg_stress"));
				else if (info_ == INCREMENT_STRESS)
					return "Increment stress: " + format_.format(resultSet.getDouble("inc_stress"));
				else if (info_ == DELTA_P_STRESS)
					return "Delta-p stress: " + format_.format(resultSet.getDouble("dp_stress"));
				else if (info_ == DELTA_T_STRESS)
					return "Delta-t stress: " + format_.format(resultSet.getDouble("dt_stress"));
				else if (info_ == SEGMENT)
					return "Segment: " + resultSet.getString("segment") + " (" + resultSet.getInt("segment_num") + ")";
			}
		}
		throw new Exception("Stress info couldn't be obtained from database.");
	}

	/**
	 * Gets ANA info from database.
	 *
	 * @param statement
	 *            Database statement.
	 * @return ANA info.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private String getANAInfo(Statement statement) throws Exception {

		// get ANA file ID
		int anaFileID = flight_.getParentItem().getParentItem().getParentItem().getParentItem().getANAFileID();
		int anaFlightID = getANAFlightID(anaFileID, statement);

		// create query
		String sql = "select ";
		if (info_ == CLASS_CODE) {
			sql += "fourteen_digit_code";
		}
		else if (info_ == DELTA_P_PRESSURE) {
			sql += "delta_p";
		}
		else if (info_ == DELTA_T_TEMPERATURE) {
			sql += "delta_t";
		}
		sql += " from ana_peaks_" + anaFileID;
		sql += " where flight_id = " + anaFlightID;
		sql += " and peak_num = " + peakNum_;

		// get ANA info
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				if (info_ == CLASS_CODE)
					return "Class code: " + resultSet.getString("fourteen_digit_code");
				else if (info_ == DELTA_P_PRESSURE)
					return "Delta-p pressure: " + format_.format(resultSet.getDouble("delta_p"));
				else if (info_ == DELTA_T_TEMPERATURE)
					return "Delta-t temperature: " + format_.format(resultSet.getDouble("delta_t"));
			}
		}
		throw new Exception("ANA info couldn't be obtained from database.");
	}

	/**
	 * Returns ANA flight ID.
	 *
	 * @param anaFileID
	 *            ANA file ID.
	 * @param statement
	 *            Database statement.
	 * @return ANA flight ID.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private int getANAFlightID(int anaFileID, Statement statement) throws Exception {
		int sthFlightNumber = getSTHFlightNumber(statement);
		String sql = "select flight_id from ana_flights where file_id = " + anaFileID + " and flight_num = " + sthFlightNumber;
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next())
				return resultSet.getInt("flight_id");
		}
		throw new Exception("ANA flight ID couldn't be obtained from database.");
	}

	/**
	 * Returns STH flight number.
	 *
	 * @param statement
	 *            Database statement.
	 * @return STH flight number.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private int getSTHFlightNumber(Statement statement) throws Exception {
		int sthFileID = flight_.getParentItem().getParentItem().getID();
		String sql = "select flight_num from sth_flights where file_id = " + sthFileID + " and flight_id = " + flight_.getID();
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next())
				return resultSet.getInt("flight_num");
		}
		throw new Exception("Spectrum flight number couldn't be obtained from database.");
	}
}
