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
package equinox.data.ui;

import java.util.Date;

/**
 * Class for saved task item.
 *
 * @author Murat Artim
 * @date Oct 8, 2015
 * @time 5:53:19 PM
 */
public class SavedTaskItem implements Comparable<SavedTaskItem> {

	/** Task ID. */
	private final int taskID_;

	/** Task title. */
	private final String title_;

	/** Schedule date. */
	private Date date_;

	/**
	 * Creates saved task item.
	 *
	 * @param taskID
	 *            Task ID.
	 * @param title
	 *            Task title.
	 * @param date
	 *            Schedule date (can be null).
	 */
	public SavedTaskItem(int taskID, String title, Date date) {
		taskID_ = taskID;
		title_ = title;
		date_ = date;
	}

	/**
	 * Sets schedule date.
	 *
	 * @param date
	 *            Schedule date.
	 */
	public void setDate(Date date) {
		date_ = date;
	}

	/**
	 * Returns task ID.
	 *
	 * @return Task ID.
	 */
	public int getTaskID() {
		return this.taskID_;
	}

	/**
	 * Returns title of task.
	 *
	 * @return Title of task.
	 */
	public String getTitle() {
		return title_;
	}

	/**
	 * Returns schedule date of task.
	 *
	 * @return Schedule date of task.
	 */
	public Date getDate() {
		return date_;
	}

	@Override
	public String toString() {
		return title_ + (date_ == null ? "" : " (" + date_.toString() + ")");
	}

	@Override
	public int compareTo(SavedTaskItem o) {
		return this.date_.compareTo(o.getDate());
	}
}
