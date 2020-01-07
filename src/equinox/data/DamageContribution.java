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

import equinox.dataServer.remote.data.ContributionType;

/**
 * Class for damage contribution data.
 *
 * @author Murat Artim
 * @date Apr 1, 2015
 * @time 5:15:56 PM
 */
public class DamageContribution implements Serializable {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Damage contribution type. */
	private final ContributionType type_;

	/** Name of contribution. */
	private final String name_;

	/** Loadcase factors of contribution. */
	private final ArrayList<LoadcaseFactor> loadcaseFactors_;

	/**
	 * Creates damage contribution.
	 *
	 * @param name
	 *            Name of contribution.
	 * @param loadcaseFactors
	 *            Loadcase factors of contribution. Only required for increment type damage contributions, null should be given otherwise.
	 * @param type
	 *            Damage contribution type.
	 */
	public DamageContribution(String name, ArrayList<LoadcaseFactor> loadcaseFactors, ContributionType type) {
		name_ = name;
		loadcaseFactors_ = loadcaseFactors;
		type_ = type;
	}

	/**
	 * Returns type of damage contribution.
	 *
	 * @return Type of damage contribution.
	 */
	public ContributionType getType() {
		return type_;
	}

	/**
	 * Returns name of contribution.
	 *
	 * @return Name of contribution.
	 */
	public String getName() {
		return toString();
	}

	/**
	 * Returns loadcase factors of contribution.
	 *
	 * @return Loadcase factors of contribution.
	 */
	public ArrayList<LoadcaseFactor> getLoadcaseFactors() {
		return loadcaseFactors_;
	}

	@Override
	public String toString() {
		return name_;
	}
}
