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
import java.util.concurrent.ExecutionException;

import org.jfree.data.category.CategoryDataset;

import equinox.Equinox;
import equinox.controller.StatisticsViewPanel;
import equinox.controller.ViewPanel;
import equinox.data.fileType.Rfort;
import equinox.process.PlotRfortPeaksProcess;
import equinox.task.InternalEquinoxTask.ShortRunningTask;

/**
 * Class for plot RFORT average number of peaks task.
 *
 * @author Murat Artim
 * @date Apr 6, 2016
 * @time 2:04:21 PM
 */
public class PlotRfortPeaks extends InternalEquinoxTask<CategoryDataset> implements ShortRunningTask {

	/** RFORT file. */
	private final Rfort rfort_;

	/**
	 * Creates plot RFORT average number of peaks task.
	 *
	 * @param rfort
	 *            RFORT file.
	 */
	public PlotRfortPeaks(Rfort rfort) {
		rfort_ = rfort;
	}

	@Override
	public String getTaskTitle() {
		return "Plot RFORT average number of peaks";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	protected CategoryDataset call() throws Exception {

		// update progress info
		updateTitle("Plotting RFORT average number of peaks...");

		// create data set
		CategoryDataset dataset = null;

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {
			dataset = new PlotRfortPeaksProcess(this, rfort_).start(connection);
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
			panel.setPlotData(dataset, "Average Number of Peaks", null, "Omissions", "Average Number of Peaks", false, true, false);

			// show column chart plot panel
			taskPanel_.getOwner().getOwner().getViewPanel().showSubPanel(ViewPanel.STATS_VIEW);
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}
}
