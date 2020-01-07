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
package equinox.data.input;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;

import equinox.data.StressComponent;
import equinox.data.ui.MyCheckMission;

/**
 * Class for MyCheck input.
 *
 * @author Murat Artim
 * @date Mar 17, 2015
 * @time 12:10:37 PM
 */
public class MyCheckInput implements Serializable {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Enumeration for aircraft program.
	 *
	 * @author Murat Artim
	 * @date Mar 17, 2015
	 * @time 12:15:17 PM
	 */
	public enum AircraftProgram {

		/** Aircraft program. */
		A320_NEO("A320-NEO", "1127   3127   2127", "1159   3159   2159"), A350XWB_900CS("A350XWB-900CS",
				"1105   4105   7105   2105   5105   8105   3105   6105   9105",
				"1133   2138   3144   4133   5138   6144   7133   8138   9144"), A350XWB_1000TL("A350XWB-1000TL",
						"1110   4110   7110   2110   5110   8110   3110   6110   9110",
						"1181   4181   7181   2181   5181   8181   3181   6181   9181"), A350XWB_1000PTL("A350XWB-1000PTL", "2121   5121   8121",
								"2150   5150   8150"), LRESG("LRESG", "1110   4110   2110   3110",
										"1150   4150   2150   3150"), A330_900CS("A330-900CS", "1110   2110   3110", "1139   2139   3139");

		/** Aircraft program name. */
		private final String name_, rotationCodes_, mlgCodes_;

		/**
		 * Creates aircraft program constant.
		 *
		 * @param name
		 *            Name of aircraft program.
		 * @param rotationCodes
		 *            Rotation load cases.
		 * @param mlgCodes
		 *            MLG load cases.
		 */
		AircraftProgram(String name, String rotationCodes, String mlgCodes) {
			name_ = name;
			rotationCodes_ = rotationCodes;
			mlgCodes_ = mlgCodes;
		}

		@Override
		public String toString() {
			return name_;
		}

		/**
		 * Returns rotation codes.
		 *
		 * @return Rotation codes.
		 */
		public String getRotationCodes() {
			return rotationCodes_;
		}

		/**
		 * Returns MLG codes.
		 *
		 * @return MLG codes.
		 */
		public String getMLGCodes() {
			return mlgCodes_;
		}
	}

	/** Missions. */
	private ArrayList<MyCheckMission> missions_;

	/** Boolean options. */
	private boolean[] countANABooleans_, generateSTHBooleans_, plotBooleans_;

	/** Aircraft program. */
	private AircraftProgram program_;

	/** Integer options. */
	private int[] countANAIntegers_, plotIntegers_;

	/** Stress component. */
	private StressComponent stressComponent_;

	/** Double options. */
	private double[] generateSTHDoubles_, plotDoubles_;

	/** Run till flight for generate STH options. */
	private int runTillFlightSTH_;

	/** Path to output directory. */
	private File outputDirectory_;

	/**
	 * Sets missions.
	 *
	 * @param missions
	 *            Missions.
	 */
	public void setMissions(Collection<MyCheckMission> missions) {
		missions_ = new ArrayList<>();
		for (MyCheckMission mission : missions)
			missions_.add(mission);
	}

	/**
	 * Sets count ANA boolean options.
	 *
	 * @param countANABooleans
	 *            Count ANA boolean options.
	 */
	public void setCountANABooleans(boolean[] countANABooleans) {
		countANABooleans_ = countANABooleans;
	}

	/**
	 * Sets count ANA integer options.
	 *
	 * @param countANAIntegers
	 *            Count ANA integer options.
	 */
	public void setCountANAIntegers(int[] countANAIntegers) {
		countANAIntegers_ = countANAIntegers;
	}

	/**
	 * Sets generate STH boolean options.
	 *
	 * @param generateSTHBooleans
	 *            Generate STH boolean options.
	 */
	public void setGenerateSTHBooleans(boolean[] generateSTHBooleans) {
		generateSTHBooleans_ = generateSTHBooleans;
	}

	/**
	 * Sets generate STH double options.
	 *
	 * @param generateSTHDoubles
	 *            Generate STH double options.
	 */
	public void setGenerateSTHDoubles(double[] generateSTHDoubles) {
		generateSTHDoubles_ = generateSTHDoubles;
	}

	/**
	 * Sets plot boolean options.
	 *
	 * @param plotBooleans
	 *            Plot boolean options.
	 */
	public void setPlotBooleans(boolean[] plotBooleans) {
		plotBooleans_ = plotBooleans;
	}

	/**
	 * Sets plot integer options.
	 *
	 * @param plotIntegers
	 *            Plot integer options.
	 */
	public void setPlotIntegers(int[] plotIntegers) {
		plotIntegers_ = plotIntegers;
	}

	/**
	 * Sets plot double options.
	 *
	 * @param plotDoubles
	 *            Plot double options.
	 */
	public void setPlotDoubles(double[] plotDoubles) {
		plotDoubles_ = plotDoubles;
	}

	/**
	 * Sets aircraft program.
	 *
	 * @param program
	 *            Aircraft program.
	 */
	public void setAircraftProgram(AircraftProgram program) {
		program_ = program;
	}

	/**
	 * Sets stress component.
	 *
	 * @param component
	 *            Stress component.
	 */
	public void setStressComponent(StressComponent component) {
		stressComponent_ = component;
	}

	/**
	 * Sets run till flight number for STH options.
	 *
	 * @param runTillFlightSTH
	 *            Run till flight number for STH options.
	 */
	public void setRunTillFlightSTH(int runTillFlightSTH) {
		runTillFlightSTH_ = runTillFlightSTH;
	}

	/**
	 * Sets output directory.
	 *
	 * @param outputDirectory
	 *            Output directory.
	 */
	public void setOutputDirectory(Path outputDirectory) {
		outputDirectory_ = outputDirectory.toFile();
	}

	/**
	 * Returns missions.
	 *
	 * @return Missions.
	 */
	public ArrayList<MyCheckMission> getMissions() {
		return missions_;
	}

	/**
	 * Returns count ANA booleans.
	 *
	 * @return Count ANA booleans.
	 */
	public boolean[] getCountANABooleans() {
		return countANABooleans_;
	}

	/**
	 * Returns count ANA integers.
	 *
	 * @return Count ANA integers.
	 */
	public int[] getCountANAIntegers() {
		return countANAIntegers_;
	}

	/**
	 * Returns generate STH booleans.
	 *
	 * @return Generate STH booleans.
	 */
	public boolean[] getGenerateSTHBooleans() {
		return generateSTHBooleans_;
	}

	/**
	 * Returns generates STH doubles.
	 *
	 * @return Generates STH doubles.
	 */
	public double[] getGenerateSTHDoubles() {
		return generateSTHDoubles_;
	}

	/**
	 * Returns plot booleans.
	 *
	 * @return Plot booleans.
	 */
	public boolean[] getPlotBooleans() {
		return plotBooleans_;
	}

	/**
	 * Returns plot integers.
	 *
	 * @return Plot integers.
	 */
	public int[] getPlotIntegers() {
		return plotIntegers_;
	}

	/**
	 * Returns plot doubles.
	 *
	 * @return Plot doubles.
	 */
	public double[] getPlotDoubles() {
		return plotDoubles_;
	}

	/**
	 * Returns aircraft program.
	 *
	 * @return Aircraft program.
	 */
	public AircraftProgram getAircraftProgram() {
		return program_;
	}

	/**
	 * Returns stress component.
	 *
	 * @return Stress component.
	 */
	public StressComponent getStressComponent() {
		return stressComponent_;
	}

	/**
	 * Returns run till flight number for generate STH options.
	 *
	 * @return Run till flight number for generate STH options.
	 */
	public int getRunTillFlightSTH() {
		return runTillFlightSTH_;
	}

	/**
	 * Returns output directory.
	 *
	 * @return Output directory.
	 */
	public Path getOutputDirectory() {
		return outputDirectory_.toPath();
	}
}
