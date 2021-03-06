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

import java.io.BufferedWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.HashMap;

import equinox.data.DPRatio;
import equinox.data.DT1PointInterpolator;
import equinox.data.DT2PointsInterpolator;
import equinox.data.DTInterpolation;
import equinox.data.DTInterpolator;
import equinox.data.LoadcaseFactor;
import equinox.data.OnegStress;
import equinox.data.Segment;
import equinox.data.SegmentFactor;
import equinox.data.StressComponent;
import equinox.data.fileType.STFFile;
import equinox.data.fileType.Spectrum;
import equinox.data.input.FastEquivalentStressInput;
import equinox.data.input.GenerateStressSequenceInput;
import equinox.task.FastGenerateStressSequence;
import equinox.utility.Utility;

/**
 * Class for generate fast SIGMA file process. The generated SIGMA file is to be used for ISAMI equivalent stress analysis.
 *
 * @author Murat Artim
 * @date 13 May 2017
 * @time 16:16:16
 *
 */
public class FastGenerateSigma implements EquinoxProcess<Path> {

	/** The owner task of this process. */
	private final FastGenerateStressSequence task_;

	/** The owner STF file. */
	private final STFFile stfFile_;

	/** STF file ID. */
	private final Integer stfID_, stressTableID_;

	/** The owner spectrum. */
	private final Spectrum spectrum_;

	/** Input. */
	private final FastEquivalentStressInput input_;

	/** Spectrum validity. */
	private final int validity_;

	/** Number of flights and flight types. */
	private int rowIndex_ = 0, colIndex_ = 0;

	/** Sigma file line. */
	private String line_;

	/** Number of columns. */
	private static final int NUM_COLS = 10;

	/** Decimal format. */
	private final DecimalFormat format_ = new DecimalFormat("0.000000E00");

	/**
	 * Creates generate fast SIGMA file process. The generated SIGMA file is to be used for ISAMI equivalent stress analysis.
	 *
	 * @param task
	 *            The owner task of this process.
	 * @param input
	 *            Stress sequence generation input.
	 * @param stfFile
	 *            STF file. Can be null if STF file ID, stress table ID and spectrum parameters are supplied.
	 * @param stfID
	 *            STF file ID. Can be null if STF is given.
	 * @param stressTableID
	 *            Stress table ID. Can be null if STF is given.
	 * @param spectrum
	 *            Spectrum. Can be null if STF is given.
	 * @param validity
	 *            Spectrum validity (i.e. total number of flights).
	 *
	 */
	public FastGenerateSigma(FastGenerateStressSequence task, FastEquivalentStressInput input, STFFile stfFile, Integer stfID, Integer stressTableID, Spectrum spectrum, int validity) {
		task_ = task;
		input_ = input;
		stfFile_ = stfFile;
		stfID_ = stfID;
		stressTableID_ = stressTableID;
		spectrum_ = spectrum;
		validity_ = validity;
	}

	@Override
	public Path start(Connection connection, PreparedStatement... preparedStatements) throws Exception {

		// update info
		task_.updateMessage("Generating stress sequence...");

		// create path to output file
		Path sigmaFile = task_.getWorkingDirectory().resolve("input.sigma");

		// get spectrum file IDs
		Spectrum cdfSet = stfFile_ == null ? spectrum_ : stfFile_.getParentItem();
		int anaFileID = cdfSet.getANAFileID();
		int txtFileID = cdfSet.getTXTFileID();
		int flsFileID = cdfSet.getFLSFileID();
		int convTableID = cdfSet.getConversionTableID();
		int stfID = stfFile_ == null ? stfID_ : stfFile_.getID();
		int stressTableID = stfFile_ == null ? stressTableID_ : stfFile_.getStressTableID();

		// create file writer
		try (BufferedWriter writer = Files.newBufferedWriter(sigmaFile, Charset.defaultCharset())) {

			// create statement
			try (Statement statement = connection.createStatement()) {

				// write SIGMA file header
				writeHeader(statement, writer, anaFileID);

				// task cancelled
				if (task_.isCancelled())
					return null;

				// write flights sequence
				writeFlightSequence(connection, statement, writer, anaFileID, flsFileID);

				// task cancelled
				if (task_.isCancelled())
					return null;

				// get DP ratio
				task_.updateMessage("Computing delta-p ratio...");
				DPRatio dpRatio = getDPRatio(connection, statement, anaFileID, txtFileID, convTableID, stfID, stressTableID);

				// get DT parameters
				task_.updateMessage("Computing delta-t interpolation...");
				DTInterpolator dtInterpolator = getDTInterpolator(connection, statement, txtFileID, stfID, stressTableID);

				// get number of flights and peaks of the ANA file
				int numPeaks = getNumberOfPeaks(statement, anaFileID);

				// task cancelled
				if (task_.isCancelled())
					return null;

				// prepare statement for selecting ANA peaks
				String sql = "select peak_num, fourteen_digit_code, delta_p, delta_t from ana_peaks_" + anaFileID + " where flight_id = ?";
				try (PreparedStatement selectANAPeak = connection.prepareStatement(sql)) {

					// prepare statement for selecting 1g issy code
					sql = "select flight_phase, issy_code, oneg_order from txt_codes where file_id = " + txtFileID + " and one_g_code = ? and increment_num = 0";
					try (PreparedStatement select1GIssyCode = connection.prepareStatement(sql)) {

						// prepare statement for selecting increment issy code
						sql = "select flight_phase, issy_code, factor_1, factor_2, factor_3, factor_4, factor_5, factor_6, factor_7, factor_8 from txt_codes where file_id = " + txtFileID
								+ " and one_g_code = ? and increment_num = ? and direction_num = ? and (nl_factor_num is null or nl_factor_num = ?)";
						try (PreparedStatement selectIncrementIssyCode = connection.prepareStatement(sql)) {

							// prepare statement for selecting STF stress
							sql = "select stress_x, stress_y, stress_xy from stf_stresses_" + stressTableID + " where file_id = " + stfID + " and issy_code = ?";
							try (PreparedStatement selectSTFStress = connection.prepareStatement(sql)) {

								// execute query for selecting ANA flights
								sql = "select * from ana_flights where file_id = " + anaFileID + " order by flight_num";
								try (ResultSet anaFlights = statement.executeQuery(sql)) {

									// loop over flights
									HashMap<String, OnegStress> oneg = new HashMap<>();
									HashMap<String, Double> inc = new HashMap<>();
									int peakCount = 0;
									while (anaFlights.next()) {

										// task cancelled
										if (task_.isCancelled())
											return null;

										// write flight header
										int flightPeaks = anaFlights.getInt("num_peaks");
										writeFlightHeader(writer, anaFlights, flightPeaks);

										// initialize variables
										int rem = flightPeaks % NUM_COLS;
										int numRows = (flightPeaks / NUM_COLS) + (rem == 0 ? 0 : 1);
										rowIndex_ = 0;
										colIndex_ = 0;
										line_ = "";

										// execute statement for getting ANA peaks
										selectANAPeak.setInt(1, anaFlights.getInt("flight_id"));
										try (ResultSet anaPeaks = selectANAPeak.executeQuery()) {

											// loop over peaks
											while (anaPeaks.next()) {

												// task cancelled
												if (task_.isCancelled())
													return null;

												// update progress
												task_.updateProgress(peakCount, numPeaks);
												peakCount++;

												// insert peak into STH peaks table
												writeSTHPeak(writer, anaPeaks, select1GIssyCode, selectSTFStress, selectIncrementIssyCode, oneg, inc, dpRatio, dtInterpolator, rem, numRows);
											}
										}
									}
								}
							}
						}
					}
				}
			}

			// pass 1 line
			writer.write("\n");
		}

		// return SIGMA file
		return sigmaFile;
	}

	/**
	 * Writes STH peaks to output file.
	 *
	 * @param writer
	 *            File writer.
	 * @param anaPeaks
	 *            ANA peaks.
	 * @param select1GIssyCode
	 *            Database statement for selecting 1g issy codes.
	 * @param selectSTFStress
	 *            Database statement for selecting STF stresses.
	 * @param selectIncrementIssyCode
	 *            Database statement for selecting increment issy codes.
	 * @param oneg
	 *            Mapping containing 1g codes versus the stresses.
	 * @param inc
	 *            Mapping containing class codes versus the stresses.
	 * @param dpRatio
	 *            Delta-p ratio.
	 * @param dtInterpolator
	 *            Delta-t interpolator.
	 * @param numRows
	 *            Number of rows to write.
	 * @param rem
	 *            Remaining number of columns in the STH output file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeSTHPeak(BufferedWriter writer, ResultSet anaPeaks, PreparedStatement select1GIssyCode, PreparedStatement selectSTFStress, PreparedStatement selectIncrementIssyCode, HashMap<String, OnegStress> oneg, HashMap<String, Double> inc, DPRatio dpRatio, DTInterpolator dtInterpolator,
			int rem, int numRows) throws Exception {

		// get class code
		String classCode = anaPeaks.getString("fourteen_digit_code");
		String onegCode = classCode.substring(0, 4);

		// get 1g stress
		OnegStress onegStress = oneg.get(onegCode);
		if (onegStress == null) {
			onegStress = get1GStress(selectSTFStress, select1GIssyCode, onegCode);
			oneg.put(onegCode, onegStress);
		}

		// get segment
		Segment segment = onegStress.getSegment();

		// get increment stress
		Double incStress = inc.get(classCode);
		if (incStress == null) {
			incStress = getIncStress(selectSTFStress, selectIncrementIssyCode, classCode, onegCode, segment);
			inc.put(classCode, incStress);
		}

		// compute and modify delta-p stress
		double dpStress = dpRatio == null ? 0.0 : dpRatio.getStress(anaPeaks.getDouble("delta_p"));
		if (dpRatio != null) {
			dpStress = modifyStress(dpRatio.getIssyCode(), segment, GenerateStressSequenceInput.DELTAP, dpStress);
		}

		// compute and modify delta-t stress
		double dtStress = dtInterpolator == null ? 0.0 : dtInterpolator.getStress(anaPeaks.getDouble("delta_t"));
		if ((dtInterpolator != null) && (dtInterpolator instanceof DT1PointInterpolator)) {
			DT1PointInterpolator onePoint = (DT1PointInterpolator) dtInterpolator;
			dtStress = modifyStress(onePoint.getIssyCode(), segment, GenerateStressSequenceInput.DELTAT, dtStress);
		}
		else if ((dtInterpolator != null) && (dtInterpolator instanceof DT2PointsInterpolator)) {
			DT2PointsInterpolator twoPoints = (DT2PointsInterpolator) dtInterpolator;
			dtStress = modify2PointDTStress(twoPoints, segment, dtStress);
		}

		// compute and modify total stress
		double totalStress = onegStress.getStress() + incStress + dpStress + dtStress;

		// remove negative stresses
		if ((totalStress < 0.0) && input_.isRemoveNegativeStresses()) {
			totalStress = 0.0;
		}

		// last row
		if (rowIndex_ == (numRows - 1)) {

			// add peaks
			line_ += String.format("%14s", format_.format(totalStress));
			colIndex_++;

			// last column
			if (colIndex_ == (rem == 0 ? NUM_COLS : rem)) {
				writer.write(line_);
				writer.write("\n");
				line_ = "";
				colIndex_ = 0;
				rowIndex_++;
			}
		}

		// other rows
		else {

			// add peaks
			line_ += String.format("%14s", format_.format(totalStress));
			colIndex_++;

			// last column
			if (colIndex_ == NUM_COLS) {
				writer.write(line_);
				writer.write("\n");
				line_ = "";
				colIndex_ = 0;
				rowIndex_++;
			}
		}
	}

	/**
	 * Modifies and returns stress according to event, segment and stress type.
	 *
	 * @param interpolator
	 *            2 points delta-t interpolator.
	 * @param segment
	 *            Segment.
	 * @param stress
	 *            Stress value extracted from STF file.
	 * @return The modified stress value.
	 */
	private double modify2PointDTStress(DT2PointsInterpolator interpolator, Segment segment, double stress) {

		// apply overall factors
		String method = input_.getStressModificationMethod(GenerateStressSequenceInput.DELTAT);
		if (method.equals(GenerateStressSequenceInput.MULTIPLY)) {
			stress *= input_.getStressModificationValue(GenerateStressSequenceInput.DELTAT);
		}
		else if (method.equals(GenerateStressSequenceInput.ADD)) {
			stress += input_.getStressModificationValue(GenerateStressSequenceInput.DELTAT);
		}
		else if (method.equals(GenerateStressSequenceInput.SET)) {
			stress = input_.getStressModificationValue(GenerateStressSequenceInput.DELTAT);
		}

		// apply segment factors
		if ((segment != null) && (input_.getSegmentFactors() != null)) {
			for (SegmentFactor sFactor : input_.getSegmentFactors())
				if (sFactor.getSegment().equals(segment)) {
					method = sFactor.getModifierMethod(GenerateStressSequenceInput.DELTAT);
					if (method.equals(GenerateStressSequenceInput.MULTIPLY)) {
						stress *= sFactor.getModifierValue(GenerateStressSequenceInput.DELTAT);
					}
					else if (method.equals(GenerateStressSequenceInput.ADD)) {
						stress += sFactor.getModifierValue(GenerateStressSequenceInput.DELTAT);
					}
					else if (method.equals(GenerateStressSequenceInput.SET)) {
						stress = sFactor.getModifierValue(GenerateStressSequenceInput.DELTAT);
					}
					break;
				}
		}

		// apply loadcase factors
		if (input_.getLoadcaseFactors() != null) {
			for (LoadcaseFactor eFactor : input_.getLoadcaseFactors())
				if (eFactor.getLoadcaseNumber().equals(interpolator.getIssyCodeSup()) || eFactor.getLoadcaseNumber().equals(interpolator.getIssyCodeInf())) {
					method = eFactor.getModifierMethod();
					if (method.equals(GenerateStressSequenceInput.MULTIPLY)) {
						stress *= eFactor.getModifierValue();
					}
					else if (method.equals(GenerateStressSequenceInput.ADD)) {
						stress += eFactor.getModifierValue();
					}
					else if (method.equals(GenerateStressSequenceInput.SET)) {
						stress = eFactor.getModifierValue();
					}
					break;
				}
		}

		// return modified stress
		return stress;
	}

	/**
	 * Sets increment stress to given ANA peak.
	 *
	 * @param selectSTFStress
	 *            Database statement for selecting stress from STF file.
	 * @param selectIncrementIssyCode
	 *            Database statement for selecting increment issy code.
	 * @param classCode
	 *            14 digit class code.
	 * @param onegCode
	 *            1g code.
	 * @param segment
	 *            Segment.
	 * @return Returns the increment stress.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Double getIncStress(PreparedStatement selectSTFStress, PreparedStatement selectIncrementIssyCode, String classCode, String onegCode, Segment segment) throws Exception {

		// add default increment stress
		double totalIncrementStress = 0.0;

		// loop over increments
		for (int i = 0; i < 5; i++) {

			// get increment block
			String block = classCode.substring((2 * i) + 4, (2 * i) + 6);

			// no increment
			if (block.equals("00")) {
				continue;
			}

			// set parameters
			selectIncrementIssyCode.setString(1, onegCode); // 1g code
			selectIncrementIssyCode.setInt(2, i + 1); // increment number
			selectIncrementIssyCode.setString(3, block.substring(1)); // direction number
			selectIncrementIssyCode.setString(4, block.substring(0, 1)); // factor number

			// query issy code, factor and event name
			try (ResultSet resultSet = selectIncrementIssyCode.executeQuery()) {

				// loop over increments
				while (resultSet.next()) {

					// get issy code, factor and event name
					String issyCode = resultSet.getString("issy_code");
					double factor = resultSet.getDouble("factor_" + block.substring(0, 1));

					// compute and modify increment stress
					double stress = factor * getSTFStress(selectSTFStress, issyCode);
					stress = modifyStress(issyCode, segment, GenerateStressSequenceInput.INCREMENT, stress);

					// add to total increment stress
					totalIncrementStress += stress;
				}
			}
		}

		// set increment stresses
		return totalIncrementStress;
	}

	/**
	 * Sets 1g stress to given ANA peak.
	 *
	 * @param selectSTFStress
	 *            Database statement for selecting stresses from STF file.
	 * @param select1gIssyCode
	 *            Database statement for selecting 1g issy code from TXT file.
	 * @param onegCode
	 *            1g code.
	 * @return The 1g stress.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private OnegStress get1GStress(PreparedStatement selectSTFStress, PreparedStatement select1gIssyCode, String onegCode) throws Exception {

		// get 1G issy code and event name
		String issyCode = null, event = null, segmentName = null;
		int segmentNum = -1;
		select1gIssyCode.setString(1, onegCode); // 1g code
		try (ResultSet resultSet = select1gIssyCode.executeQuery()) {
			while (resultSet.next()) {

				// get issy code and event name
				issyCode = resultSet.getString("issy_code");
				event = resultSet.getString("flight_phase");
				segmentNum = resultSet.getInt("oneg_order");

				// extract segment name
				segmentName = Utility.extractSegmentName(event);
			}
		}

		// create segment
		Segment segment = new Segment(segmentName, segmentNum);

		// compute and modify 1g stress
		double stress = getSTFStress(selectSTFStress, issyCode);
		stress = modifyStress(issyCode, segment, GenerateStressSequenceInput.ONEG, stress);

		// set to peak
		return new OnegStress(segment, stress);
	}

	/**
	 * Returns STF stress for given issy code.
	 *
	 * @param selectSTFStress
	 *            Database statement.
	 * @param issyCode
	 *            ISSY code.
	 * @return STF stress.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private double getSTFStress(PreparedStatement selectSTFStress, String issyCode) throws Exception {
		StressComponent component = input_.getStressComponent();
		double angle = input_.getRotationAngle();
		selectSTFStress.setString(1, issyCode); // issy code
		try (ResultSet resultSet = selectSTFStress.executeQuery()) {
			while (resultSet.next())
				if (component.equals(StressComponent.NORMAL_X))
					return resultSet.getDouble("stress_x");
				else if (component.equals(StressComponent.NORMAL_Y))
					return resultSet.getDouble("stress_y");
				else if (component.equals(StressComponent.SHEAR_XY))
					return resultSet.getDouble("stress_xy");
				else if (component.equals(StressComponent.ROTATED)) {
					double x = resultSet.getDouble("stress_x");
					double y = resultSet.getDouble("stress_y");
					double xy = resultSet.getDouble("stress_xy");
					return (0.5 * (x + y)) + (0.5 * (x - y) * Math.cos(2 * angle)) + (xy * Math.sin(2 * angle));
				}
		}
		return 0.0;
	}

	/**
	 * Modifies and returns stress according to event, segment and stress type.
	 *
	 * @param issyCode
	 *            ISSY code.
	 * @param segment
	 *            Segment.
	 * @param stressType
	 *            Stress type (1g, increment, delta-p, delta-t or total stress).
	 * @param stress
	 *            Stress value extracted from STF file.
	 * @return The modified stress value.
	 */
	private double modifyStress(String issyCode, Segment segment, int stressType, double stress) {

		// apply overall factors
		String method = input_.getStressModificationMethod(stressType);
		if (method.equals(GenerateStressSequenceInput.MULTIPLY)) {
			stress *= input_.getStressModificationValue(stressType);
		}
		else if (method.equals(GenerateStressSequenceInput.ADD)) {
			stress += input_.getStressModificationValue(stressType);
		}
		else if (method.equals(GenerateStressSequenceInput.SET)) {
			stress = input_.getStressModificationValue(stressType);
		}

		// apply segment factors
		if ((segment != null) && (input_.getSegmentFactors() != null)) {
			for (SegmentFactor sFactor : input_.getSegmentFactors())
				if (sFactor.getSegment().equals(segment)) {
					method = sFactor.getModifierMethod(stressType);
					if (method.equals(GenerateStressSequenceInput.MULTIPLY)) {
						stress *= sFactor.getModifierValue(stressType);
					}
					else if (method.equals(GenerateStressSequenceInput.ADD)) {
						stress += sFactor.getModifierValue(stressType);
					}
					else if (method.equals(GenerateStressSequenceInput.SET)) {
						stress = sFactor.getModifierValue(stressType);
					}
					break;
				}
		}

		// apply loadcase factors
		if (input_.getLoadcaseFactors() != null) {
			for (LoadcaseFactor eFactor : input_.getLoadcaseFactors())
				if (eFactor.getLoadcaseNumber().equals(issyCode)) {
					method = eFactor.getModifierMethod();
					if (method.equals(GenerateStressSequenceInput.MULTIPLY)) {
						stress *= eFactor.getModifierValue();
					}
					else if (method.equals(GenerateStressSequenceInput.ADD)) {
						stress += eFactor.getModifierValue();
					}
					else if (method.equals(GenerateStressSequenceInput.SET)) {
						stress = eFactor.getModifierValue();
					}
					break;
				}
		}

		// return modified stress
		return stress;
	}

	/**
	 * Writes out flight header to output STH file.
	 *
	 * @param writer
	 *            File writer.
	 * @param anaFlights
	 *            ANA flights.
	 * @param flightPeaks
	 *            Number of peaks of the flight.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeFlightHeader(BufferedWriter writer, ResultSet anaFlights, int flightPeaks) throws Exception {

		// update info
		String name = anaFlights.getString("name");
		task_.updateMessage("Generating flight '" + name + "'...");

		// pass 1 line
		writer.write("\n");

		// write first line of flight info
		int flightNum = anaFlights.getInt("flight_num") + 1;
		String line = "NUVOL ";
		line += String.format("%6s", flightNum);
		line += " ! FLIGHT ";
		line += name;
		writer.write(line);
		writer.write("\n");

		// write second line of flight info
		line = "TITLE FLIGHT NB ";
		line += String.format("%6s", flightNum);
		line += " ! FLIGHT ";
		line += name;
		writer.write(line);
		writer.write("\n");

		// write third line of flight info
		line = "NBOCCU ";
		line += String.format("%4s", (int) anaFlights.getDouble("validity"));
		writer.write(line);
		writer.write("\n");

		// write fourth line of flight info
		line = "NBVAL ";
		line += String.format("%6s", flightPeaks);
		writer.write(line);
		writer.write("\n");
	}

	/**
	 * Returns the number of peaks of the ANA file.
	 *
	 * @param statement
	 *            Database statement.
	 * @param anaFileID
	 *            ANA file ID.
	 * @return Number of peaks of the ANA file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static int getNumberOfPeaks(Statement statement, int anaFileID) throws Exception {
		String sql = "select sum(num_peaks) as totalPeaks from ana_flights where file_id = " + anaFileID;
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next())
				return resultSet.getInt("totalPeaks");
		}
		return 0;
	}

	/**
	 * Returns delta-t interpolation, or null if no delta-t interpolation is supplied.
	 *
	 * @param connection
	 *            Database connection.
	 * @param statement
	 *            Database statement.
	 * @param txtFileID
	 *            TXT file ID.
	 * @param stfID
	 *            STF file ID.
	 * @param stressTableID
	 *            STF stress table ID.
	 * @return Delta-t interpolation, or null if no delta-t interpolation is supplied.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private DTInterpolator getDTInterpolator(Connection connection, Statement statement, int txtFileID, int stfID, int stressTableID) throws Exception {

		// no delta-t interpolation
		DTInterpolation interpolation = input_.getDTInterpolation();
		if (interpolation.equals(DTInterpolation.NONE))
			return null;

		// get reference temperatures
		double[] refTemp = new double[2];
		refTemp[0] = input_.getReferenceDTSup() == null ? 0.0 : input_.getReferenceDTSup().doubleValue();
		refTemp[1] = input_.getReferenceDTInf() == null ? 0.0 : input_.getReferenceDTInf().doubleValue();

		// set variables
		DTInterpolator dtInterpolator = null;
		StressComponent component = input_.getStressComponent();
		double angle = input_.getRotationAngle();

		// get delta-p issy code from TXT file
		boolean supLCFound = false, infLCFound = false;
		String sql = null;
		if (interpolation.equals(DTInterpolation.ONE_POINT)) {
			sql = "select flight_phase, issy_code from txt_codes where file_id = " + txtFileID + " and issy_code = '" + input_.getDTLoadcaseSup() + "'";
		}
		else if (interpolation.equals(DTInterpolation.TWO_POINTS)) {
			sql = "select flight_phase, issy_code from txt_codes where file_id = " + txtFileID + " and (issy_code = '" + input_.getDTLoadcaseSup() + "' or issy_code = '" + input_.getDTLoadcaseInf() + "')";
		}
		try (ResultSet resultSet = statement.executeQuery(sql)) {

			// prepare statement to get STF stresses
			sql = "select stress_x, stress_y, stress_xy from stf_stresses_" + stressTableID + " where file_id = " + stfID + " and issy_code = ?";
			try (PreparedStatement statement2 = connection.prepareStatement(sql)) {

				// loop over delta-t cases
				while (resultSet.next()) {

					// set issy code
					String issyCode = resultSet.getString("issy_code");
					statement2.setString(1, issyCode);

					// get delta-p stress from STF file
					double stress = 0.0;
					try (ResultSet resultSet2 = statement2.executeQuery()) {
						while (resultSet2.next())
							if (component.equals(StressComponent.NORMAL_X)) {
								stress = resultSet2.getDouble("stress_x");
							}
							else if (component.equals(StressComponent.NORMAL_Y)) {
								stress = resultSet2.getDouble("stress_y");
							}
							else if (component.equals(StressComponent.SHEAR_XY)) {
								stress = resultSet2.getDouble("stress_xy");
							}
							else if (component.equals(StressComponent.ROTATED)) {
								double x = resultSet2.getDouble("stress_x");
								double y = resultSet2.getDouble("stress_y");
								double xy = resultSet2.getDouble("stress_xy");
								stress = (0.5 * (x + y)) + (0.5 * (x - y) * Math.cos(2 * angle)) + (xy * Math.sin(2 * angle));
							}
					}

					// 1 point interpolation
					if (interpolation.equals(DTInterpolation.ONE_POINT)) {
						dtInterpolator = new DT1PointInterpolator(resultSet.getString("flight_phase"), issyCode, stress, refTemp[0]);
						supLCFound = true;
						break;
					}

					// 2 points interpolation
					else if (interpolation.equals(DTInterpolation.TWO_POINTS)) {

						// create interpolator
						if (dtInterpolator == null) {
							dtInterpolator = new DT2PointsInterpolator();
						}

						// superior load case
						if (issyCode.equals(input_.getDTLoadcaseSup())) {
							((DT2PointsInterpolator) dtInterpolator).setSupParameters(resultSet.getString("flight_phase"), issyCode, stress, refTemp[0]);
							supLCFound = true;
						}

						// inferior load case
						else if (issyCode.equals(input_.getDTLoadcaseInf())) {
							((DT2PointsInterpolator) dtInterpolator).setInfParameters(resultSet.getString("flight_phase"), issyCode, stress, refTemp[1]);
							infLCFound = true;
						}
					}
				}
			}
		}

		// delta-t load case could not be found
		if (interpolation.equals(DTInterpolation.ONE_POINT) && !supLCFound) {
			task_.addWarning("Delta-T superior load case '" + input_.getDTLoadcaseSup() + "' could not be found.");
		}
		else if (interpolation.equals(DTInterpolation.TWO_POINTS)) {
			if (!supLCFound) {
				task_.addWarning("Delta-T superior load case '" + input_.getDTLoadcaseSup() + "' could not be found.");
			}
			if (!infLCFound) {
				task_.addWarning("Delta-T inferior load case '" + input_.getDTLoadcaseInf() + "' could not be found.");
			}
		}

		// return interpolator
		return dtInterpolator;
	}

	/**
	 * Returns delta-p ratio.
	 *
	 * @param connection
	 *            Database connection.
	 * @param statement
	 *            Database statement.
	 * @param anaFileID
	 *            ANA file ID.
	 * @param txtFileID
	 *            TXT file ID.
	 * @param convTableID
	 *            Conversion table ID.
	 * @param stfID
	 *            STF file ID.
	 * @param stressTableID
	 *            STF stress table ID.
	 * @return Delta-p ratio.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private DPRatio getDPRatio(Connection connection, Statement statement, int anaFileID, int txtFileID, int convTableID, int stfID, int stressTableID) throws Exception {

		// get reference pressure
		double refDP = getRefDP(connection, convTableID, anaFileID);

		// set variables
		DPRatio dpRatio = null;
		StressComponent component = input_.getStressComponent();
		double angle = input_.getRotationAngle();

		// create statement to get delta-p event name and issy code
		String sql = null;
		if (input_.getDPLoadcase() == null) {
			sql = "select flight_phase, issy_code from txt_codes where file_id = " + txtFileID + " and dp_case = 1";
		}
		else {
			sql = "select flight_phase from txt_codes where file_id = " + txtFileID + " and issy_code = '" + input_.getDPLoadcase() + "'";
		}

		// execute statement
		try (ResultSet resultSet = statement.executeQuery(sql)) {

			// prepare statement to get STF stresses
			sql = "select stress_x, stress_y, stress_xy from stf_stresses_" + stressTableID + " where file_id = " + stfID + " and issy_code = ?";
			try (PreparedStatement statement2 = connection.prepareStatement(sql)) {

				// loop over delta-p cases
				while (resultSet.next()) {

					// set issy code
					String issyCode = input_.getDPLoadcase() == null ? resultSet.getString("issy_code") : input_.getDPLoadcase();
					statement2.setString(1, issyCode);

					// get delta-p stress from STF file
					double stress = 0.0;
					try (ResultSet resultSet2 = statement2.executeQuery()) {
						while (resultSet2.next())
							if (component.equals(StressComponent.NORMAL_X)) {
								stress = resultSet2.getDouble("stress_x");
							}
							else if (component.equals(StressComponent.NORMAL_Y)) {
								stress = resultSet2.getDouble("stress_y");
							}
							else if (component.equals(StressComponent.SHEAR_XY)) {
								stress = resultSet2.getDouble("stress_xy");
							}
							else if (component.equals(StressComponent.ROTATED)) {
								double x = resultSet2.getDouble("stress_x");
								double y = resultSet2.getDouble("stress_y");
								double xy = resultSet2.getDouble("stress_xy");
								stress = (0.5 * (x + y)) + (0.5 * (x - y) * Math.cos(2 * angle)) + (xy * Math.sin(2 * angle));
							}
					}

					// create delta-p ratio
					dpRatio = new DPRatio(refDP, stress, resultSet.getString("flight_phase"), issyCode);
					break;
				}
			}
		}

		// delta-p load case could not be found
		if ((input_.getDPLoadcase() != null) && (dpRatio == null)) {
			task_.addWarning("Delta-P load case '" + input_.getDPLoadcase() + "' could not be found.");
		}

		// return delta-p ratio
		return dpRatio;
	}

	/**
	 * Returns reference delta-p pressure. The process is composed of the following logic;
	 * <UL>
	 * <LI>If the reference delta-p pressure is supplied by the user, this value is returned. Otherwise, the process falls back to next step.
	 * <LI>If the reference delta-p pressure is supplied within the conversion table, this value is returned. Otherwise, the process falls back to next step.
	 * <LI>Maximum pressure value within the ANA file is returned.
	 * </UL>
	 *
	 * @param connection
	 *            Database connection.
	 * @param convTableID
	 *            Conversion table ID.
	 * @param anaFileID
	 *            ANA file ID.
	 * @return Reference delta-p pressure.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private double getRefDP(Connection connection, int convTableID, int anaFileID) throws Exception {

		// initialize reference delta-p
		double refPressure = input_.getReferenceDP() == null ? 0.0 : input_.getReferenceDP().doubleValue();

		// no reference delta-p value given
		if (refPressure == 0.0) {
			// create statement
			try (Statement statement = connection.createStatement()) {

				// get reference pressure from conversion table
				String sql = "select ref_dp from xls_files where file_id = " + convTableID;
				try (ResultSet resultSet = statement.executeQuery(sql)) {
					while (resultSet.next()) {
						refPressure = resultSet.getDouble("ref_dp");
					}
				}

				// reference pressure is zero
				if (refPressure == 0.0) {

					// get maximum pressure from ANA file
					sql = "select max_dp from ana_flights where file_id = " + anaFileID + " order by max_dp desc";
					statement.setMaxRows(1);
					try (ResultSet resultSet = statement.executeQuery(sql)) {
						while (resultSet.next()) {
							refPressure = resultSet.getDouble("max_dp");
						}
					}
					statement.setMaxRows(0);
				}
			}
		}

		// return reference pressure
		return refPressure;
	}

	/**
	 * Writes out flight sequence.
	 *
	 * @param connection
	 *            Database connection.
	 * @param statement
	 *            Database statement.
	 * @param writer
	 *            File writer.
	 * @param anaFileID
	 *            ANA file ID.
	 * @param flsFileID
	 *            FLS file ID.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeFlightSequence(Connection connection, Statement statement, BufferedWriter writer, int anaFileID, int flsFileID) throws Exception {

		// update progress info
		task_.updateMessage("Writing flight sequence...");

		// write header
		writer.write("FLIGHTS SEQUENCE");
		writer.write("\n");

		// prepare statement for getting flight numbers
		String sql = "select flight_num from ana_flights where file_id = " + anaFileID + " and name = ?";
		try (PreparedStatement statement2 = connection.prepareStatement(sql)) {

			// get flight names
			sql = "select name from fls_flights where file_id = " + flsFileID + " order by flight_num asc";
			try (ResultSet resultSet = statement.executeQuery(sql)) {

				// loop over flights
				int flightCount = 0;
				while (resultSet.next()) {

					// task cancelled
					if (task_.isCancelled())
						return;

					// update progress
					task_.updateProgress(flightCount, validity_);
					flightCount++;

					// get name
					String name = resultSet.getString("name");
					statement2.setString(1, name);

					// get flight numbers
					try (ResultSet resultSet2 = statement2.executeQuery()) {

						// loop over flight numbers
						while (resultSet2.next()) {
							String line = String.format("%6s", resultSet2.getInt("flight_num") + 1);
							line += " ! FLIGHT ";
							line += name;
							writer.write(line);
							writer.write("\n");
						}
					}

				}
			}
		}
	}

	/**
	 * Writes out SIGMA file header.
	 *
	 * @param statement
	 *            Database statement.
	 * @param writer
	 *            File writer.
	 * @param anaFileID
	 *            ANA file ID.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeHeader(Statement statement, BufferedWriter writer, int anaFileID) throws Exception {

		// update progress info
		task_.updateMessage("Writing SIGMA file header...");

		// write total number of flights
		String line = "NBVOL ";
		line += String.format("%6s", validity_);
		line += " ! TOTAL NUMBER OF FLIGHTS";
		writer.write(line);
		writer.write("\n");

		// write total number of flight types
		String sql = "select num_flights from ana_files where file_id = " + anaFileID;
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				line = "NBTYPEVOL ";
				int numFlightTypes = resultSet.getInt("num_flights");
				line += String.format("%6s", numFlightTypes);
				line += " ! TOTAL NUMBER OF TYPE FLIGHTS";
				writer.write(line);
				writer.write("\n");
			}
		}

		// pass 1 line
		writer.write("\n");
	}
}
