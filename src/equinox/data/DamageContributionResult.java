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

/**
 * Class for damage contribution result.
 *
 * @author Murat Artim
 * @date Jun 5, 2015
 * @time 12:06:36 PM
 */
public class DamageContributionResult {

	/** Fatigue damage and equivalent stress. */
	private Double damage_, eqStress_;

	/** Contribution index. */
	private int contributionIndex_ = -1;

	/**
	 * Sets contribution index.
	 *
	 * @param index
	 *            Contribution index.
	 */
	public void setContributionIndex(int index) {
		contributionIndex_ = index;
	}

	/**
	 * Sets damage.
	 *
	 * @param damage
	 *            Damage.
	 */
	public void setDamage(Double damage) {
		damage_ = damage;
	}

	/**
	 * Sets equivalent stress.
	 *
	 * @param eqStress
	 *            Equivalent stress.
	 */
	public void setStress(Double eqStress) {
		eqStress_ = eqStress;
	}

	/**
	 * Returns contribution index.
	 *
	 * @return Contribution index.
	 */
	public int getContributionIndex() {
		return contributionIndex_;
	}

	/**
	 * Returns damage.
	 *
	 * @return Damage.
	 */
	public Double getDamage() {
		return damage_;
	}

	/**
	 * Returns equivalent stress.
	 *
	 * @return Equivalent stress.
	 */
	public Double getStress() {
		return eqStress_;
	}
}
