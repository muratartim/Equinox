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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import equinox.Equinox;
import equinox.data.EmbeddedTask;
import equinox.data.ExecutionMode;
import equinox.data.Triple;
import equinox.data.fileType.STFFileBucket;
import equinox.data.fileType.SpectrumItem;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.task.automation.AutomaticTaskOwner;
import equinox.task.automation.MultipleInputTask;
import equinox.utility.AlphanumComparator;

/**
 * Class for get typical flight contribution names task.
 *
 * @author Murat Artim
 * @date 27 Oct 2016
 * @time 13:59:40
 */
public class GetFlightContributionNames extends InternalEquinoxTask<Triple<List<SpectrumItem>, List<String>, List<String>>> implements ShortRunningTask, MultipleInputTask<SpectrumItem>, AutomaticTaskOwner<Triple<List<SpectrumItem>, List<String>, List<String>>> {

	/** Requesting panel. */
	private final FlightDamageContributionRequestingPanel panel_;

	/** Spectrum items. This can be either damage contributions or STF file buckets. */
	private List<SpectrumItem> items_;

	/** Input threshold. Once the threshold is reached, this task will be executed. */
	private volatile int inputThreshold_ = 0;

	/** Automatic tasks. */
	private HashMap<String, EmbeddedTask<Triple<List<SpectrumItem>, List<String>, List<String>>>> automaticTasks_ = null;

	/**
	 * Creates get contribution names task.
	 *
	 * @param panel
	 *            Requesting panel. This can be null for automatic execution.
	 * @param items
	 *            Spectrum items. This can be either flight damage contributions or STF file buckets. This can be null for automatic execution.
	 */
	public GetFlightContributionNames(FlightDamageContributionRequestingPanel panel, List<SpectrumItem> items) {
		panel_ = panel;
		items_ = items == null ? Collections.synchronizedList(new ArrayList<>()) : items;
	}

	@Override
	synchronized public void setInputThreshold(int inputThreshold) {
		inputThreshold_ = inputThreshold;
	}

	@Override
	synchronized public void addAutomaticInput(AutomaticTaskOwner<SpectrumItem> task, SpectrumItem input, ExecutionMode mode) {
		automaticInputAdded(task, input, mode, items_, inputThreshold_);
	}

	@Override
	synchronized public void inputFailed(AutomaticTaskOwner<SpectrumItem> task, ExecutionMode mode) {
		inputThreshold_ = automaticInputFailed(task, mode, items_, inputThreshold_);
	}

	@Override
	public void addAutomaticTask(String taskID, EmbeddedTask<Triple<List<SpectrumItem>, List<String>, List<String>>> task) {
		if (automaticTasks_ == null) {
			automaticTasks_ = new HashMap<>();
		}
		automaticTasks_.put(taskID, task);
	}

	@Override
	public HashMap<String, EmbeddedTask<Triple<List<SpectrumItem>, List<String>, List<String>>>> getAutomaticTasks() {
		return automaticTasks_;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Get flight damage contribution names";
	}

	@Override
	protected Triple<List<SpectrumItem>, List<String>, List<String>> call() throws Exception {

		// update progress info
		updateTitle("Retrieving flight damage contribution names...");

		// create output
		Triple<List<SpectrumItem>, List<String>, List<String>> output = new Triple<>();
		output.setElement1(items_);

		// STF file buckets
		if (items_.get(0) instanceof STFFileBucket) {
			output.setElement2(getFromBuckets(true));
			output.setElement3(getFromBuckets(false));
		}

		// contributions
		else {
			output.setElement2(getFromContributions(true));
			output.setElement3(getFromContributions(false));
		}

		// return output
		return output;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set file info
		try {

			// get output
			Triple<List<SpectrumItem>, List<String>, List<String>> output = get();

			// user initiated task
			if (automaticTasks_ == null) {
				panel_.setFlightContributions(output.getElement2(), output.getElement3());
			}

			// automatic task
			else {
				automaticTaskOwnerSucceeded(output, automaticTasks_, taskPanel_);
			}
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	@Override
	protected void failed() {

		// call ancestor
		super.failed();

		// manage automatic tasks
		automaticTaskOwnerFailed(automaticTasks_);
	}

	@Override
	protected void cancelled() {

		// call ancestor
		super.cancelled();

		// manage automatic tasks
		automaticTaskOwnerFailed(automaticTasks_);
	}

	/**
	 * Gets contribution names from input damage contributions.
	 *
	 * @param withOccurrences
	 *            True if contributions with occurrences shall be returned.
	 * @return Contribution names.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private List<String> getFromContributions(boolean withOccurrences) throws Exception {

		// create lists
		ArrayList<String> names = new ArrayList<>(), names1 = new ArrayList<>(), toBeRemoved = new ArrayList<>();

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// prepare statement
			String tableName = withOccurrences ? "flight_dam_contribution_with_occurrences" : "flight_dam_contribution_without_occurrences";
			String sql = "select flight_name from " + tableName + " where id = ? order by flight_name";
			try (PreparedStatement statement = connection.prepareStatement(sql)) {

				// get contribution names of first item
				statement.setInt(1, items_.get(0).getID());
				try (ResultSet resultSet = statement.executeQuery()) {
					while (resultSet.next()) {
						names.add(resultSet.getString("flight_name"));
					}
				}

				// get contribution names of remaining items
				for (int i = 1; i < items_.size(); i++) {
					statement.setInt(1, items_.get(i).getID());
					try (ResultSet resultSet = statement.executeQuery()) {

						// get names
						names1.clear();
						while (resultSet.next()) {
							names1.add(resultSet.getString("flight_name"));
						}

						// get to-be-removed contributions
						toBeRemoved.clear();
						for (String name : names) {
							if (!names1.contains(name)) {
								toBeRemoved.add(name);
							}
						}

						// remove contributions
						names.removeAll(toBeRemoved);
					}
				}
			}
		}

		// sort typical flight names
		Collections.sort(names, new AlphanumComparator());

		// return names
		return names;
	}

	/**
	 * Gets contribution names from input STF file buckets.
	 *
	 * @param withOccurrences
	 *            True if contributions with occurrences shall be returned.
	 * @return Contribution names.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private List<String> getFromBuckets(boolean withOccurrences) throws Exception {

		// create lists
		boolean firstNamesSet = false;
		ArrayList<String> names = new ArrayList<>(), names1 = new ArrayList<>(), toBeRemoved = new ArrayList<>();

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// prepare statement for getting STF files
			String sql = "select file_id from stf_files where cdf_id = ?";
			try (PreparedStatement getSTFIDs = connection.prepareStatement(sql)) {

				// prepare statement for getting contribution IDs
				sql = "select id from flight_dam_contributions where stf_id = ?";
				try (PreparedStatement getContributionIDs = connection.prepareStatement(sql)) {

					// prepare statement to get contribution names
					String tableName = withOccurrences ? "flight_dam_contribution_with_occurrences" : "flight_dam_contribution_without_occurrences";
					sql = "select flight_name from " + tableName + " where id = ? order by flight_name";
					try (PreparedStatement getContributionNames = connection.prepareStatement(sql)) {

						// loop over buckets
						for (SpectrumItem item : items_) {

							// cast to bucket
							STFFileBucket bucket = (STFFileBucket) item;

							// get STF IDs
							getSTFIDs.setInt(1, bucket.getParentItem().getID());
							try (ResultSet stfIDs = getSTFIDs.executeQuery()) {

								// loop over STF IDs
								while (stfIDs.next()) {

									// get contribution IDs
									getContributionIDs.setInt(1, stfIDs.getInt("file_id"));
									try (ResultSet contributionIDs = getContributionIDs.executeQuery()) {

										// loop over contribution IDs
										while (contributionIDs.next()) {

											// get contribution names
											getContributionNames.setInt(1, contributionIDs.getInt("id"));
											try (ResultSet contributionNames = getContributionNames.executeQuery()) {

												// get names of first contribution
												if (!firstNamesSet) {
													while (contributionNames.next()) {
														names.add(contributionNames.getString("flight_name"));
													}
													firstNamesSet = true;
												}

												// get names of remaining contributions
												else {

													// get names
													names1.clear();
													while (contributionNames.next()) {
														names1.add(contributionNames.getString("flight_name"));
													}

													// get to-be-removed contributions
													toBeRemoved.clear();
													for (String name : names) {
														if (!names1.contains(name)) {
															toBeRemoved.add(name);
														}
													}

													// remove contributions
													names.removeAll(toBeRemoved);
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

		// sort typical flight names
		Collections.sort(names, new AlphanumComparator());

		// return names
		return names;
	}

	/**
	 * Interface for typical flight damage contribution requesting panels.
	 *
	 * @author Murat Artim
	 * @date Sep 3, 2015
	 * @time 2:02:03 PM
	 */
	public interface FlightDamageContributionRequestingPanel {

		/**
		 * Sets contributions to this panel.
		 *
		 * @param withOccurrences
		 *            Contributions with flight occurrences to be set.
		 * @param withoutOccurrences
		 *            Contributions with flight occurrences to be set.
		 */
		void setFlightContributions(List<String> withOccurrences, List<String> withoutOccurrences);
	}
}
