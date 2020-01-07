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

/**
 * Class for spectrum plot series key.
 *
 * @author Murat Artim
 * @date Sep 9, 2014
 * @time 4:03:03 PM
 */
public class SeriesKey implements Comparable<SeriesKey> {

	/** Name of series. */
	private final String name_;

	/** Flight ID. */
	private final Integer id_;

	/**
	 * Creates series key.
	 *
	 * @param name
	 *            Name of series.
	 * @param id
	 *            Flight ID.
	 */
	public SeriesKey(String name, int id) {
		name_ = name;
		id_ = id;
	}

	/**
	 * Returns the name of series.
	 *
	 * @return The of series.
	 */
	public String getName() {
		return name_;
	}

	/**
	 * Returns the flight ID of series.
	 *
	 * @return Flight ID.
	 */
	public int getID() {
		return id_;
	}

	@Override
	public String toString() {
		return name_;
	}

	@Override
	public int compareTo(SeriesKey o) {
		int comp1 = name_.compareTo(o.name_);
		if (comp1 == 0)
			return id_.compareTo(o.id_);
		return comp1;
	}
}
