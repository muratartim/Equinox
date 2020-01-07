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
package equinox.data.fileType;

import equinox.font.IconicFont;
import javafx.scene.control.TreeItem;

/**
 * Abstract class for spectrum item. A spectrum item is a node that is displayed in the spectrum items tree view.
 *
 * @author Murat Artim
 * @date Apr 9, 2014
 * @time 9:32:53 AM
 */
public abstract class SpectrumItem extends TreeItem<String> {

	/** ID. */
	private final int id_;

	/**
	 * Creates spectrum file.
	 *
	 * @param name
	 *            Name.
	 * @param id
	 *            ID.
	 */
	public SpectrumItem(String name, int id) {

		// set name
		setValue(name);

		// set id
		id_ = id;
	}

	/**
	 * Renames this item.
	 *
	 * @param name
	 *            The new name.
	 */
	public void setName(String name) {
		setValue(name);
	}

	/**
	 * Returns name.
	 *
	 * @return Name.
	 */
	public String getName() {
		return getValue();
	}

	/**
	 * Returns ID.
	 *
	 * @return ID.
	 */
	public int getID() {
		return id_;
	}

	@Override
	public String toString() {
		return getValue();
	}

	/**
	 * Returns the parent spectrum item of this item.
	 *
	 * @return The parent spectrum item of this item.
	 */
	public abstract SpectrumItem getParentItem();

	/**
	 * Returns the icon font of this spectrum item.
	 *
	 * @return The icon font of this spectrum item.
	 */
	public abstract IconicFont getIconFont();
}
