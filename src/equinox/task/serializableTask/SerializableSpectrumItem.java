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
package equinox.task.serializableTask;

import java.io.Serializable;

import equinox.data.fileType.SpectrumItem;

/**
 * Class for serializable spectrum item.
 *
 * @author Murat Artim
 * @date Oct 8, 2015
 * @time 10:14:35 AM
 */
public class SerializableSpectrumItem implements Serializable {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** ID of spectrum item. */
	private final int id_;

	/** Simple class name of spectrum item. */
	private final String simpleClassName_;

	/**
	 * Creates serializable spectrum item.
	 *
	 * @param spectrumItem
	 *            Spectrum item.
	 */
	public SerializableSpectrumItem(SpectrumItem spectrumItem) {
		simpleClassName_ = spectrumItem.getClass().getSimpleName();
		id_ = spectrumItem.getID();
	}

	/**
	 * Returns simple class name.
	 *
	 * @return Simple class name.
	 */
	public String getSimpleClassName() {
		return simpleClassName_;
	}

	/**
	 * Returns spectrum item ID.
	 *
	 * @return Spectrum item ID.
	 */
	public int getID() {
		return id_;
	}

	/**
	 * Returns true if this is the serializable form of the given spectrum item.
	 *
	 * @param spectrumItem
	 *            Spectrum item to check.
	 * @return True if this is the serializable form of the given spectrum item.
	 */
	public boolean equals(SpectrumItem spectrumItem) {
		return simpleClassName_.equals(spectrumItem.getClass().getSimpleName()) && id_ == spectrumItem.getID();
	}
}
