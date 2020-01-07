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
package equinox.font;

import java.util.ResourceBundle;

import equinox.Equinox;

/**
 * Enumeration for iconic fonts.
 *
 * @author Murat Artim
 * @date Nov 20, 2015
 * @time 7:52:18 PM
 */
public enum IconicFont {

	/** Iconic font. */
	FONTAWESOME(Equinox.class.getResource("css/FontAwesomeLabel.css").toString(), Equinox.class.getResource("css/FontAwesomeLabelWhite.css").toString()), ICOMOON(Equinox.class.getResource("css/IcoMoonLabel.css").toString(), Equinox.class.getResource("css/IcoMoonLabelWhite.css").toString()),
	CUSTOM(Equinox.class.getResource("css/CustomFontLabel.css").toString(), Equinox.class.getResource("css/CustomFontLabelWhite.css").toString());

	/** Resource bundle to iconic font keys. */
	public static final ResourceBundle FONT_KEYS = ResourceBundle.getBundle("equinox/font/iconicfont");

	/** Style sheet URL. */
	private final String styleSheet_, whiteStyleSheet_;

	/**
	 * Creates iconic font constant.
	 *
	 * @param styleSheet
	 *            Style sheet URL.
	 * @param whiteStyleSheet
	 *            White style sheet URL.
	 */
	IconicFont(String styleSheet, String whiteStyleSheet) {
		styleSheet_ = styleSheet;
		whiteStyleSheet_ = whiteStyleSheet;
	}

	/**
	 * Returns style sheet URL.
	 *
	 * @return Style sheet URL.
	 */
	public String getStyleSheet() {
		return styleSheet_;
	}

	/**
	 * Returns white style sheet URL.
	 *
	 * @return White style sheet URL.
	 */
	public String getWhiteStyleSheet() {
		return whiteStyleSheet_;
	}
}
