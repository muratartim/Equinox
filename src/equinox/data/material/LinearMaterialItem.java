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

import equinox.dataServer.remote.data.LinearMaterial;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * Class for Linear material table item.
 *
 * @author Murat Artim
 * @date Dec 7, 2015
 * @time 12:36:04 PM
 */
public class LinearMaterialItem {

	/** String attributes. */
	private final SimpleStringProperty name = new SimpleStringProperty(), specification = new SimpleStringProperty(), libraryVersion = new SimpleStringProperty(), family = new SimpleStringProperty(), orientation = new SimpleStringProperty(), configuration = new SimpleStringProperty(),
			isamiVersion = new SimpleStringProperty();

	/** Double attributes. */
	private final SimpleDoubleProperty ceff = new SimpleDoubleProperty(), m = new SimpleDoubleProperty(), a = new SimpleDoubleProperty(), b = new SimpleDoubleProperty(), c = new SimpleDoubleProperty(), ftu = new SimpleDoubleProperty(), fty = new SimpleDoubleProperty();

	/** Material ID. */
	private final int id_;

	/**
	 * Creates Linear material item.
	 *
	 * @param id
	 *            Material ID.
	 */
	public LinearMaterialItem(int id) {
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
	 * Returns Ceff.
	 *
	 * @return Ceff.
	 */
	public double getCeff() {
		return ceff.get();
	}

	/**
	 * Returns m.
	 *
	 * @return M.
	 */
	public double getM() {
		return m.get();
	}

	/**
	 * Returns a.
	 *
	 * @return A.
	 */
	public double getA() {
		return a.get();
	}

	/**
	 * Returns b.
	 *
	 * @return B.
	 */
	public double getB() {
		return b.get();
	}

	/**
	 * Returns c.
	 *
	 * @return C.
	 */
	public double getC() {
		return c.get();
	}

	/**
	 * Returns Ftu.
	 *
	 * @return Ftu.
	 */
	public double getFtu() {
		return ftu.get();
	}

	/**
	 * Returns Fty.
	 *
	 * @return Fty.
	 */
	public double getFty() {
		return fty.get();
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
	 * Sets Ceff.
	 *
	 * @param ceff
	 *            Ceff.
	 */
	public void setCeff(double ceff) {
		this.ceff.set(ceff);
	}

	/**
	 * Sets m.
	 *
	 * @param m
	 *            M.
	 */
	public void setM(double m) {
		this.m.set(m);
	}

	/**
	 * Sets A.
	 *
	 * @param a
	 *            A.
	 */
	public void setA(double a) {
		this.a.set(a);
	}

	/**
	 * Sets B.
	 *
	 * @param b
	 *            B.
	 */
	public void setB(double b) {
		this.b.set(b);
	}

	/**
	 * Sets C.
	 *
	 * @param c
	 *            C.
	 */
	public void setC(double c) {
		this.c.set(c);
	}

	/**
	 * Sets Ftu.
	 *
	 * @param ftu
	 *            Ftu.
	 */
	public void setFtu(double ftu) {
		this.ftu.set(ftu);
	}

	/**
	 * Sets Fty.
	 *
	 * @param fty
	 *            Fty.
	 */
	public void setFty(double fty) {
		this.fty.set(fty);
	}

	/**
	 * Creates and returns Linear material to be used for analysis.
	 *
	 * @return Linear material to be used for analysis.
	 */
	public LinearMaterial getMaterial() {
		LinearMaterial material = new LinearMaterial(id_);
		material.setName(getName());
		material.setSpecification(getSpecification());
		material.setLibraryVersion(getLibraryVersion());
		material.setFamily(getFamily());
		material.setOrientation(getOrientation());
		material.setConfiguration(getConfiguration());
		material.setCeff(getCeff());
		material.setM(getM());
		material.setA(getA());
		material.setB(getB());
		material.setC(getC());
		material.setFtu(getFtu());
		material.setFty(getFty());
		material.setIsamiVersion(getIsamiVersion());
		return material;
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(3, 79).append(id_).toHashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof LinearMaterialItem))
			return false;
		if (o == this)
			return true;
		LinearMaterialItem item = (LinearMaterialItem) o;
		return new EqualsBuilder().append(id_, item.id_).isEquals();
	}
}
