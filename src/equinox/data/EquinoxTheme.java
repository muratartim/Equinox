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

import java.net.URL;

import equinox.Equinox;
import equinox.data.ProgramArguments.ArgumentType;

/**
 * Enumeration for Equinox color themes.
 *
 * @author Murat Artim
 * @date 1 Sep 2017
 * @time 11:07:40
 *
 */
public enum EquinoxTheme {

	/** Color theme. */
	STEELBLUE("steelblue", "steelblue", "fxml/", "css/"), SLATEGRAY("slategray", "slategray", "fxml/slategray/", "css/slategray/"), MIDNIGHTBLUE("midnightblue", "#2c3e50", "fxml/midnightblue/", "css/midnightblue/");

	/** Theme attribute. */
	private final String name, color, fxmlDir, cssDir;

	/**
	 * Creates Equinox color theme enumeration.
	 *
	 * @param name
	 *            Name of theme.
	 * @param color
	 *            Color name of theme.
	 * @param fxmlDir
	 *            Relative path to FXML resource directory.
	 * @param cssDir
	 *            Relative path to CSS resource directory.
	 */
	private EquinoxTheme(String name, String color, String fxmlDir, String cssDir) {
		this.name = name;
		this.color = color;
		this.fxmlDir = fxmlDir;
		this.cssDir = cssDir;
	}

	/**
	 * Returns name of theme.
	 *
	 * @return Name of theme.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns color name of theme.
	 *
	 * @return The color name.
	 */
	public String getColor() {
		return color;
	}

	/**
	 * Returns the relative path to FXML resource directory.
	 *
	 * @return Relative path to FXML resource directory.
	 */
	public String getFxmlDir() {
		return fxmlDir;
	}

	/**
	 * Returns the relative path to CSS resource directory.
	 *
	 * @return Relative path to CSS resource directory.
	 */
	public String getCssDir() {
		return cssDir;
	}

	/**
	 * Returns the default color theme.
	 *
	 * @return The default color theme.
	 */
	public static EquinoxTheme getDefault() {
		return EquinoxTheme.STEELBLUE;
	}

	/**
	 * Returns the color theme for the given name, or the default theme if name not known.
	 *
	 * @param name
	 *            Name of the theme.
	 * @return The color theme for the given name, or the default theme if name not known.
	 */
	public static EquinoxTheme forName(String name) {
		for (EquinoxTheme theme : EquinoxTheme.values()) {
			if (theme.getName().equals(name))
				return theme;
		}
		return getDefault();
	}

	/**
	 * Returns URL to FXML resource.
	 *
	 * @param fileName
	 *            FXML file name.
	 * @return URL to FXML resource.
	 */
	public static URL getFXMLResource(String fileName) {

		// get current theme
		EquinoxTheme theme = forName(Equinox.ARGUMENTS.getArgument(ArgumentType.COLOR_THEME));

		// return resource URL
		return Equinox.class.getResource(theme.getFxmlDir() + fileName);
	}
}
