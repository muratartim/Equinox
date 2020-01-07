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
package equinox.data.material;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import equinox.dataServer.remote.data.FatigueMaterial;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * Class for fatigue material table item.
 *
 * @author Murat Artim
 * @date Nov 29, 2015
 * @time 1:05:46 PM
 */
public class FatigueMaterialItem {

	/** String attributes. */
	private final SimpleStringProperty name = new SimpleStringProperty(), specification = new SimpleStringProperty(), libraryVersion = new SimpleStringProperty(), family = new SimpleStringProperty(), orientation = new SimpleStringProperty(), configuration = new SimpleStringProperty(),
			isamiVersion = new SimpleStringProperty();

	/** Double attributes. */
	private final SimpleDoubleProperty p = new SimpleDoubleProperty(), q = new SimpleDoubleProperty(), M = new SimpleDoubleProperty();

	/** Material ID. */
	private final int id_;

	/**
	 * Creates fatigue material item.
	 *
	 * @param id
	 *            Material ID.
	 */
	public FatigueMaterialItem(int id) {
		id_ = id;
	}

	/**
	 * Returns material ID.
	 *
	 * @return Material ID.
	 */
	public int getID() {
		return id_;
	}

	/**
	 * Returns name.
	 *
	 * @return Name.
	 */
	public String getName() {
		return name.get();
	}

	/**
	 * Returns specification.
	 *
	 * @return Specification.
	 */
	public String getSpecification() {
		return specification.get();
	}

	/**
	 * Returns material library version.
	 *
	 * @return Material library version.
	 */
	public String getLibraryVersion() {
		return libraryVersion.get();
	}

	/**
	 * Returns material ISAMI version.
	 *
	 * @return Material ISAMI version.
	 */
	public String getIsamiVersion() {
		return isamiVersion.get();
	}

	/**
	 * Returns material family.
	 *
	 * @return Material family.
	 */
	public String getFamily() {
		return family.get();
	}

	/**
	 * Returns orientation.
	 *
	 * @return Orientation.
	 */
	public String getOrientation() {
		return orientation.get();
	}

	/**
	 * Returns configuration.
	 *
	 * @return Configuration.
	 */
	public String getConfiguration() {
		return configuration.get();
	}

	/**
	 * Returns p.
	 *
	 * @return P.
	 */
	public double getP() {
		return p.get();
	}

	/**
	 * Returns q.
	 *
	 * @return Q.
	 */
	public double getQ() {
		return q.get();
	}

	/**
	 * Returns M.
	 *
	 * @return M.
	 */
	public double getM() {
		return M.get();
	}

	/**
	 * Returns search string for the material.
	 *
	 * @return Search string for the material.
	 */
	public String getSearchString() {
		String string = getName() + "-";
		string += getSpecification() + "-";
		string += getFamily() + "-";
		string += getOrientation() + "-";
		string += getConfiguration();
		return string;
	}

	/**
	 * Sets material name.
	 *
	 * @param name
	 *            Material name.
	 */
	public void setName(String name) {
		this.name.set(name);
	}

	/**
	 * Sets specification.
	 *
	 * @param specification
	 *            Specification.
	 */
	public void setSpecification(String specification) {
		this.specification.set(specification);
	}

	/**
	 * Sets material library version.
	 *
	 * @param libraryVersion
	 *            Material library version.
	 */
	public void setLibraryVersion(String libraryVersion) {
		this.libraryVersion.set(libraryVersion);
	}

	/**
	 * Sets material ISAMI version.
	 *
	 * @param isamiVersion
	 *            Material ISAMI version.
	 */
	public void setIsamiVersion(String isamiVersion) {
		this.isamiVersion.set(isamiVersion);
	}

	/**
	 * Sets material family.
	 *
	 * @param family
	 *            Material family.
	 */
	public void setFamily(String family) {
		this.family.set(family);
	}

	/**
	 * Sets orientation.
	 *
	 * @param orientation
	 *            Orientation.
	 */
	public void setOrientation(String orientation) {
		this.orientation.set(orientation);
	}

	/**
	 * Sets configuration.
	 *
	 * @param configuration
	 *            Configuration.
	 */
	public void setConfiguration(String configuration) {
		this.configuration.set(configuration);
	}

	/**
	 * Sets p.
	 *
	 * @param p
	 *            P.
	 */
	public void setP(double p) {
		this.p.set(p);
	}

	/**
	 * Sets q.
	 *
	 * @param q
	 *            Q.
	 */
	public void setQ(double q) {
		this.q.set(q);
	}

	/**
	 * Sets M.
	 *
	 * @param M
	 *            M.
	 */
	public void setM(double M) {
		this.M.set(M);
	}

	/**
	 * Creates and returns fatigue material to be used for analysis.
	 *
	 * @return Fatigue material to be used for analysis.
	 */
	public FatigueMaterial getMaterial() {
		FatigueMaterial material = new FatigueMaterial(id_);
		material.setName(getName());
		material.setSpecification(getSpecification());
		material.setLibraryVersion(getLibraryVersion());
		material.setFamily(getFamily());
		material.setOrientation(getOrientation());
		material.setConfiguration(getConfiguration());
		material.setP(getP());
		material.setQ(getQ());
		material.setM(getM());
		material.setIsamiVersion(getIsamiVersion());
		return material;
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(13, 19).append(id_).toHashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof FatigueMaterialItem))
			return false;
		if (o == this)
			return true;
		FatigueMaterialItem item = (FatigueMaterialItem) o;
		return new EqualsBuilder().append(id_, item.id_).isEquals();
	}
}
