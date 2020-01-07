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
package equinox.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Class for settings.
 *
 * @author Murat Artim
 * @date Apr 30, 2014
 * @time 1:27:59 PM
 */
public class Settings implements Serializable {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Setting index. */
	public static final int NOTIFY_EQUINOX_UPDATES = 0, NOTIFY_PLUGIN_UPDATES = 1, NOTIFY_MATERIAL_UPDATES = 2, LIKES = 3, ANALYSIS_SERVER_HOSTNAME = 4, ANALYSIS_SERVER_PORT = 5, WEB_HOSTNAME = 6, WEB_PORT = 7, NOTIFY_MESSAGES = 8, NOTIFY_FILES = 9, NOTIFY_ERRORS = 10, NOTIFY_WARNINGS = 11,
			NOTIFY_INFO = 12, NOTIFY_QUEUED = 13, NOTIFY_SUBMITTED = 14, NOTIFY_SUCCEEDED = 15, USE_SYSTEMTRAY = 16, ANALYSIS_ENGINE = 17, ISAMI_SUB_VERSION = 18, FALLBACK_TO_INBUILT = 19, APPLY_COMPRESSION = 20, NOTIFY_SAVED = 21, NOTIFY_SCHEDULED = 22, SHOW_NOTIFY_FROM_BOTTOM = 23,
			KEEP_ANALYSIS_OUTPUTS = 24, DETAILED_ANALYSIS = 25, SHOW_NEWSFEED = 26, ISAMI_VERSION = 27, FILER_USERNAME = 28, FILER_HOSTNAME = 29, FILER_PORT = 30, FILER_PASSWORD = 31, FILER_ROOT_PATH = 32, WEB_PATH = 33, EXCHANGE_SERVER_HOSTNAME = 34, EXCHANGE_SERVER_PORT = 35,
			DATA_SERVER_HOSTNAME = 36, DATA_SERVER_PORT = 37;

	/** Settings. */
	private final HashMap<Integer, Setting> settings_;

	/**
	 * Creates settings.
	 */
	public Settings() {
		settings_ = new HashMap<>();
		setDefaultValues();
	}

	/**
	 * Returns the value of the setting at the given index.
	 *
	 * @param index
	 *            Index of the demanded setting.
	 * @return The value of the setting.
	 */
	public Object getValue(int index) {
		return settings_.get(index).getValue();
	}

	/**
	 * Returns true if the given wish ID is already liked.
	 *
	 * @param wishID
	 *            Wish ID to check.
	 * @return True if the given wish ID is already liked.
	 */
	@SuppressWarnings("unchecked")
	public boolean isWishLiked(long wishID) {
		Setting setting = settings_.get(LIKES);
		ArrayList<Long> likes = (ArrayList<Long>) setting.getValue();
		for (long id : likes)
			if (id == wishID)
				return true;
		return false;
	}

	/**
	 * Sets the value of the setting at the given index.
	 *
	 * @param index
	 *            Index of the setting to set.
	 * @param value
	 *            The value to set.
	 * @return True if application restart is required.
	 */
	public boolean setValue(int index, Object value) {
		return settings_.get(index).setValue(value);
	}

	/**
	 * Adds given wish ID to liked wishes.
	 *
	 * @param wishID
	 *            Wish ID to add.
	 */
	@SuppressWarnings("unchecked")
	public void addToLikedWishes(long wishID) {
		Setting setting = settings_.get(LIKES);
		ArrayList<Long> likes = (ArrayList<Long>) setting.getValue();
		likes.add(wishID);
	}

	/**
	 * Returns all settings mapping. Note that this method is meant to be called for encryption.
	 *
	 * @return All settings.
	 */
	public HashMap<Integer, Setting> getSettings() {
		return settings_;
	}

	/**
	 * Puts setting into settings mapping. Note that this method is meant to be called for descryption.
	 *
	 * @param index
	 *            Index of setting.
	 * @param setting
	 *            Setting.
	 */
	public void putSetting(int index, Setting setting) {
		settings_.put(index, setting);
	}

	/**
	 * Sets default values of settings.
	 */
	private void setDefaultValues() {
		settings_.put(Settings.NOTIFY_EQUINOX_UPDATES, new Setting(true, false));
		settings_.put(Settings.NOTIFY_PLUGIN_UPDATES, new Setting(true, false));
		settings_.put(Settings.NOTIFY_MATERIAL_UPDATES, new Setting(true, false));
		settings_.put(Settings.LIKES, new Setting(new ArrayList<Long>(), false));
		settings_.put(Settings.EXCHANGE_SERVER_HOSTNAME, new Setting("exchange-service.equinox-digital-twin.com", true));
		settings_.put(Settings.EXCHANGE_SERVER_PORT, new Setting("1234", true));
		settings_.put(Settings.DATA_SERVER_HOSTNAME, new Setting("data-service.equinox-digital-twin.com", true));
		settings_.put(Settings.DATA_SERVER_PORT, new Setting("1235", true));
		settings_.put(Settings.ANALYSIS_SERVER_HOSTNAME, new Setting("analysis-service.equinox-digital-twin.com", true));
		settings_.put(Settings.ANALYSIS_SERVER_PORT, new Setting("1236", true));
		settings_.put(Settings.WEB_HOSTNAME, new Setting("http://www.equinox-digital-twin.com", true));
		settings_.put(Settings.WEB_PORT, new Setting("80", true));
		settings_.put(Settings.WEB_PATH, new Setting("/files/", false));
		settings_.put(Settings.NOTIFY_MESSAGES, new Setting(true, false));
		settings_.put(Settings.NOTIFY_FILES, new Setting(true, false));
		settings_.put(Settings.NOTIFY_ERRORS, new Setting(true, false));
		settings_.put(Settings.NOTIFY_WARNINGS, new Setting(true, false));
		settings_.put(Settings.NOTIFY_INFO, new Setting(true, false));
		settings_.put(Settings.NOTIFY_QUEUED, new Setting(true, false));
		settings_.put(Settings.NOTIFY_SUBMITTED, new Setting(true, false));
		settings_.put(Settings.NOTIFY_SUCCEEDED, new Setting(true, false));
		settings_.put(Settings.USE_SYSTEMTRAY, new Setting(true, false));
		settings_.put(Settings.ANALYSIS_ENGINE, new Setting(AnalysisEngine.INBUILT, false));
		settings_.put(Settings.ISAMI_SUB_VERSION, new Setting(IsamiSubVersion.DERIVATIVES, false));
		settings_.put(Settings.FALLBACK_TO_INBUILT, new Setting(true, false));
		settings_.put(Settings.APPLY_COMPRESSION, new Setting(true, false));
		settings_.put(Settings.NOTIFY_SAVED, new Setting(true, false));
		settings_.put(Settings.NOTIFY_SCHEDULED, new Setting(true, false));
		settings_.put(Settings.SHOW_NOTIFY_FROM_BOTTOM, new Setting(true, false));
		settings_.put(Settings.KEEP_ANALYSIS_OUTPUTS, new Setting(true, false));
		settings_.put(Settings.DETAILED_ANALYSIS, new Setting(false, false));
		settings_.put(Settings.SHOW_NEWSFEED, new Setting(true, false));
		settings_.put(Settings.ISAMI_VERSION, new Setting(IsamiVersion.DEFAULT_VERSION, false));
		settings_.put(Settings.FILER_ROOT_PATH, new Setting("filerRoot/", false));
		settings_.put(Settings.FILER_HOSTNAME, new Setting("file-server.equinox-digital-twin.com", false));
		settings_.put(Settings.FILER_USERNAME, new Setting("aurora", false));
		settings_.put(Settings.FILER_PASSWORD, new Setting("17891917", false));
		settings_.put(Settings.FILER_PORT, new Setting("2222", false));
	}
}
