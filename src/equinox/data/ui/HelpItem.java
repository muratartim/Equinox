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
import javafx.scene.control.TreeItem;

/**
 * Class for help item.
 *
 * @author Murat Artim
 * @date May 11, 2014
 * @time 9:00:34 AM
 */
public class HelpItem extends TreeItem<String> {

	/** Page name and location. */
	private final String page_, location_;

	/** Icon. */
	private final Label icon_ = new Label("\ue994");

	/**
	 * Creates spectrum file.
	 *
	 * @param page
	 *            Help page name.
	 * @param location
	 *            Location within the page. Can be null if page start.
	 */
	public HelpItem(String page, String location) {
		page_ = page;
		setValue(page_);
		location_ = location;
		icon_.getStylesheets().add(IconicFont.ICOMOON.getStyleSheet());
	}

	/**
	 * Returns the page of this item.
	 *
	 * @return Page.
	 */
	public String getPage() {
		return page_;
	}

	/**
	 * Returns the location of this item.
	 *
	 * @return Location.
	 */
	public String getLocation() {
		return location_;
	}

	@Override
	public String toString() {
		return location_ == null ? page_ : location_;
	}

	/**
	 * Returns the icon.
	 *
	 * @return The icon.
	 */
	public Label getIcon() {
		return icon_;
	}
}
