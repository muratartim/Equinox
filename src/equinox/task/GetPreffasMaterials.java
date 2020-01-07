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
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import equinox.Equinox;
import equinox.data.IsamiVersion;
import equinox.data.material.PreffasMaterialItem;
import equinox.task.InternalEquinoxTask.ShortRunningTask;

/**
 * Class for get Preffas materials task.
 *
 * @author Murat Artim
 * @date Nov 30, 2015
 * @time 10:44:36 AM
 */
public class GetPreffasMaterials extends InternalEquinoxTask<ArrayList<PreffasMaterialItem>> implements ShortRunningTask {

	/** Requesting panel. */
	private final PreffasMaterialRequestingPanel panel_;

	/** ISAMI version to get the materials. */
	private final IsamiVersion isamiVersion_;

	/**
	 * Creates get Preffas materials task.
	 *
	 * @param panel
	 *            Requesting panel.
	 * @param isamiVersion
	 *            ISAMI version to get the materials.
	 */
	public GetPreffasMaterials(PreffasMaterialRequestingPanel panel, IsamiVersion isamiVersion) {
		panel_ = panel;
		isamiVersion_ = isamiVersion;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Get Preffas materials";
	}

	@Override
	protected ArrayList<PreffasMaterialItem> call() throws Exception {

		// update progress info
		updateTitle("Getting Preffas materials");

		// initialize list
		ArrayList<PreffasMaterialItem> materials = new ArrayList<>();

		// update info
		updateMessage("Getting Preffas materials from local database...");

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// create statement
			try (Statement statement = connection.createStatement()) {

				// execute statement
				String sql = "select * from preffas_materials where isami_version = '" + isamiVersion_.getIsamiVersion() + "' ";
				sql += "order by name, specification, family, orientation, configuration";
				try (ResultSet resultSet = statement.executeQuery(sql)) {

					// loop over materials
					while (resultSet.next()) {

						// create item
						PreffasMaterialItem item = new PreffasMaterialItem(resultSet.getInt("id"));
						item.setName(resultSet.getString("name"));
						item.setSpecification(resultSet.getString("specification"));
						item.setLibraryVersion(resultSet.getString("library_version"));
						item.setFamily(resultSet.getString("family"));
						item.setOrientation(resultSet.getString("orientation"));
						item.setConfiguration(resultSet.getString("configuration"));
						item.setCeff(resultSet.getDouble("par_ceff"));
						item.setM(resultSet.getDouble("par_m"));
						item.setA(resultSet.getDouble("par_a"));
						item.setB(resultSet.getDouble("par_b"));
						item.setC(resultSet.getDouble("par_c"));
						item.setFtu(resultSet.getDouble("par_ftu"));
						item.setFty(resultSet.getDouble("par_fty"));
						item.setIsamiVersion(resultSet.getString("isami_version"));

						// add to list
						materials.add(item);
					}
				}
			}
		}

		// return list
		return materials;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set results to panel
		try {
			panel_.setPreffasMaterials(get());
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	/**
	 * Interface for preffas material requesting panels.
	 *
	 * @author Murat Artim
	 * @date Mar 3, 2016
	 * @time 10:50:08 AM
	 */
	public interface PreffasMaterialRequestingPanel {

		/**
		 * Sets Preffas materials.
		 *
		 * @param preffasMaterials
		 *            Preffas materials.
		 */
		void setPreffasMaterials(ArrayList<PreffasMaterialItem> preffasMaterials);
	}
}
