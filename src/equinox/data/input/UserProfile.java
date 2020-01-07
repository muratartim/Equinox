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
package equinox.data.input;

import equinox.serverUtilities.Permission;

/**
 * Enumeration for user profile.
 *
 * @author Murat Artim
 * @date 4 Apr 2018
 * @time 14:46:26
 */
public enum UserProfile {

	/** User profile. */
	// @formatter:off
	STANDARD_USER(Permission.SEE_CONNECTED_USERS, Permission.SEND_CHAT_MESSAGE, Permission.SHARE_FILE, Permission.SEARCH_MULTIPLICATION_TABLE,
			Permission.SEARCH_PILOT_POINT, Permission.SEARCH_SPECTRUM, Permission.CHECK_FOR_EQUINOX_UPDATES, Permission.CHECK_FOR_MATERIAL_UPDATES,
			Permission.GET_EQUINOX_PLUGIN_INFO, Permission.INSTALL_EQUINOX_PLUGIN, Permission.DOWNLOAD_HELP_VIDEO, Permission.DOWNLOAD_PILOT_POINT,
			Permission.DOWNLOAD_SAMPLE_INPUT, Permission.DOWNLOAD_MULTIPLICATION_TABLE, Permission.DOWNLOAD_SPECTRUM, Permission.GET_BUG_REPORTS,
			Permission.GET_HELP_VIDEOS, Permission.GET_WISHES, Permission.SUBMIT_BUG_REPORT, Permission.SUBMIT_WISH, Permission.GET_MATERIALS,
			Permission.GET_PILOT_POINT_IMAGES, Permission.EQUIVALENT_STRESS_ANALYSIS, Permission.DAMAGE_CONTRIBUTION_ANALYSIS,
			Permission.DAMAGE_ANGLE_ANALYSIS, Permission.ADD_NEW_SPECTRUM, Permission.ADD_NEW_STRESS_SEQUENCE, Permission.RUN_EXCALIBUR_PLUGIN,
			Permission.CHECK_FOR_PLUGIN_UPDATES, Permission.ADD_NEW_STF_FILE, Permission.CREATE_DUMMY_STF_FILE, Permission.EDIT_SPECTRUM_INFO,
			Permission.ASSIGN_MISSION_PARAMETERS_TO_SPECTRUM, Permission.RENAME_FILE, Permission.SAVE_FILE, Permission.EXPORT_SPECTRUM,
			Permission.EXPORT_LOADCASE_FACTOR_FILE, Permission.DELETE_FILE, Permission.GENERATE_STRESS_SEQUENCE, Permission.EDIT_PILOT_POINT_INFO,
			Permission.SET_PILOT_POINT_IMAGE, Permission.EXPORT_PILOT_POINT, Permission.EXPORT_DAMAGE_CONTRIBUTIONS, Permission.PLOT_TYPICAL_FLIGHT,
			Permission.PLOT_MISSION_PROFILE, Permission.PLOT_LEVEL_CROSSINGS, Permission.REMOVE_PLUGIN, Permission.PLOT_TYPICAL_FLIGHT_STATISTICS,
			Permission.PLOT_EQUIVALENT_STRESS_COMPARISON, Permission.PLOT_TYPICAL_FLIGHT_COMPARISON, Permission.PLOT_STRESS_SEQUENCE_COMPARISON,
			Permission.SUBMIT_ACCESS_REQUEST),
	ESCSAF_MEMBER(Permission.SEE_CONNECTED_USERS, Permission.SEND_CHAT_MESSAGE, Permission.SHARE_FILE, Permission.SEARCH_MULTIPLICATION_TABLE,
			Permission.SEARCH_PILOT_POINT, Permission.SEARCH_SPECTRUM, Permission.CHECK_FOR_EQUINOX_UPDATES, Permission.CHECK_FOR_MATERIAL_UPDATES,
			Permission.GET_EQUINOX_PLUGIN_INFO, Permission.INSTALL_EQUINOX_PLUGIN, Permission.DOWNLOAD_HELP_VIDEO, Permission.DOWNLOAD_PILOT_POINT,
			Permission.DOWNLOAD_SAMPLE_INPUT, Permission.DOWNLOAD_MULTIPLICATION_TABLE, Permission.DOWNLOAD_SPECTRUM, Permission.GET_BUG_REPORTS,
			Permission.GET_HELP_VIDEOS, Permission.GET_WISHES, Permission.PLOT_CONTRIBUTION_STATISTICS, Permission.PLOT_PILOT_POINT_COUNT,
			Permission.PLOT_SPECTRUM_COUNT, Permission.PLOT_SPECTRUM_SIZE, Permission.SUBMIT_BUG_REPORT, Permission.SUBMIT_WISH, Permission.GET_MATERIALS,
			Permission.GET_PILOT_POINT_IMAGES, Permission.EQUIVALENT_STRESS_ANALYSIS, Permission.DAMAGE_CONTRIBUTION_ANALYSIS, Permission.DAMAGE_ANGLE_ANALYSIS,
			Permission.ADD_NEW_SPECTRUM, Permission.ADD_NEW_STRESS_SEQUENCE, Permission.ADD_NEW_AIRCRAFT_MODEL, Permission.RUN_RFORT_EXTENDED_PLUGIN,
			Permission.RUN_ADAPT_DRF_PLUGIN, Permission.RUN_EXCALIBUR_PLUGIN, Permission.RUN_MYCHECK_PLUGIN, Permission.CHECK_FOR_PLUGIN_UPDATES,
			Permission.ADD_NEW_STF_FILE, Permission.CREATE_DUMMY_STF_FILE, Permission.EDIT_SPECTRUM_INFO, Permission.ASSIGN_MISSION_PARAMETERS_TO_SPECTRUM,
			Permission.RENAME_FILE, Permission.SAVE_FILE, Permission.EXPORT_SPECTRUM, Permission.EXPORT_LOADCASE_FACTOR_FILE, Permission.DELETE_FILE,
			Permission.GENERATE_STRESS_SEQUENCE, Permission.EDIT_PILOT_POINT_INFO, Permission.SET_PILOT_POINT_IMAGE, Permission.EXPORT_PILOT_POINT,
			Permission.EXPORT_DAMAGE_CONTRIBUTIONS, Permission.EXPORT_AIRCRAFT_MODEL, Permission.PLOT_TYPICAL_FLIGHT, Permission.PLOT_MISSION_PROFILE,
			Permission.PLOT_LEVEL_CROSSINGS, Permission.GENERATE_LIFE_FACTORS, Permission.REMOVE_PLUGIN, Permission.PLOT_TYPICAL_FLIGHT_STATISTICS,
			Permission.PLOT_EQUIVALENT_STRESS_COMPARISON, Permission.PLOT_TYPICAL_FLIGHT_COMPARISON, Permission.PLOT_STRESS_SEQUENCE_COMPARISON,
			Permission.PLOT_LOADCASE_COMPARISON, Permission.PLOT_ELEMENT_STRESS_COMPARISON, Permission.PLOT_ELEMENT_STRESSES,
			Permission.GENERATE_EQUIVALENT_STRESS_RATIOS, Permission.SUBMIT_ACCESS_REQUEST);

	// @formatter:on

	/** Permissions. */
	private final Permission[] permissions;

	/**
	 * Creates user profile.
	 *
	 * @param permissions
	 *            User permissions.
	 */
	UserProfile(Permission... permissions) {
		this.permissions = permissions;
	}

	/**
	 * Returns the permissions of this profile.
	 *
	 * @return The permissions.
	 */
	public Permission[] getPermissions() {
		return permissions;
	}
}
