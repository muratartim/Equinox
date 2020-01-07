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
import equinox.controller.MissionProfileComparisonViewPanel;
import equinox.controller.MissionProfileViewPanel;
import equinox.controller.ViewPanel;
import equinox.data.fileType.Spectrum;
import equinox.data.fileType.StressSequence;
import equinox.task.InternalEquinoxTask.ShortRunningTask;

/**
 * Class for get mission profile peak info task.
 *
 * @author Murat Artim
 * @date Jun 9, 2016
 * @time 1:37:30 PM
 */
public class GetMissionProfilePeakInfo extends InternalEquinoxTask<String> implements ShortRunningTask {

	/** Data label option index. */
	public static final int TOTAL_STRESS = 0, CLASS_CODE = 1, ONE_G_FLIGHT_PHASE = 2, ONE_G_ISSY_CODE = 3, ONE_G_STRESS = 4, ONE_G_COMMENT = 5, INCREMENT_FLIGHT_PHASE = 6, INCREMENT_ISSY_CODE = 7, INCREMENT_FACTOR = 8, INCREMENT_STRESS = 9, INCREMENT_COMMENT = 10, DELTA_P_PRESSURE = 11,
			DELTA_P_STRESS = 12, LINEARITY = 13, SEGMENT = 14, FLIGHT_NAME = 15, DELTA_T_TEMPERATURE = 16, DELTA_T_STRESS = 17;

	/** Peak stress search tolerance. */
	private static final double TOLERANCE = 0.0001;

	/** Decimal format. */
	private final DecimalFormat format_ = new DecimalFormat("#.###");

	/** Segment name. */
	private final String segmentName_;

	/** Segment and factor number. */
	private final int info_, segmentNum_, factorNum_;

	/** Input stress sequence. */
	private final StressSequence sequence_;

	/** Total stress. */
	private final double totalStress_;

	/** True if profile comparison. */
	private final boolean isComparison_;

	/**
	 * Creates get mission profile peak info task.
	 *
	 * @param peakInfo
	 *            The requested peak info.
	 * @param sequence
	 *            Stress sequence.
	 * @param segmentName
	 *            Segment name.
	 * @param segmentNum
	 *            Segment number.
	 * @param factorNum
	 *            Factor number.
	 * @param y
	 *            Peak y coordinate.
	 * @param isComparison
	 *            True if profile comparison.
	 */
	public GetMissionProfilePeakInfo(int peakInfo, StressSequence sequence, String segmentName, int segmentNum, int factorNum, double y, boolean isComparison) {
		info_ = peakInfo;
		sequence_ = sequence;
		segmentName_ = segmentName;
		segmentNum_ = segmentNum;
		factorNum_ = factorNum;
		totalStress_ = y;
		isComparison_ = isComparison;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Get mission profile peak info";
	}

	@Override
	protected String call() throws Exception {

		// update progress info
		updateTitle("Getting mission profile peak info...");

		// initialize info
		String info = null;

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// create statement
			try (Statement statement = connection.createStatement()) {

				// get segment ID
				int segmentID = getSegmentID(statement);

				// get flight ID and peak number
				int[] flightIDAndPeakNum = getFlightIDAndPeakNum(segmentID, statement, connection);

				// flight name
				if (info_ == FLIGHT_NAME) {
					info = getTypicalFlightName(flightIDAndPeakNum, statement);
				}
				else if ((info_ == ONE_G_STRESS) || (info_ == INCREMENT_STRESS) || (info_ == DELTA_P_STRESS) || (info_ == DELTA_T_STRESS)) {
					info = getStressInfo(flightIDAndPeakNum, statement);
				}
				else if ((info_ == CLASS_CODE) || (info_ == DELTA_P_PRESSURE) || (info_ == DELTA_T_TEMPERATURE)) {
					info = getANAInfo(flightIDAndPeakNum, statement);
				}
				else if ((info_ == ONE_G_FLIGHT_PHASE) || (info_ == ONE_G_ISSY_CODE) || (info_ == ONE_G_COMMENT)) {
					info = get1GInfo(flightIDAndPeakNum, statement);
				}
				else if ((info_ == INCREMENT_FLIGHT_PHASE) || (info_ == INCREMENT_ISSY_CODE) || (info_ == INCREMENT_FACTOR) || (info_ == INCREMENT_COMMENT) || (info_ == LINEARITY)) {
					info = getIncrementInfo(flightIDAndPeakNum, connection, statement);
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

			// single profile
			if (!isComparison_) {
				MissionProfileViewPanel panel = (MissionProfileViewPanel) taskPanel_.getOwner().getOwner().getViewPanel().getSubPanel(ViewPanel.MISSION_PROFILE_VIEW);
				panel.setPeakInfo(get());
			}

			// comparison
			else {
				MissionProfileComparisonViewPanel panel = (MissionProfileComparisonViewPanel) taskPanel_.getOwner().getOwner().getViewPanel().getSubPanel(ViewPanel.MISSION_PROFILE_COMPARISON_VIEW);
				panel.setPeakInfo(get(), sequence_);
			}
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	/**
	 * Returns typical flight name.
	 *
	 * @param flightIDAndPeakNum
	 *            An array containing flight ID and peak number.
	 * @param statement
	 *            Database statement.
	 * @return Typical flight name.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private String getTypicalFlightName(int[] flightIDAndPeakNum, Statement statement) throws Exception {
		String sql = "select name from sth_flights where ";
		sql += "flight_id = " + flightIDAndPeakNum[0] + " and file_id = " + sequence_.getID();
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next())
				return "Typical flight: " + resultSet.getString("name");
		}
		throw new Exception("Typical flight name cannot be obtained from database.");
	}

	/**
	 * Retrieves increment info from database.
	 *
	 * @param flightIDAndPeakNum
	 *            An array containing flight ID and peak number.
	 * @param connection
	 *            Database connection.
	 * @param statement
	 *            Database statement.
	 * @return Increment info.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private String getIncrementInfo(int[] flightIDAndPeakNum, Connection connection, Statement statement) throws Exception {

		// get file IDs
		Spectrum cdfSet = sequence_.getParentItem().getParentItem();
		int anaFileID = cdfSet.getANAFileID();
		int txtFileID = cdfSet.getTXTFileID();
		int convTableID = cdfSet.getConversionTableID();

		// get class code
		String classCode = null;
		int anaFlightID = getANAFlightID(anaFileID, flightIDAndPeakNum[0], statement);
		String sql = "select fourteen_digit_code";
		sql += " from ana_peaks_" + anaFileID;
		sql += " where flight_id = " + anaFlightID;
		sql += " and peak_num = " + flightIDAndPeakNum[1];
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
	 * Retrieves 1g info from database.
	 *
	 * @param flightIDAndPeakNum
	 *            An array containing flight ID and peak number.
	 * @param statement
	 *            Database statement.
	 * @return 1G info.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private String get1GInfo(int[] flightIDAndPeakNum, Statement statement) throws Exception {

		// get file IDs
		Spectrum cdfSet = sequence_.getParentItem().getParentItem();
		int anaFileID = cdfSet.getANAFileID();
		int txtFileID = cdfSet.getTXTFileID();
		int convTableID = cdfSet.getConversionTableID();

		// get class code
		String classCode = null;
		int anaFlightID = getANAFlightID(anaFileID, flightIDAndPeakNum[0], statement);
		String sql = "select fourteen_digit_code";
		sql += " from ana_peaks_" + anaFileID;
		sql += " where flight_id = " + anaFlightID;
		sql += " and peak_num = " + flightIDAndPeakNum[1];
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
	 * Gets ANA info from database.
	 *
	 * @param flightIDAndPeakNum
	 *            An array containing flight ID and peak number.
	 * @param statement
	 *            Database statement.
	 * @return ANA info.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private String getANAInfo(int[] flightIDAndPeakNum, Statement statement) throws Exception {

		// get ANA file ID
		int anaFileID = sequence_.getParentItem().getParentItem().getANAFileID();
		int anaFlightID = getANAFlightID(anaFileID, flightIDAndPeakNum[0], statement);

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
		sql += " and peak_num = " + flightIDAndPeakNum[1];

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
	 * @param sthFlightID
	 *            STH flight ID.
	 * @param statement
	 *            Database statement.
	 * @return ANA flight ID.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private int getANAFlightID(int anaFileID, int sthFlightID, Statement statement) throws Exception {
		int sthFlightNumber = getSTHFlightNumber(sthFlightID, statement);
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
	 * @param sthFlightID
	 *            STH flight ID.
	 * @param statement
	 *            Database statement.
	 * @return STH flight number.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private int getSTHFlightNumber(int sthFlightID, Statement statement) throws Exception {
		String sql = "select flight_num from sth_flights where file_id = " + sequence_.getID() + " and flight_id = " + sthFlightID;
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next())
				return resultSet.getInt("flight_num");
		}
		throw new Exception("Spectrum flight number couldn't be obtained from database.");
	}

	/**
	 * Returns stress info.
	 *
	 * @param flightIDAndPeakNum
	 *            An array containing flight ID and peak number.
	 * @param statement
	 *            Database statement.
	 * @return Stress info.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private String getStressInfo(int[] flightIDAndPeakNum, Statement statement) throws Exception {

		// create query
		String sql = "select ";
		if (info_ == ONE_G_STRESS) {
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
		sql += " from sth_peaks_" + sequence_.getID();
		sql += " where flight_id = " + flightIDAndPeakNum[0];
		sql += " and peak_num = " + flightIDAndPeakNum[1];

		// get stress info
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				if (info_ == ONE_G_STRESS)
					return "1g stress: " + format_.format(resultSet.getDouble("oneg_stress"));
				else if (info_ == INCREMENT_STRESS)
					return "Increment stress: " + format_.format(resultSet.getDouble("inc_stress"));
				else if (info_ == DELTA_P_STRESS)
					return "Delta-p stress: " + format_.format(resultSet.getDouble("dp_stress"));
				else if (info_ == DELTA_T_STRESS)
					return "Delta-t stress: " + format_.format(resultSet.getDouble("dt_stress"));
			}
		}
		throw new Exception("Stress info couldn't be obtained from database.");
	}

	/**
	 * Returns an array containing flight ID and peak number.
	 *
	 * @param segmentID
	 *            Segment ID.
	 * @param statement
	 *            Database statement.
	 * @param connection
	 *            Database connection.
	 * @return An array containing flight ID and peak number.
	 * @throws Exception
	 *             If flight ID and peak number cannot be obtained.
	 */
	private int[] getFlightIDAndPeakNum(int segmentID, Statement statement, Connection connection) throws Exception {

		// prepare statement to get total stress from STH peaks table
		String sql = "select flight_id, peak_num, peak_val from sth_peaks_" + sequence_.getID();
		sql += " where flight_id = ? and peak_num = ?";
		try (PreparedStatement getStress = connection.prepareStatement(sql)) {

			// execute query to get flight ID and peak number from segment increment stresses table
			sql = "select flight_id, peak_num from segment_increment_stresses_" + sequence_.getID();
			sql += " where segment_id = " + segmentID + " and factor_num = " + factorNum_;
			try (ResultSet resultSet = statement.executeQuery(sql)) {

				// loop over peaks
				while (resultSet.next()) {

					// get flight id and peak number
					int flightID = resultSet.getInt("flight_id");
					int peakNum = resultSet.getInt("peak_num");

					// get total stress
					getStress.setInt(1, flightID);
					getStress.setInt(2, peakNum);
					try (ResultSet resultSet2 = getStress.executeQuery()) {

						// loop over peaks
						while (resultSet2.next()) {

							// get total stress
							double totalStress = resultSet2.getDouble("peak_val");

							// peak found (total stress within tolerance)
							if ((totalStress_ <= (totalStress + TOLERANCE)) && (totalStress_ >= (totalStress - TOLERANCE)))
								return new int[] { flightID, peakNum };
						}
					}
				}
			}
		}

		// no flight ID or peak number found
		throw new Exception("Cannot find typical flight ID and peak number.");
	}

	/**
	 * Returns segment ID.
	 *
	 * @param statement
	 *            Database statement.
	 * @return Segment ID.
	 * @throws Exception
	 *             If segment ID cannot be obtained.
	 */
	private int getSegmentID(Statement statement) throws Exception {

		// get segment ID
		String sql = "select segment_id from segments_" + sequence_.getID();
		sql += " where segment_name = '" + segmentName_ + "' and segment_num = " + segmentNum_;
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next())
				return resultSet.getInt("segment_id");
		}

		// no segment ID found
		throw new Exception("Cannot find flight segment info for segment '" + segmentName_ + "'.");
	}
}
