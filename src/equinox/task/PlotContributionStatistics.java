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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.jfree.data.category.CategoryDataset;

import equinox.controller.StatisticsViewPanel;
import equinox.controller.ViewPanel;
import equinox.dataServer.remote.data.ContributionType;
import equinox.dataServer.remote.message.DataMessage;
import equinox.dataServer.remote.message.DatabaseQueryFailed;
import equinox.dataServer.remote.message.DatabaseQueryPermissionDenied;
import equinox.dataServer.remote.message.GetAircraftSectionsForPilotPointsRequest;
import equinox.dataServer.remote.message.PlotContributionStatisticsRequest;
import equinox.dataServer.remote.message.PlotContributionStatisticsResponse;
import equinox.network.DataServerManager;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.utility.exception.PermissionDeniedException;
import equinox.utility.exception.ServerDatabaseQueryFailedException;

/**
 * Class for plot damage contribution statistics task.
 *
 * @author Murat Artim
 * @date 14 Aug 2017
 * @time 14:46:52
 *
 */
public class PlotContributionStatistics extends InternalEquinoxTask<CategoryDataset> implements ShortRunningTask, DatabaseQueryListenerTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Aircraft program, section and mission. */
	private final String program_, section_, mission_;

	/** Damage contribution type. */
	private final ContributionType contributionType_;

	/** Number of increments to plot. */
	private int limit_ = 3;

	/** Server query completion indicator. */
	private final AtomicBoolean isQueryCompleted;

	/** Server query message. */
	private final AtomicReference<DataMessage> serverMessageRef;

	/**
	 * Creates plot damage contribution statistics task.
	 *
	 * @param program
	 *            Aircraft program.
	 * @param section
	 *            Aircraft section.
	 * @param mission
	 *            Fatigue mission.
	 * @param contributionType
	 *            Damage contribution type.
	 */
	public PlotContributionStatistics(String program, String section, String mission, ContributionType contributionType) {
		program_ = program;
		section_ = section;
		mission_ = mission;
		contributionType_ = section_.equals(GetAircraftSectionsForPilotPointsRequest.ALL_SECTIONS) ? contributionType : ContributionType.INCREMENT;
		isQueryCompleted = new AtomicBoolean();
		serverMessageRef = new AtomicReference<>(null);
	}

	/**
	 * Sets maximum number of most damaging increments to show.
	 *
	 * @param limit
	 *            Maximum number of most damaging increments to show.
	 */
	public void setIncrementLimit(int limit) {
		limit_ = limit;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Plot contribution statistics";
	}

	@Override
	public void respondToDataMessage(DataMessage message) throws Exception {
		processServerDataMessage(message, this, serverMessageRef, isQueryCompleted);
	}

	@Override
	protected CategoryDataset call() throws Exception {

		// check permission
		checkPermission(Permission.PLOT_CONTRIBUTION_STATISTICS);

		// update progress info
		updateTitle("Plotting contribution statistics...");
		updateMessage("Please wait...");

		// initialize variables
		DataServerManager watcher = null;
		boolean removeListener = false;

		try {

			// create request message
			PlotContributionStatisticsRequest request = new PlotContributionStatisticsRequest();
			request.setListenerHashCode(hashCode());
			request.setContributionType(contributionType_);
			request.setLimit(limit_);
			request.setMission(mission_);
			request.setProgram(program_);
			request.setSection(section_);

			// disable task canceling
			taskPanel_.updateCancelState(false);

			// register to network watcher and send analysis request
			watcher = taskPanel_.getOwner().getOwner().getDataServerManager();
			watcher.addMessageListener(this);
			removeListener = true;
			watcher.sendMessage(request);

			// wait for query to complete
			waitForDataServer(this, isQueryCompleted);

			// remove from network watcher
			watcher.removeMessageListener(this);
			removeListener = false;

			// enable task canceling
			taskPanel_.updateCancelState(true);

			// task cancelled
			if (isCancelled())
				return null;

			// get query message
			DataMessage message = serverMessageRef.get();

			// permission denied
			if (message instanceof DatabaseQueryPermissionDenied)
				throw new PermissionDeniedException(((DatabaseQueryPermissionDenied) message).getPermission());

			// query failed
			else if (message instanceof DatabaseQueryFailed)
				throw new ServerDatabaseQueryFailedException((DatabaseQueryFailed) message);

			// query succeeded
			else if (message instanceof PlotContributionStatisticsResponse)
				return ((PlotContributionStatisticsResponse) message).getDataset();

			// invalid response
			throw new Exception("Invalid response received from server.");
		}

		// remove from network watcher
		finally {
			if (watcher != null && removeListener) {
				watcher.removeMessageListener(this);
			}
		}
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
			StatisticsViewPanel outputPanel = (StatisticsViewPanel) taskPanel_.getOwner().getOwner().getViewPanel().getSubPanel(ViewPanel.STATS_VIEW);

			// setup plot labels
			String title = "Damage Contributions";
			String subTitle = program_ + ", " + section_ + ", " + mission_ + ", " + contributionType_;
			String yAxisLabel = "Averaged Fatigue Equivalent Stress Contribution [%]";

			// all sections and incremental contributions
			if (section_.equals(GetAircraftSectionsForPilotPointsRequest.ALL_SECTIONS)) {
				String xAxisLabel = "Aircraft Section";
				if (contributionType_.equals(ContributionType.INCREMENT)) {
					outputPanel.setPlotData(dataset, title, subTitle, xAxisLabel, yAxisLabel, true, false, true);
				}
				else {
					outputPanel.setPlotData(dataset, title, subTitle, xAxisLabel, yAxisLabel, false, false, false);
				}
			}

			// others
			else {
				String xAxisLabel = "Incremental Loadcase";
				outputPanel.setPlotData(dataset, title, subTitle, xAxisLabel, yAxisLabel, false, false, false);
			}

			// show column chart plot panel
			taskPanel_.getOwner().getOwner().getViewPanel().showSubPanel(ViewPanel.STATS_VIEW);
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}
}
