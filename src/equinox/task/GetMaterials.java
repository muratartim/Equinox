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
import java.util.concurrent.ExecutionException;

import equinox.Equinox;
import equinox.data.IsamiVersion;
import equinox.data.fileType.ExternalStressSequence;
import equinox.data.fileType.STFFile;
import equinox.data.fileType.STFFileBucket;
import equinox.data.fileType.StressSequence;
import equinox.dataServer.remote.data.FatigueMaterial;
import equinox.dataServer.remote.data.LinearMaterial;
import equinox.dataServer.remote.data.Material;
import equinox.dataServer.remote.data.PreffasMaterial;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;

/**
 * Class for get materials task.
 *
 * @author Murat Artim
 * @date 11 Aug 2017
 * @time 13:27:53
 *
 */
public class GetMaterials extends InternalEquinoxTask<Material[]> implements ShortRunningTask {

	/** Info index. */
	public static final int FATIGUE_MATERIAL = 0, PREFFAS_MATERIAL = 1, LINEAR_MATERIAL = 2;

	/** Items. */
	private final ObservableList<TreeItem<String>> items_;

	/** Requesting panel. */
	private final MaterialRequestingPanel panel_;

	/** ISAMI version. */
	private final IsamiVersion isamiVersion_;

	/**
	 * Creates get materials task.
	 *
	 * @param items
	 *            Items to get the material for.
	 * @param panel
	 *            Requesting panel.
	 * @param isamiVersion
	 *            ISAMI version.
	 */
	public GetMaterials(ObservableList<TreeItem<String>> items, MaterialRequestingPanel panel, IsamiVersion isamiVersion) {
		items_ = items;
		panel_ = panel;
		isamiVersion_ = isamiVersion;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Get materials";
	}

	@Override
	protected Material[] call() throws Exception {

		// update progress info
		updateMessage("Getting materials from database");

		// create info list
		Material[] materials = new Material[3];

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// create statement
			try (Statement statement = connection.createStatement()) {

				// get first item
				TreeItem<String> item = items_.get(0);

				// STF file
				if (item instanceof STFFile) {
					getSTFMaterialInfo(materials, statement);
				}

				// STF file bucket
				else if (item instanceof STFFileBucket) {
					getSTFBucketMaterialInfo(materials, statement);
				}

				// stress sequence
				else if (item instanceof StressSequence) {
					getStressSequenceMaterialInfo(materials, statement);
				}

				// external stress sequence
				else if (item instanceof ExternalStressSequence) {
					getExternalStressSequenceMaterialInfo(materials, statement);
				}
			}
		}

		// return materials
		return materials;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set file info
		try {
			panel_.setMaterials(get());
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	/**
	 * Returns external stress sequence file material info.
	 *
	 * @param materials
	 *            Material array.
	 * @param statement
	 *            Database statement.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void getExternalStressSequenceMaterialInfo(Material[] materials, Statement statement) throws Exception {

		// get null fatigue materials
		boolean hasNull = false;
		int fatigueCount = 0;
		String sql = "select count(file_id) as nullCount from ext_sth_files where fatigue_material is null and (";
		for (TreeItem<String> item : items_) {
			sql += "file_id = " + ((ExternalStressSequence) item).getID() + " or ";
		}
		sql = sql.substring(0, sql.lastIndexOf(" or ")) + ")";
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				hasNull = resultSet.getInt("nullCount") != 0;
			}
		}

		// no null
		if (!hasNull) {

			// get distinct fatigue material count
			sql = "select count(distinct fatigue_material) as fatigueCount from ext_sth_files where ";
			for (TreeItem<String> item : items_) {
				sql += "file_id = " + ((ExternalStressSequence) item).getID() + " or ";
			}
			sql = sql.substring(0, sql.lastIndexOf(" or "));
			try (ResultSet resultSet = statement.executeQuery(sql)) {
				while (resultSet.next()) {
					fatigueCount = resultSet.getInt("fatigueCount");
				}
			}
		}

		// get null preffas materials
		hasNull = false;
		int preffasCount = 0;
		sql = "select count(file_id) as nullCount from ext_sth_files where preffas_material is null and (";
		for (TreeItem<String> item : items_) {
			sql += "file_id = " + ((ExternalStressSequence) item).getID() + " or ";
		}
		sql = sql.substring(0, sql.lastIndexOf(" or ")) + ")";
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				hasNull = resultSet.getInt("nullCount") != 0;
			}
		}

		// no null
		if (!hasNull) {

			// get distinct preffas material count
			sql = "select count(distinct preffas_material) as preffasCount from ext_sth_files where ";
			for (TreeItem<String> item : items_) {
				sql += "file_id = " + ((ExternalStressSequence) item).getID() + " or ";
			}
			sql = sql.substring(0, sql.lastIndexOf(" or "));
			try (ResultSet resultSet = statement.executeQuery(sql)) {
				while (resultSet.next()) {
					preffasCount = resultSet.getInt("preffasCount");
				}
			}
		}

		// get null linear materials
		hasNull = false;
		int linearCount = 0;
		sql = "select count(file_id) as nullCount from ext_sth_files where linear_material is null and (";
		for (TreeItem<String> item : items_) {
			sql += "file_id = " + ((ExternalStressSequence) item).getID() + " or ";
		}
		sql = sql.substring(0, sql.lastIndexOf(" or ")) + ")";
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				hasNull = resultSet.getInt("nullCount") != 0;
			}
		}

		// no null
		if (!hasNull) {

			// get linear material count
			sql = "select count(distinct linear_material) as linearCount from ext_sth_files where ";
			for (TreeItem<String> item : items_) {
				sql += "file_id = " + ((ExternalStressSequence) item).getID() + " or ";
			}
			sql = sql.substring(0, sql.lastIndexOf(" or "));
			try (ResultSet resultSet = statement.executeQuery(sql)) {
				while (resultSet.next()) {
					linearCount = resultSet.getInt("linearCount");
				}
			}
		}

		// no material or more than 1 type of material
		if (fatigueCount != 1 && preffasCount != 1 && linearCount != 1)
			return;

		// set material info
		String fatigueMaterialName = null, preffasMaterialName = null, linearMaterialName = null;
		sql = "select fatigue_material, preffas_material, linear_material from ext_sth_files where file_id = " + ((ExternalStressSequence) items_.get(0)).getID();
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				fatigueMaterialName = fatigueCount == 1 ? resultSet.getString("fatigue_material") : null;
				preffasMaterialName = preffasCount == 1 ? resultSet.getString("preffas_material") : null;
				linearMaterialName = linearCount == 1 ? resultSet.getString("linear_material") : null;
			}
		}

		// get fatigue material
		materials[FATIGUE_MATERIAL] = fatigueMaterialName == null ? null : getFatigueMaterial(fatigueMaterialName, statement);
		materials[PREFFAS_MATERIAL] = preffasMaterialName == null ? null : getPreffasMaterial(preffasMaterialName, statement);
		materials[LINEAR_MATERIAL] = linearMaterialName == null ? null : getLinearMaterial(linearMaterialName, statement);
	}

	/**
	 * Returns STF bucket file material info.
	 *
	 * @param materials
	 *            Material array.
	 * @param statement
	 *            Database statement.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void getSTFBucketMaterialInfo(Material[] materials, Statement statement) throws Exception {

		// get null fatigue materials
		boolean hasNull = false;
		int fatigueCount = 0;
		String sql = "select count(file_id) as nullCount from stf_files where fatigue_material is null and (";
		for (TreeItem<String> item : items_) {
			sql += "cdf_id = " + ((STFFileBucket) item).getParentItem().getID() + " or ";
		}
		sql = sql.substring(0, sql.lastIndexOf(" or ")) + ")";
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				hasNull = resultSet.getInt("nullCount") != 0;
			}
		}

		// no null
		if (!hasNull) {

			// get fatigue material count
			sql = "select count(distinct fatigue_material) as fatigueCount from stf_files where ";
			for (TreeItem<String> item : items_) {
				sql += "cdf_id = " + ((STFFileBucket) item).getParentItem().getID() + " or ";
			}
			sql = sql.substring(0, sql.lastIndexOf(" or "));
			try (ResultSet resultSet = statement.executeQuery(sql)) {
				while (resultSet.next()) {
					fatigueCount = resultSet.getInt("fatigueCount");
				}
			}
		}

		// get null preffas materials
		hasNull = false;
		int preffasCount = 0;
		sql = "select count(file_id) as nullCount from stf_files where preffas_material is null and (";
		for (TreeItem<String> item : items_) {
			sql += "cdf_id = " + ((STFFileBucket) item).getParentItem().getID() + " or ";
		}
		sql = sql.substring(0, sql.lastIndexOf(" or ")) + ")";
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				hasNull = resultSet.getInt("nullCount") != 0;
			}
		}

		// no null
		if (!hasNull) {

			// get preffas material count
			sql = "select count(distinct preffas_material) as preffasCount from stf_files where ";
			for (TreeItem<String> item : items_) {
				sql += "cdf_id = " + ((STFFileBucket) item).getParentItem().getID() + " or ";
			}
			sql = sql.substring(0, sql.lastIndexOf(" or "));
			try (ResultSet resultSet = statement.executeQuery(sql)) {
				while (resultSet.next()) {
					preffasCount = resultSet.getInt("preffasCount");
				}
			}
		}

		// get null linear materials
		hasNull = false;
		int linearCount = 0;
		sql = "select count(file_id) as nullCount from stf_files where linear_material is null and (";
		for (TreeItem<String> item : items_) {
			sql += "cdf_id = " + ((STFFileBucket) item).getParentItem().getID() + " or ";
		}
		sql = sql.substring(0, sql.lastIndexOf(" or ")) + ")";
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				hasNull = resultSet.getInt("nullCount") != 0;
			}
		}

		// no null
		if (!hasNull) {

			// get linear material count
			sql = "select count(distinct linear_material) as linearCount from stf_files where ";
			for (TreeItem<String> item : items_) {
				sql += "cdf_id = " + ((STFFileBucket) item).getParentItem().getID() + " or ";
			}
			sql = sql.substring(0, sql.lastIndexOf(" or "));
			try (ResultSet resultSet = statement.executeQuery(sql)) {
				while (resultSet.next()) {
					linearCount = resultSet.getInt("linearCount");
				}
			}
		}

		// no material or more than 1 type of material
		if (fatigueCount != 1 && preffasCount != 1 && linearCount != 1)
			return;

		// set material info
		String fatigueMaterialName = null, preffasMaterialName = null, linearMaterialName = null;
		sql = "select fatigue_material, preffas_material, linear_material from stf_files where cdf_id = " + ((STFFileBucket) items_.get(0)).getParentItem().getID();
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				fatigueMaterialName = fatigueCount == 1 ? resultSet.getString("fatigue_material") : null;
				preffasMaterialName = preffasCount == 1 ? resultSet.getString("preffas_material") : null;
				linearMaterialName = linearCount == 1 ? resultSet.getString("linear_material") : null;
				break;
			}
		}

		// get fatigue material
		materials[FATIGUE_MATERIAL] = fatigueMaterialName == null ? null : getFatigueMaterial(fatigueMaterialName, statement);
		materials[PREFFAS_MATERIAL] = preffasMaterialName == null ? null : getPreffasMaterial(preffasMaterialName, statement);
		materials[LINEAR_MATERIAL] = linearMaterialName == null ? null : getLinearMaterial(linearMaterialName, statement);
	}

	/**
	 * Returns stress sequence file materials.
	 *
	 * @param materials
	 *            Material array.
	 * @param statement
	 *            Database statement.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void getStressSequenceMaterialInfo(Material[] materials, Statement statement) throws Exception {

		// get null fatigue materials
		boolean hasNull = false;
		int fatigueCount = 0;
		String sql = "select count(file_id) as nullCount from stf_files where fatigue_material is null and (";
		for (TreeItem<String> item : items_) {
			sql += "file_id = " + ((StressSequence) item).getParentItem().getID() + " or ";
		}
		sql = sql.substring(0, sql.lastIndexOf(" or ")) + ")";
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				hasNull = resultSet.getInt("nullCount") != 0;
			}
		}

		// no null
		if (!hasNull) {

			// get fatigue material count
			sql = "select count(distinct fatigue_material) as fatigueCount from stf_files where ";
			for (TreeItem<String> item : items_) {
				sql += "file_id = " + ((StressSequence) item).getParentItem().getID() + " or ";
			}
			sql = sql.substring(0, sql.lastIndexOf(" or "));
			try (ResultSet resultSet = statement.executeQuery(sql)) {
				while (resultSet.next()) {
					fatigueCount = resultSet.getInt("fatigueCount");
				}
			}
		}

		// get null preffas materials
		hasNull = false;
		int preffasCount = 0;
		sql = "select count(file_id) as nullCount from stf_files where preffas_material is null and (";
		for (TreeItem<String> item : items_) {
			sql += "file_id = " + ((StressSequence) item).getParentItem().getID() + " or ";
		}
		sql = sql.substring(0, sql.lastIndexOf(" or ")) + ")";
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				hasNull = resultSet.getInt("nullCount") != 0;
			}
		}

		// no null
		if (!hasNull) {

			// get preffas material count
			sql = "select count(distinct preffas_material) as preffasCount from stf_files where ";
			for (TreeItem<String> item : items_) {
				sql += "file_id = " + ((StressSequence) item).getParentItem().getID() + " or ";
			}
			sql = sql.substring(0, sql.lastIndexOf(" or "));
			try (ResultSet resultSet = statement.executeQuery(sql)) {
				while (resultSet.next()) {
					preffasCount = resultSet.getInt("preffasCount");
				}
			}
		}

		// get null linear materials
		hasNull = false;
		int linearCount = 0;
		sql = "select count(file_id) as nullCount from stf_files where linear_material is null and (";
		for (TreeItem<String> item : items_) {
			sql += "file_id = " + ((StressSequence) item).getParentItem().getID() + " or ";
		}
		sql = sql.substring(0, sql.lastIndexOf(" or ")) + ")";
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				hasNull = resultSet.getInt("nullCount") != 0;
			}
		}

		// no null
		if (!hasNull) {

			// get linear material count
			sql = "select count(distinct linear_material) as linearCount from stf_files where ";
			for (TreeItem<String> item : items_) {
				sql += "file_id = " + ((StressSequence) item).getParentItem().getID() + " or ";
			}
			sql = sql.substring(0, sql.lastIndexOf(" or "));
			try (ResultSet resultSet = statement.executeQuery(sql)) {
				while (resultSet.next()) {
					linearCount = resultSet.getInt("linearCount");
				}
			}
		}

		// no material or more than 1 type of material
		if (fatigueCount != 1 && preffasCount != 1 && linearCount != 1)
			return;

		// set material info
		String fatigueMaterialName = null, preffasMaterialName = null, linearMaterialName = null;
		sql = "select fatigue_material, preffas_material, linear_material from stf_files where file_id = " + ((StressSequence) items_.get(0)).getParentItem().getID();
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				fatigueMaterialName = fatigueCount == 1 ? resultSet.getString("fatigue_material") : null;
				preffasMaterialName = preffasCount == 1 ? resultSet.getString("preffas_material") : null;
				linearMaterialName = linearCount == 1 ? resultSet.getString("linear_material") : null;
			}
		}

		// get fatigue material
		materials[FATIGUE_MATERIAL] = fatigueMaterialName == null ? null : getFatigueMaterial(fatigueMaterialName, statement);
		materials[PREFFAS_MATERIAL] = preffasMaterialName == null ? null : getPreffasMaterial(preffasMaterialName, statement);
		materials[LINEAR_MATERIAL] = linearMaterialName == null ? null : getLinearMaterial(linearMaterialName, statement);
	}

	/**
	 * Returns STF file materials.
	 *
	 * @param materials
	 *            Material array.
	 * @param statement
	 *            Database statement.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void getSTFMaterialInfo(Material[] materials, Statement statement) throws Exception {

		// get null fatigue materials
		boolean hasNull = false;
		int fatigueCount = 0;
		String sql = "select count(file_id) as nullCount from stf_files where fatigue_material is null and (";
		for (TreeItem<String> item : items_) {
			sql += "file_id = " + ((STFFile) item).getID() + " or ";
		}
		sql = sql.substring(0, sql.lastIndexOf(" or ")) + ")";
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				hasNull = resultSet.getInt("nullCount") != 0;
			}
		}

		// no null
		if (!hasNull) {

			// get fatigue material count
			sql = "select count(distinct fatigue_material) as fatigueCount from stf_files where ";
			for (TreeItem<String> item : items_) {
				sql += "file_id = " + ((STFFile) item).getID() + " or ";
			}
			sql = sql.substring(0, sql.lastIndexOf(" or "));
			try (ResultSet resultSet = statement.executeQuery(sql)) {
				while (resultSet.next()) {
					fatigueCount = resultSet.getInt("fatigueCount");
				}
			}
		}

		// get null preffas materials
		hasNull = false;
		int preffasCount = 0;
		sql = "select count(file_id) as nullCount from stf_files where preffas_material is null and (";
		for (TreeItem<String> item : items_) {
			sql += "file_id = " + ((STFFile) item).getID() + " or ";
		}
		sql = sql.substring(0, sql.lastIndexOf(" or ")) + ")";
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				hasNull = resultSet.getInt("nullCount") != 0;
			}
		}

		// no null
		if (!hasNull) {

			// get preffas material count
			sql = "select count(distinct preffas_material) as preffasCount from stf_files where ";
			for (TreeItem<String> item : items_) {
				sql += "file_id = " + ((STFFile) item).getID() + " or ";
			}
			sql = sql.substring(0, sql.lastIndexOf(" or "));
			try (ResultSet resultSet = statement.executeQuery(sql)) {
				while (resultSet.next()) {
					preffasCount = resultSet.getInt("preffasCount");
				}
			}
		}

		// get null linear materials
		hasNull = false;
		int linearCount = 0;
		sql = "select count(file_id) as nullCount from stf_files where linear_material is null and (";
		for (TreeItem<String> item : items_) {
			sql += "file_id = " + ((STFFile) item).getID() + " or ";
		}
		sql = sql.substring(0, sql.lastIndexOf(" or ")) + ")";
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				hasNull = resultSet.getInt("nullCount") != 0;
			}
		}

		// no null
		if (!hasNull) {

			// get linear material count
			sql = "select count(distinct linear_material) as linearCount from stf_files where ";
			for (TreeItem<String> item : items_) {
				sql += "file_id = " + ((STFFile) item).getID() + " or ";
			}
			sql = sql.substring(0, sql.lastIndexOf(" or "));
			try (ResultSet resultSet = statement.executeQuery(sql)) {
				while (resultSet.next()) {
					linearCount = resultSet.getInt("linearCount");
				}
			}
		}

		// no material or more than 1 type of material
		if (fatigueCount != 1 && preffasCount != 1 && linearCount != 1)
			return;

		// get material info
		String fatigueMaterialName = null, preffasMaterialName = null, linearMaterialName = null;
		sql = "select fatigue_material, preffas_material, linear_material from stf_files where file_id = " + ((STFFile) items_.get(0)).getID();
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				fatigueMaterialName = fatigueCount == 1 ? resultSet.getString("fatigue_material") : null;
				preffasMaterialName = preffasCount == 1 ? resultSet.getString("preffas_material") : null;
				linearMaterialName = linearCount == 1 ? resultSet.getString("linear_material") : null;
			}
		}

		// get fatigue material
		materials[FATIGUE_MATERIAL] = fatigueMaterialName == null ? null : getFatigueMaterial(fatigueMaterialName, statement);
		materials[PREFFAS_MATERIAL] = preffasMaterialName == null ? null : getPreffasMaterial(preffasMaterialName, statement);
		materials[LINEAR_MATERIAL] = linearMaterialName == null ? null : getLinearMaterial(linearMaterialName, statement);
	}

	/**
	 * Returns the material for the given material name, or null if the material could not be found.
	 *
	 * @param fatigueMaterialName
	 *            Material name.
	 * @param statement
	 *            Database statement.
	 * @return The material for the given material name, or null if the material could not be found.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private FatigueMaterial getFatigueMaterial(String fatigueMaterialName, Statement statement) throws Exception {

		// execute statement
		try (ResultSet resultSet = statement.executeQuery("select * from fatigue_materials where (name || '/' || specification || '/' || orientation || '/' || configuration || '/' || library_version) = '" + fatigueMaterialName + "'")) {

			// loop over materials
			while (resultSet.next()) {

				// get ISAMI version
				String isamiVersion = resultSet.getString("isami_version");

				// not the required one
				if (!isamiVersion.equals(isamiVersion_.getIsamiVersion())) {
					continue;
				}

				// create material
				FatigueMaterial material = new FatigueMaterial(resultSet.getInt("id"));
				material.setName(resultSet.getString("name"));
				material.setSpecification(resultSet.getString("specification"));
				material.setLibraryVersion(resultSet.getString("library_version"));
				material.setFamily(resultSet.getString("family"));
				material.setOrientation(resultSet.getString("orientation"));
				material.setConfiguration(resultSet.getString("configuration"));
				material.setP(resultSet.getDouble("par_p"));
				material.setQ(resultSet.getDouble("par_q"));
				material.setM(resultSet.getDouble("par_m"));
				material.setIsamiVersion(isamiVersion);

				// return material
				return material;
			}
		}

		// could not find material
		return null;
	}

	/**
	 * Returns the material for the given material name, or null if the material could not be found.
	 *
	 * @param preffasMaterialName
	 *            Material name.
	 * @param statement
	 *            Database statement.
	 * @return The material for the given material name, or null if the material could not be found.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private PreffasMaterial getPreffasMaterial(String preffasMaterialName, Statement statement) throws Exception {

		// execute statement
		try (ResultSet resultSet = statement.executeQuery("select * from preffas_materials where (name || '/' || specification || '/' || orientation || '/' || configuration || '/' || library_version) = '" + preffasMaterialName + "'")) {

			// loop over materials
			while (resultSet.next()) {

				// get ISAMI version
				String isamiVersion = resultSet.getString("isami_version");

				// not the required one
				if (!isamiVersion.equals(isamiVersion_.getIsamiVersion())) {
					continue;
				}

				// create material
				PreffasMaterial material = new PreffasMaterial(resultSet.getInt("id"));
				material.setName(resultSet.getString("name"));
				material.setSpecification(resultSet.getString("specification"));
				material.setLibraryVersion(resultSet.getString("library_version"));
				material.setFamily(resultSet.getString("family"));
				material.setOrientation(resultSet.getString("orientation"));
				material.setConfiguration(resultSet.getString("configuration"));
				material.setCeff(resultSet.getDouble("par_ceff"));
				material.setM(resultSet.getDouble("par_m"));
				material.setA(resultSet.getDouble("par_a"));
				material.setB(resultSet.getDouble("par_b"));
				material.setC(resultSet.getDouble("par_c"));
				material.setFtu(resultSet.getDouble("par_ftu"));
				material.setFty(resultSet.getDouble("par_fty"));
				material.setIsamiVersion(isamiVersion);

				// return material
				return material;
			}
		}

		// could not find material
		return null;
	}

	/**
	 * Returns the material for the given material name, or null if the material could not be found.
	 *
	 * @param linearMaterialName
	 *            Material name.
	 * @param statement
	 *            Database statement.
	 * @return The material for the given material name, or null if the material could not be found.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private LinearMaterial getLinearMaterial(String linearMaterialName, Statement statement) throws Exception {

		// execute statement
		try (ResultSet resultSet = statement.executeQuery("select * from linear_materials where (name || '/' || specification || '/' || orientation || '/' || configuration || '/' || library_version) = '" + linearMaterialName + "'")) {

			// loop over materials
			while (resultSet.next()) {

				// get ISAMI version
				String isamiVersion = resultSet.getString("isami_version");

				// not the required one
				if (!isamiVersion.equals(isamiVersion_.getIsamiVersion())) {
					continue;
				}

				// create item
				LinearMaterial material = new LinearMaterial(resultSet.getInt("id"));
				material.setName(resultSet.getString("name"));
				material.setSpecification(resultSet.getString("specification"));
				material.setLibraryVersion(resultSet.getString("library_version"));
				material.setFamily(resultSet.getString("family"));
				material.setOrientation(resultSet.getString("orientation"));
				material.setConfiguration(resultSet.getString("configuration"));
				material.setCeff(resultSet.getDouble("par_ceff"));
				material.setM(resultSet.getDouble("par_m"));
				material.setA(resultSet.getDouble("par_a"));
				material.setB(resultSet.getDouble("par_b"));
				material.setC(resultSet.getDouble("par_c"));
				material.setFtu(resultSet.getDouble("par_ftu"));
				material.setFty(resultSet.getDouble("par_fty"));
				material.setIsamiVersion(isamiVersion);

				// return material
				return material;
			}
		}

		// could not find material
		return null;
	}

	/**
	 * Interface for material requesting panels.
	 *
	 * @author Murat Artim
	 * @date Feb 2, 2016
	 * @time 4:18:25 PM
	 */
	public interface MaterialRequestingPanel {

		/**
		 * Sets materials to this panel.
		 *
		 * @param materials
		 *            Materials to set.
		 */
		void setMaterials(Material[] materials);
	}
}
