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

import equinox.font.IconicFont;
import javafx.scene.control.Label;

/**
 * Class for file filter item.
 *
 * @author Murat Artim
 * @date May 7, 2016
 * @time 12:04:18 AM
 */
public class FilterItem {

	/** Name of filter item. */
	private final String name_;

	/** File class of filter item. */
	private final Class<?> fileClass_;

	/** Icon label of filter item. */
	private final Label iconLabel_;

	/**
	 * Creates file filter item.
	 *
	 * @param name
	 *            Name of filter.
	 * @param imageName
	 *            Image name of filter.
	 * @param font
	 *            Icon font.
	 * @param fileClass
	 *            File class of filter.
	 */
	public FilterItem(String name, String imageName, IconicFont font, Class<?> fileClass) {
		name_ = name;
		fileClass_ = fileClass;
		iconLabel_ = new Label(imageName);
		iconLabel_.getStylesheets().add(font.getStyleSheet());
	}

	/**
	 * Returns name of file filter.
	 *
	 * @return Name of file filter.
	 */
	public String getName() {
		return name_;
	}

	/**
	 * Returns icon of file filter.
	 *
	 * @return Icon of file filter.
	 */
	public Label getIcon() {
		return iconLabel_;
	}

	/**
	 * Returns file class of file filter.
	 *
	 * @return File class of file filter.
	 */
	public Class<?> getFileClass() {
		return fileClass_;
	}
}
