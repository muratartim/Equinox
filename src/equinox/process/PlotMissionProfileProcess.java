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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.data.xy.YIntervalSeries;
import org.jfree.data.xy.YIntervalSeriesCollection;

import equinox.data.Segment;
import equinox.data.fileType.StressSequence;
import equinox.task.InternalEquinoxTask;

/**
 * Class for plot mission profile process.
 *
 * @author Murat Artim
 * @date Jun 12, 2016
 * @time 8:46:13 PM
 */
public class PlotMissionProfileProcess implements EquinoxProcess<XYDataset[]> {

	/** The owner task. */
	private final InternalEquinoxTask<?> task_;

	/** Input stress sequence. */
	private final StressSequence sequence_;

	/** List of segments. */
	private ArrayList<Segment> segments_;

	/** Maximum difference in range. */
	private double maxPos_ = 0.0, minNeg_ = 0.0;

	/** Step plotting options. */
	private final boolean[] plotStep_;

	/** Increment plotting options. */
	private final boolean plotPosInc_, plotNegInc_;

	/** Segment table names. */
	private final String segmentsTable_, segmentSteadyStressesTable_, segmentIncrementStressesTable_;

	/**
	 * Creates plot mission profile process.
	 *
	 * @param task
	 *            The owner task.
	 * @param sequence
	 *            Input stress sequence.
	 * @param plotPosInc
	 *            True to plot positive increments.
	 * @param plotNegInc
	 *            True to plot negative increments.
	 * @param plotStep
	 *            Step plotting options. Array size must be 8 (for 8 steps). Null can be given for plotting all steps.
	 */
	public PlotMissionProfileProcess(InternalEquinoxTask<?> task, StressSequence sequence, boolean plotPosInc, boolean plotNegInc, boolean[] plotStep) {
		task_ = task;
		sequence_ = sequence;
		plotPosInc_ = plotPosInc;
		plotNegInc_ = plotNegInc;
		if (plotStep == null)
			plotStep_ = new boolean[] { true, true, true, true, true, true, true, true };
		else
			plotStep_ = plotStep;
		segmentsTable_ = null;
		segmentSteadyStressesTable_ = null;
		segmentIncrementStressesTable_ = null;
	}

	/**
	 * Creates plot mission profile process.
	 *
	 * @param task
	 *            The owner task.
	 * @param segmentsTable
	 *            Segments table name.
	 * @param segmentSteadyStressesTable
	 *            Segment steady stresses table name.
	 * @param segmentIncrementStressesTable
	 *            Segment increment stresses table name.
	 * @param plotPosInc
	 *            True to plot positive increments.
	 * @param plotNegInc
	 *            True to plot negative increments.
	 * @param plotStep
	 *            Step plotting options. Array size must be 8 (for 8 steps). Null can be given for plotting all steps.
	 */
	public PlotMissionProfileProcess(InternalEquinoxTask<?> task, String segmentsTable, String segmentSteadyStressesTable, String segmentIncrementStressesTable, boolean plotPosInc, boolean plotNegInc, boolean[] plotStep) {
		task_ = task;
		segmentsTable_ = segmentsTable;
		segmentSteadyStressesTable_ = segmentSteadyStressesTable;
		segmentIncrementStressesTable_ = segmentIncrementStressesTable;
		plotPosInc_ = plotPosInc;
		plotNegInc_ = plotNegInc;
		if (plotStep == null)
			plotStep_ = new boolean[] { true, true, true, true, true, true, true, true };
		else
			plotStep_ = plotStep;
		sequence_ = null;
	}

	/**
	 * Returns flight segments.
	 *
	 * @return Flight segments.
	 */
	public ArrayList<Segment> getSegments() {
		return segments_;
	}

	/**
	 * Returns the maximum positive increment value of the plot.
	 *
	 * @return The maximum positive increment value of the plot.
	 */
	public double getMaxPositiveIncrement() {
		return maxPos_;
	}

	/**
	 * Returns the minimum negative increment value of the plot.
	 *
	 * @return The minimum negative increment value of the plot.
	 */
	public double getMinNegativeIncrement() {
		return minNeg_;
	}

	@Override
	public XYDataset[] start(Connection connection, PreparedStatement... preparedStatements) throws Exception {

		// update progress info
		task_.updateMessage("Plotting mission profile...");

		// create dataset array
		XYDataset[] datasets = new XYDataset[3];
		segments_ = new ArrayList<>();

		// create increment dataset
		YIntervalSeriesCollection incrementDataset = new YIntervalSeriesCollection();
		datasets[0] = incrementDataset;
		YIntervalSeries positiveInc = null, negativeInc = null;
		if (plotPosInc_) {
			positiveInc = new YIntervalSeries("Positive Increments");
			incrementDataset.addSeries(positiveInc);
		}
		if (plotNegInc_) {
			negativeInc = new YIntervalSeries("Negative Increments");
			incrementDataset.addSeries(negativeInc);
		}

		// create steady dataset
		XYSeriesCollection steadyDataset = new XYSeriesCollection();
		datasets[1] = steadyDataset;
		XYSeries steady = new XYSeries("Steady");
		XYSeries oneg = new XYSeries("1G");
		XYSeries dp = new XYSeries("Delta-P");
		XYSeries dt = new XYSeries("Delta-T");
		steadyDataset.addSeries(steady);
		steadyDataset.addSeries(oneg);
		steadyDataset.addSeries(dp);
		steadyDataset.addSeries(dt);

		// create increment points dataset
		XYSeriesCollection incrementPointsDataset = new XYSeriesCollection();
		datasets[2] = incrementPointsDataset;
		XYSeries positiveIncPoints = null, negativeIncPoints = null;
		if (plotPosInc_) {
			positiveIncPoints = new XYSeries("");
			incrementPointsDataset.addSeries(positiveIncPoints);
		}
		if (plotNegInc_) {
			negativeIncPoints = new XYSeries(" ");
			incrementPointsDataset.addSeries(negativeIncPoints);
		}

		// create statement
		try (Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)) {

			// prepare statement to get steady stresses
			String sql = "select oneg_stress, dp_stress, dt_stress from ";
			sql += sequence_ == null ? segmentSteadyStressesTable_ : "segment_steady_stresses_" + sequence_.getID();
			sql += " where segment_id = ?";
			try (PreparedStatement getSteady = connection.prepareStatement(sql)) {

				// prepare statement to get positive increment stresses
				sql = "select stress, factor_num from ";
				sql += sequence_ == null ? segmentIncrementStressesTable_ : "segment_increment_stresses_" + sequence_.getID();
				sql += " where stress >= 0 and segment_id = ? order by factor_num";
				try (PreparedStatement getPosInc = connection.prepareStatement(sql)) {

					// prepare statement to get negative increment stresses
					sql = "select stress, factor_num from ";
					sql += sequence_ == null ? segmentIncrementStressesTable_ : "segment_increment_stresses_" + sequence_.getID();
					sql += " where stress < 0 and segment_id = ? order by factor_num";
					try (PreparedStatement getNegInc = connection.prepareStatement(sql)) {

						// create and execute statement to get segments
						sql = "select segment_id, segment_name, segment_num from ";
						sql += sequence_ == null ? segmentsTable_ : "segments_" + sequence_.getID();
						sql += " order by segment_num asc";
						try (ResultSet getSegment = statement.executeQuery(sql)) {

							// move to last row
							if (getSegment.last()) {

								// get number of segments
								int numSegments = getSegment.getRow();

								// move to beginning
								getSegment.beforeFirst();

								// loop over segments
								int segmentCount = 0;
								while (getSegment.next()) {

									// update progress
									task_.updateProgress(segmentCount, numSegments);
									segmentCount++;

									// get segment ID
									String segmentName = getSegment.getString("segment_name");
									int segmentNum = getSegment.getInt("segment_num");
									int segmentID = getSegment.getInt("segment_id");

									// create and add segment
									segments_.add(new Segment(segmentName, segmentNum));

									// get steady stress
									double onegStress = 0.0, dpStress = 0.0, dtStress = 0.0;
									getSteady.setInt(1, segmentID);
									try (ResultSet steadyStresses = getSteady.executeQuery()) {
										while (steadyStresses.next()) {
											onegStress = steadyStresses.getDouble("oneg_stress");
											dpStress = steadyStresses.getDouble("dp_stress");
											dtStress = steadyStresses.getDouble("dt_stress");
										}
									}

									// compute total steady stress
									double steadyStress = onegStress + dpStress + dtStress;

									// add steady stresses to series
									oneg.add(segmentNum, onegStress);
									oneg.add(segmentNum + 1.0, onegStress);
									dp.add(segmentNum, dpStress);
									dp.add(segmentNum + 1.0, dpStress);
									dt.add(segmentNum, dtStress);
									dt.add(segmentNum + 1.0, dtStress);
									steady.add(segmentNum, steadyStress);
									steady.add(segmentNum + 1.0, steadyStress);

									// update max/min stresses
									if (maxPos_ <= steadyStress)
										maxPos_ = steadyStress;
									if (minNeg_ >= steadyStress)
										minNeg_ = steadyStress;

									// plot positive increments
									if (plotPosInc_)
										plotPositiveIncrements(segmentID, segmentNum, steadyStress, getPosInc, positiveInc, positiveIncPoints);

									// plot negative increments
									if (plotNegInc_)
										plotNegativeIncrements(segmentID, segmentNum, steadyStress, getNegInc, negativeInc, negativeIncPoints);
								}
							}
						}
					}
				}
			}
		}

		// return datasets
		return datasets;
	}

	/**
	 * Plots positive increments.
	 *
	 * @param segmentID
	 *            Segment ID.
	 * @param segmentNum
	 *            Segment number.
	 * @param steadyStress
	 *            Steady stress.
	 * @param getPosInc
	 *            Database statement to get positive incremental stresses.
	 * @param positiveInc
	 *            Chart series to add the positive increments as intervals.
	 * @param positiveIncPoints
	 *            Chart series to add the positive increments as points.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void plotPositiveIncrements(int segmentID, int segmentNum, double steadyStress, PreparedStatement getPosInc, YIntervalSeries positiveInc, XYSeries positiveIncPoints) throws Exception {

		// get positive increment stresses
		getPosInc.setInt(1, segmentID);
		try (ResultSet increment = getPosInc.executeQuery()) {
			while (increment.next()) {

				// get factor number
				int facNum = increment.getInt("factor_num");

				// step not plotted
				if (!plotStep_[facNum - 1])
					continue;

				// get stress
				double stress = increment.getDouble("stress");

				// compute plot values
				double x = segmentNum + (2 * facNum - 1.0) / 16.0;
				double y = steadyStress + stress;
				double yAvg = 0.5 * (y + steadyStress);

				// add to series
				positiveInc.add(x, yAvg, steadyStress, y);
				positiveIncPoints.add(x, y);

				// update maximum positive stress
				if (maxPos_ <= y)
					maxPos_ = y;
			}
		}
	}

	/**
	 * Plots negative increments.
	 *
	 * @param segmentID
	 *            Segment ID.
	 * @param segmentNum
	 *            Segment number.
	 * @param steadyStress
	 *            Steady stress.
	 * @param getNegInc
	 *            Database statement to get negative incremental stresses.
	 * @param negativeInc
	 *            Chart series to add the negative increments as intervals.
	 * @param negativeIncPoints
	 *            Chart series to add the negative increments as points.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void plotNegativeIncrements(int segmentID, int segmentNum, double steadyStress, PreparedStatement getNegInc, YIntervalSeries negativeInc, XYSeries negativeIncPoints) throws Exception {

		// get negative increment stresses
		getNegInc.setInt(1, segmentID);
		try (ResultSet increment = getNegInc.executeQuery()) {
			while (increment.next()) {

				// get factor number
				int facNum = increment.getInt("factor_num");

				// step not plotted
				if (!plotStep_[facNum - 1])
					continue;

				// get stress
				double stress = increment.getDouble("stress");

				// compute plot values
				double x = segmentNum + (2 * facNum - 1.0) / 16.0;
				double y = steadyStress + stress;
				double yAvg = 0.5 * (y + steadyStress);

				// add to series
				negativeInc.add(x, yAvg, steadyStress, y);
				negativeIncPoints.add(x, y);

				// update minimum negative stress
				if (minNeg_ >= y)
					minNeg_ = y;
			}
		}
	}
}
