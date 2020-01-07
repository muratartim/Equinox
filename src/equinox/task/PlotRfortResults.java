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
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import org.jfree.data.xy.XYSeriesCollection;

import equinox.Equinox;
import equinox.controller.MissionParameterPlotViewPanel;
import equinox.controller.ViewPanel;
import equinox.data.fileType.Rfort;
import equinox.process.PlotRfortResultsProcess;
import equinox.task.InternalEquinoxTask.ShortRunningTask;

/**
 * Class for plot RFORT results task.
 *
 * @author Murat Artim
 * @date Mar 10, 2016
 * @time 4:13:58 PM
 */
public class PlotRfortResults extends InternalEquinoxTask<XYSeriesCollection> implements ShortRunningTask {

	/** RFORT file. */
	private final Rfort rfort_;

	/** Stress type. */
	private final String stressType_;

	/** Visible pilot points and omissions. */
	private final ArrayList<String> visiblePPs_, visibleOmissions_;

	/** True to plot absolute deviations. */
	private final boolean plotAbsoluteDeviations_;

	/**
	 * Creates plot RFORT results task.
	 *
	 * @param rfort
	 *            RFORT file.
	 * @param stressType
	 *            Stress type.
	 * @param visiblePPs
	 *            Visible pilot points. Null can be given for showing all.
	 * @param visibleOmissions
	 *            Visible omissions. Null can be given for showing all.
	 * @param plotAbsoluteDeviations
	 *            True to plot absolute deviations.
	 */
	public PlotRfortResults(Rfort rfort, String stressType, ArrayList<String> visiblePPs, ArrayList<String> visibleOmissions, boolean plotAbsoluteDeviations) {
		rfort_ = rfort;
		stressType_ = stressType;
		visiblePPs_ = visiblePPs;
		visibleOmissions_ = visibleOmissions;
		plotAbsoluteDeviations_ = plotAbsoluteDeviations;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Plot RFORT results";
	}

	@Override
	protected XYSeriesCollection call() throws Exception {

		// initialize dataset
		XYSeriesCollection dataset = null;

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {
			dataset = new PlotRfortResultsProcess(this, rfort_, stressType_, visiblePPs_, visibleOmissions_, plotAbsoluteDeviations_).start(connection);
		}

		// return data set
		return dataset;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set chart data
		try {

			// get mission parameters view panel
			MissionParameterPlotViewPanel panel = (MissionParameterPlotViewPanel) taskPanel_.getOwner().getOwner().getViewPanel().getSubPanel(ViewPanel.MISSION_PARAMETERS_VIEW);

			// set data
			String title = "RFORT " + stressType_ + " Results";
			String xAxisLabel = "Number of Peaks";
			String yAxisLabel = (plotAbsoluteDeviations_ ? "Absolute " : "") + stressType_ + " Equivalent Stress Deviations (%)";
			panel.plottingCompleted(get(), title, xAxisLabel, yAxisLabel, null, true, false, "RFORT Results View");

			// show mission parameters view panel
			taskPanel_.getOwner().getOwner().getViewPanel().showSubPanel(ViewPanel.MISSION_PARAMETERS_VIEW);
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}
}
