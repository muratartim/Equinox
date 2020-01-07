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

import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

import equinox.Equinox;
import equinox.controller.StatisticsViewPanel;
import equinox.controller.ViewPanel;
import equinox.data.fileType.Rfort;
import equinox.process.PlotRfortEquivalentStressesProcess;
import equinox.task.InternalEquinoxTask.ShortRunningTask;

/**
 * Class for plot RFORT equivalent stresses task.
 *
 * @author Murat Artim
 * @date Apr 19, 2016
 * @time 9:25:46 AM
 */
public class PlotRfortEquivalentStresses extends InternalEquinoxTask<CategoryDataset> implements ShortRunningTask {

	/** RFORT file. */
	private final Rfort rfort_;

	/** Stress type. */
	private final String stressType_;

	/** Visible pilot points and omissions. */
	private final ArrayList<String> visiblePPs_, visibleOmissions_;

	/**
	 * Creates plot RFORT fatigue equivalent stress deviations task.
	 *
	 * @param rfort
	 *            RFORT file.
	 * @param stressType
	 *            Stress type.
	 * @param visiblePPs
	 *            Visible pilot points. Null can be given for showing all.
	 * @param visibleOmissions
	 *            Visible omissions. Null can be given for showing all.
	 */
	public PlotRfortEquivalentStresses(Rfort rfort, String stressType, ArrayList<String> visiblePPs, ArrayList<String> visibleOmissions) {
		rfort_ = rfort;
		stressType_ = stressType;
		visiblePPs_ = visiblePPs;
		visibleOmissions_ = visibleOmissions;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Plot RFORT equivalent stresses";
	}

	@Override
	protected CategoryDataset call() throws Exception {

		// update info
		updateMessage("Plotting RFORT equivalent stresses...");

		// initialize data set
		DefaultCategoryDataset dataset = null;

		// create connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {
			dataset = new PlotRfortEquivalentStressesProcess(this, rfort_, stressType_, visiblePPs_, visibleOmissions_).start(connection);
		}

		// return dataset
		return dataset;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set chart data
		try {

			// get dataset
			CategoryDataset dataset = get();

			// get column plot panel
			StatisticsViewPanel panel = (StatisticsViewPanel) taskPanel_.getOwner().getOwner().getViewPanel().getSubPanel(ViewPanel.STATS_VIEW);

			// set chart data to panel
			String title = stressType_ + " Equivalent Stresses";
			panel.setPlotData(dataset, title, null, "Pilot Points", title, true, true, false);

			// show column chart plot panel
			taskPanel_.getOwner().getOwner().getViewPanel().showSubPanel(ViewPanel.STATS_VIEW);
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}
}
