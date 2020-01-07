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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;

/**
 * Utility class for extracting font awesome property file.
 *
 * @author Murat Artim
 * @date Nov 19, 2015
 * @time 11:52:08 AM
 */
public class ExtractFontAwesomeProperties {

	/**
	 * Extracts font awesome property file.
	 *
	 * @param args
	 *            Not used.
	 * @throws IOException
	 *             If exception occurs during process.
	 */
	public static void main(String[] args) throws IOException {

		// input css file
		Path css = Paths.get("/Users/aurora/Documents/Developer/Workspaces/EclipseWorkspace/Equinox/src/equinox/font/fontawesome.keys");
		Path properties = Paths.get("/Users/aurora/Documents/Developer/Workspaces/EclipseWorkspace/Equinox/src/equinox/font/fontawesome.properties");

		// create file reader
		try (BufferedReader reader = Files.newBufferedReader(css, Charset.defaultCharset())) {

			// create output file writer
			try (BufferedWriter writer = Files.newBufferedWriter(properties, Charset.defaultCharset())) {

				// read file till the end
				String line;
				while ((line = reader.readLine()) != null) {

					// empty line
					if (line.trim().isEmpty())
						continue;

					// not icon
					if (!line.startsWith(".fa-") || !line.contains(":"))
						continue;

					// get icon name
					String name = line.split(":")[0].trim();
					name = name.replaceFirst("\\.", "");
					name = name.replaceFirst("-", ".");

					// read till content found
					while (!line.contains("content:")) {
						line = reader.readLine();
						if (line == null)
							return;
					}

					// get content
					String content = line.split(":")[1].trim();
					content = content.replaceFirst(";", "");
					content = content.replaceAll("\"", "");
					content = content.replaceFirst(Matcher.quoteReplacement("\\"), "\\u");

					// write out
					writer.write(name + "=\\" + content);
					writer.newLine();
				}
			}
		}
	}
}
