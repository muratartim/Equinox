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
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import equinox.Equinox;
import equinox.dataServer.remote.data.FatigueMaterial;
import equinox.dataServer.remote.data.LinearMaterial;
import equinox.dataServer.remote.data.PreffasMaterial;
import equinox.dataServer.remote.message.DataMessage;
import equinox.dataServer.remote.message.DatabaseQueryFailed;
import equinox.dataServer.remote.message.DatabaseQueryPermissionDenied;
import equinox.dataServer.remote.message.GetFatigueMaterialsRequest;
import equinox.dataServer.remote.message.GetFatigueMaterialsResponse;
import equinox.dataServer.remote.message.GetLinearMaterialsRequest;
import equinox.dataServer.remote.message.GetLinearMaterialsResponse;
import equinox.dataServer.remote.message.GetPreffasMaterialsRequest;
import equinox.dataServer.remote.message.GetPreffasMaterialsResponse;
import equinox.network.DataServerManager;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.utility.exception.PermissionDeniedException;
import equinox.utility.exception.ServerDatabaseQueryFailedException;

/**
 * Class for update material library task.
 *
 * @author Murat Artim
 * @date 31 May 2017
 * @time 17:35:42
 *
 */
public class UpdateMaterialLibrary extends InternalEquinoxTask<Void> implements LongRunningTask, DatabaseQueryListenerTask {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** List containing the material ISAMI versions to be downloaded. */
	private final ArrayList<String> toBeDownloaded;

	/** Server query completion indicator. */
	private final AtomicBoolean isQueryCompleted;

	/** Server query message. */
	private final AtomicReference<DataMessage> serverMessageRef;

	/**
	 * Creates update material library task.
	 *
	 * @param toBeDownloaded
	 *            List containing the material ISAMI versions to be downloaded.
	 */
	public UpdateMaterialLibrary(ArrayList<String> toBeDownloaded) {
		this.toBeDownloaded = toBeDownloaded;
		isQueryCompleted = new AtomicBoolean();
		serverMessageRef = new AtomicReference<>(null);
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Update material library";
	}

	@Override
	public void respondToDataMessage(DataMessage message) throws Exception {
		processServerDataMessage(message, this, serverMessageRef, isQueryCompleted);
	}

	@Override
	protected Void call() throws Exception {

		// check permission
		checkPermission(Permission.GET_MATERIALS);

		// get connection to local database
		try (Connection localConnection = Equinox.DBC_POOL.getConnection()) {

			try {

				// disable auto-commit
				localConnection.setAutoCommit(false);

				// download material tables
				updateMessage("Downloading materials from central database...");
				downloadMaterials(localConnection);

				// commit updates
				localConnection.commit();
				localConnection.setAutoCommit(true);
			}

			// exception occurred during process
			catch (Exception e) {

				// roll back updates
				if (localConnection != null) {
					localConnection.rollback();
					localConnection.setAutoCommit(true);
				}

				// propagate exception
				throw e;
			}
		}

		// return
		return null;
	}

	/**
	 * Downloads and loads all materials to local database.
	 *
	 * @param localConnection
	 *            Local database connection.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void downloadMaterials(Connection localConnection) throws Exception {

		// prepare statement to insert fatigue materials into local database
		String sql = "insert into fatigue_materials(name, specification, library_version, family, orientation, configuration, par_p, par_q, par_m, isami_version) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		try (PreparedStatement insertFatigueMaterials = localConnection.prepareStatement(sql)) {

			// prepare statement to insert linear propagation materials into local database
			sql = "insert into linear_materials(name, specification, library_version, family, orientation, configuration, par_ceff, par_m, par_a, par_b, par_c, par_ftu, par_fty, isami_version) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
			try (PreparedStatement insertLinearMaterial = localConnection.prepareStatement(sql)) {

				// prepare statement to insert preffas propagation materials into local database
				sql = "insert into preffas_materials(name, specification, library_version, family, orientation, configuration, par_ceff, par_m, par_a, par_b, par_c, par_ftu, par_fty, isami_version) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
				try (PreparedStatement insertPreffasMaterial = localConnection.prepareStatement(sql)) {

					// loop over material ISAMI versions to be downloaded
					for (String materialIsamiVersion : toBeDownloaded) {

						// update progress info
						updateMessage("Downloading version '" + materialIsamiVersion + "'...");

						// insert fatigue materials
						insertFatigueMaterials(insertFatigueMaterials, materialIsamiVersion);

						// insert linear materials
						insertLinearMaterials(insertLinearMaterial, materialIsamiVersion);

						// insert preffas materials
						insertPreffasMaterials(insertPreffasMaterial, materialIsamiVersion);
					}
				}
			}
		}
	}

	/**
	 * Inserts propagation materials into local database.
	 *
	 * @param statement
	 *            Database statement.
	 * @param materialIsamiVersion
	 *            Material ISAMI version to be downloaded.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void insertPreffasMaterials(PreparedStatement statement, String materialIsamiVersion) throws Exception {

		// get preffas materials
		ArrayList<PreffasMaterial> materials = getPreffasMaterials(materialIsamiVersion);

		// loop over materials
		for (PreffasMaterial material : materials) {

			// get material attributes
			String name = material.getName();
			String specification = material.getSpecification();
			String libraryVersion = material.getLibraryVersion();
			String family = material.getFamily();
			String orientation = material.getOrientation();
			String configuration = material.getConfiguration();
			double parCeff = material.getCeff();
			double parM = material.getM();
			double parA = material.getA();
			double parB = material.getB();
			double parC = material.getC();
			double parFtu = material.getFtu();
			double parFty = material.getFty();
			String isamiVersion = material.getIsamiVersion();

			// insert
			statement.setString(1, name);
			if (specification == null) {
				statement.setNull(2, java.sql.Types.VARCHAR);
			}
			else {
				statement.setString(2, specification);
			}
			if (libraryVersion == null) {
				statement.setNull(3, java.sql.Types.VARCHAR);
			}
			else {
				statement.setString(3, libraryVersion);
			}
			if (family == null) {
				statement.setNull(4, java.sql.Types.VARCHAR);
			}
			else {
				statement.setString(4, family);
			}
			if (orientation == null) {
				statement.setNull(5, java.sql.Types.VARCHAR);
			}
			else {
				statement.setString(5, orientation);
			}
			if (configuration == null) {
				statement.setNull(6, java.sql.Types.VARCHAR);
			}
			else {
				statement.setString(6, configuration);
			}
			statement.setDouble(7, parCeff);
			statement.setDouble(8, parM);
			statement.setDouble(9, parA);
			statement.setDouble(10, parB);
			statement.setDouble(11, parC);
			statement.setDouble(12, parFtu);
			statement.setDouble(13, parFty);
			statement.setString(14, isamiVersion);
			statement.executeUpdate();
		}
	}

	/**
	 * Inserts propagation materials into local database.
	 *
	 * @param statement
	 *            Database statement.
	 * @param materialIsamiVersion
	 *            Material ISAMI version to be downloaded.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void insertLinearMaterials(PreparedStatement statement, String materialIsamiVersion) throws Exception {

		// get linear materials
		ArrayList<LinearMaterial> materials = getLinearMaterials(materialIsamiVersion);

		// loop over materials
		for (LinearMaterial material : materials) {

			// get material attributes
			String name = material.getName();
			String specification = material.getSpecification();
			String libraryVersion = material.getLibraryVersion();
			String family = material.getFamily();
			String orientation = material.getOrientation();
			String configuration = material.getConfiguration();
			double parCeff = material.getCeff();
			double parM = material.getM();
			double parA = material.getA();
			double parB = material.getB();
			double parC = material.getC();
			double parFtu = material.getFtu();
			double parFty = material.getFty();
			String isamiVersion = material.getIsamiVersion();

			// insert
			statement.setString(1, name);
			if (specification == null) {
				statement.setNull(2, java.sql.Types.VARCHAR);
			}
			else {
				statement.setString(2, specification);
			}
			if (libraryVersion == null) {
				statement.setNull(3, java.sql.Types.VARCHAR);
			}
			else {
				statement.setString(3, libraryVersion);
			}
			if (family == null) {
				statement.setNull(4, java.sql.Types.VARCHAR);
			}
			else {
				statement.setString(4, family);
			}
			if (orientation == null) {
				statement.setNull(5, java.sql.Types.VARCHAR);
			}
			else {
				statement.setString(5, orientation);
			}
			if (configuration == null) {
				statement.setNull(6, java.sql.Types.VARCHAR);
			}
			else {
				statement.setString(6, configuration);
			}
			statement.setDouble(7, parCeff);
			statement.setDouble(8, parM);
			statement.setDouble(9, parA);
			statement.setDouble(10, parB);
			statement.setDouble(11, parC);
			statement.setDouble(12, parFtu);
			statement.setDouble(13, parFty);
			statement.setString(14, isamiVersion);
			statement.executeUpdate();
		}
	}

	/**
	 * Inserts fatigue materials into local database.
	 *
	 * @param statement
	 *            Database statement.
	 * @param materialIsamiVersion
	 *            Material ISAMI version to be downloaded.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void insertFatigueMaterials(PreparedStatement statement, String materialIsamiVersion) throws Exception {

		// get fatigue materials
		ArrayList<FatigueMaterial> materials = getFatigueMaterials(materialIsamiVersion);

		// loop over materials
		for (FatigueMaterial material : materials) {

			// get material attributes
			String name = material.getName();
			String specification = material.getSpecification();
			String libraryVersion = material.getLibraryVersion();
			String family = material.getFamily();
			String orientation = material.getOrientation();
			String configuration = material.getConfiguration();
			double parP = material.getP();
			double parQ = material.getQ();
			double parM = material.getM();
			String isamiVersion = material.getIsamiVersion();

			// insert
			statement.setString(1, name);
			if (specification == null) {
				statement.setNull(2, java.sql.Types.VARCHAR);
			}
			else {
				statement.setString(2, specification);
			}
			if (libraryVersion == null) {
				statement.setNull(3, java.sql.Types.VARCHAR);
			}
			else {
				statement.setString(3, libraryVersion);
			}
			if (family == null) {
				statement.setNull(4, java.sql.Types.VARCHAR);
			}
			else {
				statement.setString(4, family);
			}
			if (orientation == null) {
				statement.setNull(5, java.sql.Types.VARCHAR);
			}
			else {
				statement.setString(5, orientation);
			}
			if (configuration == null) {
				statement.setNull(6, java.sql.Types.VARCHAR);
			}
			else {
				statement.setString(6, configuration);
			}
			statement.setDouble(7, parP);
			statement.setDouble(8, parQ);
			statement.setDouble(9, parM);
			statement.setString(10, isamiVersion);
			statement.executeUpdate();
		}
	}

	/**
	 * Gets preffas materials from central database.
	 *
	 * @param materialIsamiVersion
	 *            Material ISAMI version to be downloaded.
	 * @return Preffas materials.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private ArrayList<PreffasMaterial> getPreffasMaterials(String materialIsamiVersion) throws Exception {

		// initialize variables
		DataServerManager watcher = null;
		boolean removeListener = false;

		try {

			// create request message
			GetPreffasMaterialsRequest request = new GetPreffasMaterialsRequest();
			request.setListenerHashCode(hashCode());
			request.setMaterialIsamiVersion(materialIsamiVersion);

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
			else if (message instanceof GetPreffasMaterialsResponse)
				return ((GetPreffasMaterialsResponse) message).getMaterials();

			// invalid server response
			throw new Exception("Invalid server response received.");
		}

		// remove from network watcher
		finally {
			if (watcher != null && removeListener) {
				watcher.removeMessageListener(this);
			}
			serverMessageRef.set(null);
			isQueryCompleted.set(false);
		}
	}

	/**
	 * Gets linear materials from central database.
	 *
	 * @param materialIsamiVersion
	 *            Material ISAMI version to be downloaded.
	 * @return Linear materials.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private ArrayList<LinearMaterial> getLinearMaterials(String materialIsamiVersion) throws Exception {

		// initialize variables
		DataServerManager watcher = null;
		boolean removeListener = false;

		try {

			// create request message
			GetLinearMaterialsRequest request = new GetLinearMaterialsRequest();
			request.setListenerHashCode(hashCode());
			request.setMaterialIsamiVersion(materialIsamiVersion);

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
			else if (message instanceof GetLinearMaterialsResponse)
				return ((GetLinearMaterialsResponse) message).getMaterials();

			// invalid server response
			throw new Exception("Invalid server response received.");
		}

		// remove from network watcher
		finally {
			if (watcher != null && removeListener) {
				watcher.removeMessageListener(this);
			}
			serverMessageRef.set(null);
			isQueryCompleted.set(false);
		}
	}

	/**
	 * Gets fatigue materials from central database.
	 *
	 * @param materialIsamiVersion
	 *            Material ISAMI version to be downloaded.
	 * @return Fatigue materials.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private ArrayList<FatigueMaterial> getFatigueMaterials(String materialIsamiVersion) throws Exception {

		// initialize variables
		DataServerManager watcher = null;
		boolean removeListener = false;

		try {

			// create request message
			GetFatigueMaterialsRequest request = new GetFatigueMaterialsRequest();
			request.setListenerHashCode(hashCode());
			request.setMaterialIsamiVersion(materialIsamiVersion);

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
			else if (message instanceof GetFatigueMaterialsResponse)
				return ((GetFatigueMaterialsResponse) message).getMaterials();

			// invalid server response
			throw new Exception("Invalid server response received.");
		}

		// remove from network watcher
		finally {
			if (watcher != null && removeListener) {
				watcher.removeMessageListener(this);
			}
			serverMessageRef.set(null);
			isQueryCompleted.set(false);
		}
	}
}
