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

/**
 * Class for serializable form of RFORT Extended tool table item.
 *
 * @author Murat Artim
 * @date Oct 12, 2015
 * @time 4:55:39 PM
 */
public class SerializableRfortPilotPoint implements Serializable {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** File name. */
	private final String name_, factor_;

	/** STF file. */
	private final File stfFile_;

	/** Fatigue material. */
	private final FatigueMaterial fatigueMaterial_;

	/** Preffas material. */
	private final PreffasMaterial preffasMaterial_;

	/** Linear material. */
	private final LinearMaterial linearMaterial_;

	/** Include in RFORT process. */
	private final boolean includeInRfort_;

	/**
	 * Creates pilot point item for RFORT extended input table.
	 *
	 * @param stfFile
	 *            Stress input file.
	 * @param factor
	 *            Load to stress factor.
	 * @param fatigueMaterial
	 *            Fatigue material.
	 * @param preffasMaterial
	 *            Preffas material.
	 * @param linearMaterial
	 *            Linear material.
	 * @param includeInRfort
	 *            True to include this pilot point in RFORT process.
	 */
	public SerializableRfortPilotPoint(File stfFile, String factor, FatigueMaterial fatigueMaterial, PreffasMaterial preffasMaterial, LinearMaterial linearMaterial, boolean includeInRfort) {
		stfFile_ = stfFile;
		name_ = stfFile_.getName();
		factor_ = factor;
		fatigueMaterial_ = fatigueMaterial;
		preffasMaterial_ = preffasMaterial;
		linearMaterial_ = linearMaterial;
		includeInRfort_ = includeInRfort;
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
		return name_;
	}

	/**
	 * Returns load to stress factor.
	 *
	 * @return Load to stress factor.
	 */
	public String getFactor() {
		return factor_;
	}

	/**
	 * Returns fatigue material.
	 *
	 * @return Fatigue material.
	 */
	public FatigueMaterial getFatigueMaterial() {
		return fatigueMaterial_;
	}

	/**
	 * Returns preffas material.
	 *
	 * @return Preffas material.
	 */
	public PreffasMaterial getPreffasMaterial() {
		return preffasMaterial_;
	}

	/**
	 * Returns linear material.
	 *
	 * @return Linear material.
	 */
	public LinearMaterial getLinearMaterial() {
		return linearMaterial_;
	}

	/**
	 * Returns true if the pilot point should be included in RFORT process.
	 *
	 * @return True if the pilot point should be included in RFORT process.
	 */
	public boolean getIncludeInRfort() {
		return includeInRfort_;
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(45, 105).append(name_).toHashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof SerializableRfortPilotPoint))
			return false;
		if (o == this)
			return true;
		SerializableRfortPilotPoint item = (SerializableRfortPilotPoint) o;
		return new EqualsBuilder().append(name_, item.name_).isEquals();
	}
}
