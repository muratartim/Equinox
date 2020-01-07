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
import org.jfree.data.category.DefaultCategoryDataset;

import equinox.Equinox;
import equinox.controller.DamageContributionViewPanel;
import equinox.controller.ViewPanel;
import equinox.data.fileType.FlightDamageContributions;
import equinox.plugin.FileType;
import equinox.process.PlotFlightDamageContributionsProcess;
import equinox.task.InternalEquinoxTask.ShortRunningTask;

/**
 * Class for plot typical flight damage contributions task.
 *
 * @author Murat Artim
 * @date 18 Oct 2016
 * @time 14:45:55
 */
public class PlotFlightDamageContributions extends InternalEquinoxTask<CategoryDataset> implements ShortRunningTask {

	/** Damage contributions item. */
	private final FlightDamageContributions contributions_;

	/**
	 * Creates plot typical flight damage contributions task.
	 *
	 * @param contributions
	 *            Typical flight damage contributions to plot.
	 */
	public PlotFlightDamageContributions(FlightDamageContributions contributions) {
		contributions_ = contributions;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Plot typical flight damage contributions";
	}

	@Override
	protected CategoryDataset call() throws Exception {

		// update progress info
		updateTitle("Plotting typical flight damage contributions...");

		// initialize data set
		DefaultCategoryDataset dataset = null;

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {
			dataset = new PlotFlightDamageContributionsProcess(this, contributions_).start(connection);
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

			// get damage contribution view panel
			DamageContributionViewPanel panel = (DamageContributionViewPanel) taskPanel_.getOwner().getOwner().getViewPanel().getSubPanel(ViewPanel.DAMAGE_CONTRIBUTION_VIEW);

			// set data
			String stfFileName = FileType.getNameWithoutExtension(contributions_.getParentItem().getName());
			panel.setPlotData(get(), stfFileName, null);

			// show damage contributions view panel
			taskPanel_.getOwner().getOwner().getViewPanel().showSubPanel(ViewPanel.DAMAGE_CONTRIBUTION_VIEW);
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}
}
