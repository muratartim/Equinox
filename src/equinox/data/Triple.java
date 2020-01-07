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

import java.io.Serializable;

/**
 * Class for triple data structure.
 *
 * @author Murat Artim
 * @date 25 Aug 2018
 * @time 11:18:31
 * @param <V>
 *            Class for first element.
 * @param <T>
 *            Class for second element.
 * @param <Z>
 *            Class for third element.
 */
public class Triple<V, T, Z> implements Serializable {

	/** Serial id. */
	private static final long serialVersionUID = 1L;

	/** First element. */
	private V element1;

	/** Second element. */
	private T element2;

	/** Third element. */
	private Z element3;

	/**
	 * Creates a triple with empty content.
	 */
	public Triple() {
	}

	/**
	 * Creates a triple with given elements.
	 *
	 * @param element1
	 *            First element.
	 * @param element2
	 *            Second element.
	 * @param element3
	 *            Third element.
	 */
	public Triple(V element1, T element2, Z element3) {
		this.element1 = element1;
		this.element2 = element2;
		this.element3 = element3;
	}

	/**
	 * Returns first element.
	 *
	 * @return First element.
	 */
	public V getElement1() {
		return element1;
	}

	/**
	 * Returns second element.
	 *
	 * @return Second element.
	 */
	public T getElement2() {
		return element2;
	}

	/**
	 * Returns third element.
	 *
	 * @return Third element.
	 */
	public Z getElement3() {
		return element3;
	}

	/**
	 * Sets first element.
	 *
	 * @param element1
	 *            First element.
	 */
	public void setElement1(V element1) {
		this.element1 = element1;
	}

	/**
	 * Sets second element.
	 *
	 * @param element2
	 *            Second element.
	 */
	public void setElement2(T element2) {
		this.element2 = element2;
	}

	/**
	 * Sets third element.
	 *
	 * @param element3
	 *            Third element.
	 */
	public void setElement3(Z element3) {
		this.element3 = element3;
	}
}