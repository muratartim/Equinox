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
package equinox.data.ui;

import java.io.File;
import java.io.Serializable;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import equinox.dataServer.remote.data.FatigueMaterial;
import equinox.dataServer.remote.data.LinearMaterial;
import equinox.dataServer.remote.data.PreffasMaterial;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * Class for RFORT Extended tool table item.
 *
 * @author Murat Artim
 * @date Jan 16, 2015
 * @time 1:33:14 PM
 */
public class RfortPilotPoint implements Serializable {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** File name. */
	private final SimpleStringProperty name = new SimpleStringProperty(), factor = new SimpleStringProperty();

	/** STF file. */
	private final File stfFile_;

	/** Fatigue material. */
	private final SimpleObjectProperty<FatigueMaterial> fatiguematerial = new SimpleObjectProperty<>();

	/** Preffas material. */
	private final SimpleObjectProperty<PreffasMaterial> preffasmaterial = new SimpleObjectProperty<>();

	/** Linear material. */
	private final SimpleObjectProperty<LinearMaterial> linearmaterial = new SimpleObjectProperty<>();

	/** Include in RFORT process. */
	private final SimpleBooleanProperty includeinrfort = new SimpleBooleanProperty();

	/**
	 * Creates pilot point item for RFORT extended input table.
	 *
	 * @param stfFile
	 *            Stress input file.
	 */
	public RfortPilotPoint(File stfFile) {
		stfFile_ = stfFile;
		name.set(stfFile_.getName());
	}

	/**
	 * Returns stress input file.
	 *
	 * @return Stress input file.
	 */
	public File getFile() {
		return stfFile_;
	}

	/**
	 * Returns file name.
	 *
	 * @return File name.
	 */
	public String getName() {
		return name.get();
	}

	/**
	 * Returns load to stress factor.
	 *
	 * @return Load to stress factor.
	 */
	public String getFactor() {
		return factor.get();
	}

	/**
	 * Returns fatigue material.
	 *
	 * @return Fatigue material.
	 */
	public FatigueMaterial getFatigueMaterial() {
		return fatiguematerial.get();
	}

	/**
	 * Returns preffas material.
	 *
	 * @return Preffas material.
	 */
	public PreffasMaterial getPreffasMaterial() {
		return preffasmaterial.get();
	}

	/**
	 * Returns linear material.
	 *
	 * @return Linear material.
	 */
	public LinearMaterial getLinearMaterial() {
		return linearmaterial.get();
	}

	/**
	 * Returns true if the pilot point should be included in RFORT process.
	 *
	 * @return True if the pilot point should be included in RFORT process.
	 */
	public boolean isIncludeInRfort() {
		return includeinrfort.get();
	}

	/**
	 * Returns the serializable form of RFORT Extended tool table item.
	 *
	 * @return The serializable form of RFORT Extended tool table item.
	 */
	public SerializableRfortPilotPoint getSerializableForm() {
		return new SerializableRfortPilotPoint(getFile(), getFactor(), getFatigueMaterial(), getPreffasMaterial(), getLinearMaterial(), isIncludeInRfort());
	}

	/**
	 * Sets load to stress factor.
	 *
	 * @param factor
	 *            Load to stress factor.
	 */
	public void setFactor(String factor) {
		this.factor.set(factor);
	}

	/**
	 * Sets fatigue material.
	 *
	 * @param fatiguematerial
	 *            Fatigue material.
	 */
	public void setFatigueMaterial(FatigueMaterial fatiguematerial) {
		this.fatiguematerial.set(fatiguematerial);
	}

	/**
	 * Sets preffas material.
	 *
	 * @param preffasmaterial
	 *            Preffas material.
	 */
	public void setPreffasMaterial(PreffasMaterial preffasmaterial) {
		this.preffasmaterial.set(preffasmaterial);
	}

	/**
	 * Sets linear material.
	 *
	 * @param linearmaterial
	 *            Linear material.
	 */
	public void setLinearMaterial(LinearMaterial linearmaterial) {
		this.linearmaterial.set(linearmaterial);
	}

	/**
	 * Sets whether this pilot point should be included in RFORT process.
	 *
	 * @param includeinrfort
	 *            True to include this pilot point in RFORT process.
	 */
	public void setIncludeInRfort(boolean includeinrfort) {
		this.includeinrfort.set(includeinrfort);
	}

	/**
	 * Returns fatigue material property.
	 *
	 * @return Fatigue material property.
	 */
	public SimpleObjectProperty<FatigueMaterial> fatiguematerialProperty() {
		return fatiguematerial;
	}

	/**
	 * Returns preffas material property.
	 *
	 * @return Preffas material property.
	 */
	public SimpleObjectProperty<PreffasMaterial> preffasmaterialProperty() {
		return preffasmaterial;
	}

	/**
	 * Returns linear propagation material property.
	 *
	 * @return Linear propagation material property.
	 */
	public SimpleObjectProperty<LinearMaterial> linearmaterialProperty() {
		return linearmaterial;
	}

	/**
	 * Returns include in RFORT property.
	 *
	 * @return Include in RFORT property.
	 */
	public SimpleBooleanProperty includeinrfortProperty() {
		return includeinrfort;
	}

	@Override
	public String toString() {
		return getName();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(33, 51).append(name.get()).toHashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof RfortPilotPoint))
			return false;
		if (o == this)
			return true;
		RfortPilotPoint item = (RfortPilotPoint) o;
		return new EqualsBuilder().append(name.get(), item.name.get()).isEquals();
	}
}
