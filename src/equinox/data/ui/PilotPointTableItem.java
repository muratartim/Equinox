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

import javafx.beans.property.SimpleStringProperty;

/**
 * Class for pilot point table item.
 *
 * @author Murat Artim
 * @date Aug 27, 2015
 * @time 3:42:09 PM
 */
public class PilotPointTableItem {

	/** STF file ID. */
	private final int stfID_;

	/** STF file attributes. */
	private final SimpleStringProperty stfname = new SimpleStringProperty(), program = new SimpleStringProperty(),
			section = new SimpleStringProperty(), mission = new SimpleStringProperty(), eid = new SimpleStringProperty();

	/**
	 * Creates pilot point table item.
	 *
	 * @param stfID
	 *            STF file ID.
	 */
	public PilotPointTableItem(int stfID) {
		stfID_ = stfID;
	}

	/**
	 * Returns STF file ID.
	 *
	 * @return STF file ID.
	 */
	public int getSTFFileID() {
		return stfID_;
	}

	/**
	 * Returns STF file name.
	 *
	 * @return STF file name.
	 */
	public String getStfname() {
		return this.stfname.get();
	}

	/**
	 * Returns A/C program.
	 *
	 * @return A/C program.
	 */
	public String getProgram() {
		return this.program.get();
	}

	/**
	 * Returns A/C section.
	 *
	 * @return A/C section.
	 */
	public String getSection() {
		return this.section.get();
	}

	/**
	 * Returns fatigue mission.
	 *
	 * @return Fatigue mission.
	 */
	public String getMission() {
		return this.mission.get();
	}

	/**
	 * Returns element ID.
	 *
	 * @return Element ID.
	 */
	public String getEid() {
		return this.eid.get();
	}

	/**
	 * Sets STF file name.
	 *
	 * @param stfname
	 *            STF file name.
	 */
	public void setStfname(String stfname) {
		this.stfname.set(stfname);
	}

	/**
	 * Sets A/C program.
	 *
	 * @param program
	 *            A/C program.
	 */
	public void setProgram(String program) {
		this.program.set(program);
	}

	/**
	 * Sets A/C section.
	 *
	 * @param section
	 *            A/C section.
	 */
	public void setSection(String section) {
		this.section.set(section);
	}

	/**
	 * Sets fatigue mission.
	 *
	 * @param mission
	 *            Fatigue mission.
	 */
	public void setMission(String mission) {
		this.mission.set(mission);
	}

	/**
	 * Sets element ID.
	 *
	 * @param eid
	 *            Element ID.
	 */
	public void setEid(String eid) {
		this.eid.set(eid);
	}
}
