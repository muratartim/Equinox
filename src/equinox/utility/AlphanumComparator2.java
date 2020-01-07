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
package equinox.utility;

import java.util.Comparator;

/**
 * This is an updated version with enhancements made by Daniel Migowski, Andre Bogus, and David Koelle.
 *
 * @author Murat Artim
 * @date Feb 27, 2015
 * @time 1:03:06 PM
 */
public class AlphanumComparator2 implements Comparator<Object> {

	@Override
	public int compare(Object o1, Object o2) {

		// to string
		String s1 = o1.toString();
		String s2 = o2.toString();

		int thisMarker = 0;
		int thatMarker = 0;
		int s1Length = s1.length();
		int s2Length = s2.length();

		while (thisMarker < s1Length && thatMarker < s2Length) {
			String thisChunk = getChunk(s1, s1Length, thisMarker);
			thisMarker += thisChunk.length();

			String thatChunk = getChunk(s2, s2Length, thatMarker);
			thatMarker += thatChunk.length();

			// If both chunks contain numeric characters, sort them numerically
			int result = 0;
			if (isDigit(thisChunk.charAt(0)) && isDigit(thatChunk.charAt(0))) {
				// Simple chunk comparison by length.
				int thisChunkLength = thisChunk.length();
				result = thisChunkLength - thatChunk.length();
				// If equal, the first different number counts
				if (result == 0) {
					for (int i = 0; i < thisChunkLength; i++) {
						result = thisChunk.charAt(i) - thatChunk.charAt(i);
						if (result != 0) {
							return result;
						}
					}
				}
			}
			else {
				result = thisChunk.compareTo(thatChunk);
			}

			if (result != 0)
				return result;
		}

		return s1Length - s2Length;
	}

	/**
	 * Returns true if character is digit.
	 *
	 * @param ch
	 *            Character.
	 * @return True if character is digit.
	 */
	private final static boolean isDigit(char ch) {
		return ch >= 48 && ch <= 57;
	}

	/**
	 * Returns a chunk from the given string.
	 *
	 * @param s
	 *            String.
	 * @param slength
	 *            Length of string is passed in for improved efficiency (only need to calculate it once).
	 * @param marker
	 *            Marker.
	 * @return A chunk from the given string.
	 */
	private final static String getChunk(String s, int slength, int marker) {
		StringBuilder chunk = new StringBuilder();
		char c = s.charAt(marker);
		chunk.append(c);
		marker++;
		if (isDigit(c)) {
			while (marker < slength) {
				c = s.charAt(marker);
				if (!isDigit(c))
					break;
				chunk.append(c);
				marker++;
			}
		}
		else {
			while (marker < slength) {
				c = s.charAt(marker);
				if (isDigit(c))
					break;
				chunk.append(c);
				marker++;
			}
		}
		return chunk.toString();
	}
}
