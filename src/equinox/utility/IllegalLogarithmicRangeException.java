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

/**
 * Exception to be thrown when a bound value isn't supported by the logarithmic axis<br>
 * <br>
 *
 * @author Kevin Senechal mailto: kevin.senechal@dooapp.com
 *
 */
public class IllegalLogarithmicRangeException extends Exception {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Creates illegal logarithmic range exception.
	 *
	 * @param message
	 *            Exception message.
	 */
	public IllegalLogarithmicRangeException(String message) {
		super(message);
	}
}
