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
 * Class for pair data structure.
 *
 * @author Murat Artim
 * @param <V>
 *            Class for first element.
 * @param <T>
 *            Class for second element.
 * @date 22 Aug 2018
 * @time 13:09:33
 */
public class Pair<V, T> implements Serializable {

	/** Serial id. */
	private static final long serialVersionUID = 1L;

	/** First element. */
	private V element1;

	/** Second element. */
	private T element2;

	/**
	 * Creates a pair with empty content.
	 */
	public Pair() {
	}

	/**
	 * Creates a pair with given elements.
	 *
	 * @param element1
	 *            First element.
	 * @param element2
	 *            Second element.
	 */
	public Pair(V element1, T element2) {
		this.element1 = element1;
		this.element2 = element2;
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
}