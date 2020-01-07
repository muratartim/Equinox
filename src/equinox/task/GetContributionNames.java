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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import equinox.Equinox;
import equinox.data.EmbeddedTask;
import equinox.data.ExecutionMode;
import equinox.data.Pair;
import equinox.data.fileType.STFFileBucket;
import equinox.data.fileType.SpectrumItem;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.task.automation.AutomaticTaskOwner;
import equinox.task.automation.MultipleInputTask;

/**
 * Class for get contribution names task.1
 *
 * @author Murat Artim
 * @date Apr 15, 2015
 * @time 9:15:11 PM
 */
public class GetContributionNames extends InternalEquinoxTask<List<String>> implements ShortRunningTask, MultipleInputTask<SpectrumItem>, AutomaticTaskOwner<Pair<List<SpectrumItem>, List<String>>> {

	/** Requesting object. */
	private final DamageContributionRequester panel_;

	/** Spectrum items. This can be either damage contributions or STF file buckets. */
	private List<SpectrumItem> items_;

	/** Input threshold. Once the threshold is reached, this task will be executed. */
	private volatile int inputThreshold_ = 0;

	/** Automatic tasks. */
	private HashMap<String, EmbeddedTask<Pair<List<SpectrumItem>, List<String>>>> automaticTasks_ = null;

	/**
	 * Creates get contribution names task.
	 *
	 * @param panel
	 *            Requesting object. This can be null.
	 * @param items
	 *            Spectrum items. This can be either damage contributions or STF file buckets. This can be null for automatic execution.
	 */
	public GetContributionNames(DamageContributionRequester panel, List<SpectrumItem> items) {
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
	public void addAutomaticTask(String taskID, EmbeddedTask<Pair<List<SpectrumItem>, List<String>>> task) {
		if (automaticTasks_ == null) {
			automaticTasks_ = new HashMap<>();
		}
		automaticTasks_.put(taskID, task);
	}

	@Override
	public HashMap<String, EmbeddedTask<Pair<List<SpectrumItem>, List<String>>>> getAutomaticTasks() {
		return automaticTasks_;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Get damage contribution names";
	}

	@Override
	protected List<String> call() throws Exception {

		// update progress info
		updateTitle("Retrieving damage contribution names...");

		// STF file buckets
		if (items_.get(0) instanceof STFFileBucket)
			return getFromBuckets();

		// contributions
		return getFromContributions();
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set file info
		try {

			// get output
			List<String> contributionNames = get();

			// user started task
			if (automaticTasks_ == null) {
				if (panel_ != null) {
					panel_.setContributions(contributionNames);
				}
			}

			// automatic task
			else {
				automaticTaskOwnerSucceeded(new Pair<>(items_, contributionNames), automaticTasks_, taskPanel_);
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
	 * @return Contribution names.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private ArrayList<String> getFromContributions() throws Exception {

		// create lists
		ArrayList<String> names = new ArrayList<>();

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// prepare statement
			String sql = "select distinct name from dam_contribution where ";
			String[] ids = new String[items_.size()];
			for (int i = 0; i < ids.length; i++) {
				ids[i] = "contributions_id = " + items_.get(i).getID();
			}
			sql += String.join(" or ", ids) + " order by name";

			// create statement
			try (Statement statement = connection.createStatement()) {

				// get contribution names of first item
				try (ResultSet resultSet = statement.executeQuery(sql)) {
					while (resultSet.next()) {
						names.add(resultSet.getString("name"));
					}
				}
			}
		}

		// return names
		return names;
	}

	/**
	 * Gets contribution names from input STF file buckets.
	 *
	 * @return Contribution names.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private ArrayList<String> getFromBuckets() throws Exception {

		// create lists
		boolean firstNamesSet = false;
		ArrayList<String> names = new ArrayList<>(), names1 = new ArrayList<>(), toBeRemoved = new ArrayList<>();

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// prepare statement for getting STF files
			String sql = "select file_id from stf_files where cdf_id = ?";
			try (PreparedStatement getSTFIDs = connection.prepareStatement(sql)) {

				// prepare statement for getting contribution IDs
				sql = "select contributions_id from dam_contributions where stf_id = ?";
				try (PreparedStatement getContributionIDs = connection.prepareStatement(sql)) {

					// prepare statement to get contribution names
					sql = "select name from dam_contribution where contributions_id = ? order by name";
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
											getContributionNames.setInt(1, contributionIDs.getInt("contributions_id"));
											try (ResultSet contributionNames = getContributionNames.executeQuery()) {

												// get names of first contribution
												if (!firstNamesSet) {
													while (contributionNames.next()) {
														names.add(contributionNames.getString("name"));
													}
													firstNamesSet = true;
												}

												// get names of remaining contributions
												else {

													// get names
													names1.clear();
													while (contributionNames.next()) {
														names1.add(contributionNames.getString("name"));
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

		// return names
		return names;
	}

	/**
	 * Interface for damage contribution requesting panels.
	 *
	 * @author Murat Artim
	 * @date Sep 3, 2015
	 * @time 2:02:03 PM
	 */
	public interface DamageContributionRequester {

		/**
		 * Sets contributions to this panel.
		 *
		 * @param contributions
		 *            Contributions to be set.
		 */
		void setContributions(List<String> contributions);
	}
}
