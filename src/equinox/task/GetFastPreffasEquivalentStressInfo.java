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
package equinox.task;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import equinox.Equinox;
import equinox.controller.InfoViewPanel;
import equinox.controller.ViewPanel;
import equinox.data.LoadcaseFactor;
import equinox.data.SegmentFactor;
import equinox.data.StressComponent;
import equinox.data.fileType.FastPreffasEquivalentStress;
import equinox.data.input.FastEquivalentStressInput;
import equinox.data.input.GenerateStressSequenceInput;
import equinox.data.ui.TableItem;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import javafx.scene.control.TreeItem;

/**
 * Class for getting fast preffas equivalent stress info.
 *
 * @author Murat Artim
 * @date Jun 15, 2016
 * @time 1:02:43 PM
 */
public class GetFastPreffasEquivalentStressInfo extends InternalEquinoxTask<ArrayList<TreeItem<TableItem>>> implements ShortRunningTask {

	/** Preffas equivalent stress item. */
	private final FastPreffasEquivalentStress equivalentStress_;

	/** Decimal format. */
	private final DecimalFormat format_ = new DecimalFormat("0.##"), format2_ = new DecimalFormat("0.##E0");

	/**
	 * Creates get fast preffas equivalent stress info task.
	 *
	 * @param equivalentStress
	 *            Preffas equivalent stress item.
	 */
	public GetFastPreffasEquivalentStressInfo(FastPreffasEquivalentStress equivalentStress) {
		equivalentStress_ = equivalentStress;
	}

	@Override
	public String getTaskTitle() {
		return "Get preffas equivalent stress info for '" + equivalentStress_.getName() + "'";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	protected ArrayList<TreeItem<TableItem>> call() throws Exception {

		// update progress info
		updateTitle("Retrieving preffas equivalent stress info '" + equivalentStress_.getName() + "'");

		// create list
		ArrayList<TreeItem<TableItem>> list = new ArrayList<>();

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// create statement
			try (Statement statement = connection.createStatement()) {

				// get all lines of the conversion table
				String sql = "select * from fast_preffas_equivalent_stresses where id = " + equivalentStress_.getID();
				try (ResultSet resultSet = statement.executeQuery(sql)) {
					while (resultSet.next()) {

						// equivalent stress
						list.add(new TreeItem<>(new TableItem("Preffas equivalent stress", format_.format(resultSet.getDouble("stress")))));

						// spectrum info
						TreeItem<TableItem> spectrum = new TreeItem<>(new TableItem("Spectrum info", ""));
						spectrum.getChildren().add(new TreeItem<>(new TableItem("A/C program", equivalentStress_.getParentItem().getParentItem().getProgram())));
						spectrum.getChildren().add(new TreeItem<>(new TableItem("A/C section", equivalentStress_.getParentItem().getParentItem().getSection())));
						spectrum.getChildren().add(new TreeItem<>(new TableItem("Fatigue mission", equivalentStress_.getParentItem().getMission())));
						list.add(spectrum);

						// get analysis input
						FastEquivalentStressInput analysisInput = null;
						Blob blob = resultSet.getBlob("analysis_input");
						if (blob != null) {
							byte[] bytes = blob.getBytes(1L, (int) blob.length());
							blob.free();
							try (ByteArrayInputStream bos = new ByteArrayInputStream(bytes)) {
								try (ObjectInputStream ois = new ObjectInputStream(bos)) {
									analysisInput = (FastEquivalentStressInput) ois.readObject();
								}
							}
						}

						// input found
						if (analysisInput != null) {

							// stress component and rotation angle
							StressComponent stressComp = analysisInput.getStressComponent();
							TreeItem<TableItem> stress = new TreeItem<>(new TableItem("Stress component", stressComp.toString()));
							list.add(stress);
							if (stressComp.equals(StressComponent.ROTATED)) {
								stress.getChildren().add(new TreeItem<>(new TableItem("Rotation angle", format_.format(Math.toDegrees(analysisInput.getRotationAngle())))));
							}

							// delta-t temperature info
							TreeItem<TableItem> deltaT = new TreeItem<>(new TableItem("Temperature", ""));
							String value = analysisInput.getDTLoadcaseSup();
							deltaT.getChildren().add(new TreeItem<>(new TableItem("DT load case (sup.)", value == null ? "N/A" : value)));
							deltaT.getChildren().add(new TreeItem<>(new TableItem("Reference DT (sup.)", value == null ? "N/A" : format_.format(analysisInput.getReferenceDTSup()))));
							value = analysisInput.getDTLoadcaseInf();
							deltaT.getChildren().add(new TreeItem<>(new TableItem("DT load case (inf.)", value == null ? "N/A" : value)));
							deltaT.getChildren().add(new TreeItem<>(new TableItem("Reference DT (inf.)", value == null ? "N/A" : format_.format(analysisInput.getReferenceDTInf()))));
							list.add(deltaT);

							// overall stress modifiers
							TreeItem<TableItem> overallStressModifiers = new TreeItem<>(new TableItem("Overall stress factors", ""));
							value = format_.format(analysisInput.getStressModificationValue(GenerateStressSequenceInput.ONEG)) + " (" + analysisInput.getStressModificationMethod(GenerateStressSequenceInput.ONEG) + ")";
							overallStressModifiers.getChildren().add(new TreeItem<>(new TableItem("1g stress modifier", value)));
							value = format_.format(analysisInput.getStressModificationValue(GenerateStressSequenceInput.INCREMENT)) + " (" + analysisInput.getStressModificationMethod(GenerateStressSequenceInput.INCREMENT) + ")";
							overallStressModifiers.getChildren().add(new TreeItem<>(new TableItem("Increment stress modifier", value)));
							value = format_.format(analysisInput.getStressModificationValue(GenerateStressSequenceInput.DELTAP)) + " (" + analysisInput.getStressModificationMethod(GenerateStressSequenceInput.DELTAP) + ")";
							overallStressModifiers.getChildren().add(new TreeItem<>(new TableItem("Delta-p stress modifier", value)));
							value = format_.format(analysisInput.getStressModificationValue(GenerateStressSequenceInput.DELTAT)) + " (" + analysisInput.getStressModificationMethod(GenerateStressSequenceInput.DELTAT) + ")";
							overallStressModifiers.getChildren().add(new TreeItem<>(new TableItem("Delta-t stress modifier", value)));
							list.add(overallStressModifiers);

							// loadcase modifiers
							ArrayList<LoadcaseFactor> lcFactors = analysisInput.getLoadcaseFactors();
							if (lcFactors != null && !lcFactors.isEmpty()) {
								TreeItem<TableItem> loadcaseModifiers = new TreeItem<>(new TableItem("Loadcase factors", ""));
								for (LoadcaseFactor lcFactor : lcFactors) {
									String loadcaseNumber = lcFactor.getLoadcaseNumber();
									String eventName = lcFactor.getEventName();
									String comments = lcFactor.getComments();
									TreeItem<TableItem> loadcase = new TreeItem<>(new TableItem("Loadcase '" + loadcaseNumber + "'", (eventName == null ? "" : eventName) + (comments == null ? "" : " (" + comments + ")")));
									loadcase.getChildren().add(new TreeItem<>(new TableItem("Stress modifier", format_.format(lcFactor.getModifierValue()) + " (" + lcFactor.getModifierMethod() + ")")));
									loadcaseModifiers.getChildren().add(loadcase);
								}
								if (!loadcaseModifiers.getChildren().isEmpty()) {
									list.add(loadcaseModifiers);
								}
							}

							// segment modifiers
							ArrayList<SegmentFactor> sFactors = analysisInput.getSegmentFactors();
							if (sFactors != null && !sFactors.isEmpty()) {
								TreeItem<TableItem> segmentModifiers = new TreeItem<>(new TableItem("Segment factors", ""));
								for (SegmentFactor sFactor : sFactors) {
									TreeItem<TableItem> segment = new TreeItem<>(new TableItem("Segment '" + sFactor.getSegment().toString() + "'", ""));
									segment.getChildren().add(new TreeItem<>(new TableItem("1G stress modifier", format_.format(sFactor.getModifierValue(GenerateStressSequenceInput.ONEG)) + " (" + sFactor.getModifierMethod(GenerateStressSequenceInput.ONEG) + ")")));
									segment.getChildren().add(new TreeItem<>(new TableItem("Increment stress modifier", format_.format(sFactor.getModifierValue(GenerateStressSequenceInput.INCREMENT)) + " (" + sFactor.getModifierMethod(GenerateStressSequenceInput.INCREMENT) + ")")));
									segment.getChildren().add(new TreeItem<>(new TableItem("Delta-p stress modifier", format_.format(sFactor.getModifierValue(GenerateStressSequenceInput.DELTAP)) + " (" + sFactor.getModifierMethod(GenerateStressSequenceInput.DELTAP) + ")")));
									segment.getChildren().add(new TreeItem<>(new TableItem("Delta-t stress modifier", format_.format(sFactor.getModifierValue(GenerateStressSequenceInput.DELTAT)) + " (" + sFactor.getModifierMethod(GenerateStressSequenceInput.DELTAT) + ")")));
									segmentModifiers.getChildren().add(segment);
								}
								if (!segmentModifiers.getChildren().isEmpty()) {
									list.add(segmentModifiers);
								}
							}
						}

						// rainflow info
						TreeItem<TableItem> rainflow = new TreeItem<>(new TableItem("Rainflow info", ""));
						if (analysisInput != null) {
							rainflow.getChildren().add(new TreeItem<>(new TableItem("Remove negative stresses", analysisInput.isRemoveNegativeStresses() ? "Yes" : "No")));
						}
						double omissionLevel = resultSet.getDouble("omission_level");
						rainflow.getChildren().add(new TreeItem<>(new TableItem("Omission level", omissionLevel == -1 ? "No omission applied." : format_.format(omissionLevel))));
						rainflow.getChildren().add(new TreeItem<>(new TableItem("Total number of flights", (int) resultSet.getDouble("validity") + "")));
						list.add(rainflow);

						// material info
						TreeItem<TableItem> material = new TreeItem<>(new TableItem("Material info", ""));
						material.getChildren().add(new TreeItem<>(new TableItem("Name", resultSet.getString("material_name"))));
						material.getChildren().add(new TreeItem<>(new TableItem("Specification", resultSet.getString("material_specification"))));
						material.getChildren().add(new TreeItem<>(new TableItem("Library version", resultSet.getString("material_library_version"))));
						material.getChildren().add(new TreeItem<>(new TableItem("Family", resultSet.getString("material_family"))));
						material.getChildren().add(new TreeItem<>(new TableItem("Orientation", resultSet.getString("material_orientation"))));
						material.getChildren().add(new TreeItem<>(new TableItem("Configuration", resultSet.getString("material_configuration"))));
						material.getChildren().add(new TreeItem<>(new TableItem("Material constant (Ceff)", format2_.format(resultSet.getDouble("material_ceff")))));
						material.getChildren().add(new TreeItem<>(new TableItem("Material constant (m)", format_.format(resultSet.getDouble("material_m")))));
						material.getChildren().add(new TreeItem<>(new TableItem("Material constant (A)", format_.format(resultSet.getDouble("material_a")))));
						material.getChildren().add(new TreeItem<>(new TableItem("Material constant (B)", format_.format(resultSet.getDouble("material_b")))));
						material.getChildren().add(new TreeItem<>(new TableItem("Material constant (C)", format_.format(resultSet.getDouble("material_c")))));
						material.getChildren().add(new TreeItem<>(new TableItem("Ftu", format_.format(resultSet.getDouble("material_ftu")))));
						material.getChildren().add(new TreeItem<>(new TableItem("Fty", format_.format(resultSet.getDouble("material_fty")))));
						material.getChildren().add(new TreeItem<>(new TableItem("Material ISAMI version", resultSet.getString("material_isami_version"))));
						list.add(material);
					}
				}
			}
		}

		// return list
		return list;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set file info
		try {
			InfoViewPanel panel = (InfoViewPanel) taskPanel_.getOwner().getOwner().getViewPanel().getSubPanel(ViewPanel.INFO_VIEW);
			panel.getInfoTable().getRoot().getChildren().setAll(get());
			panel.showInfoView(true, equivalentStress_);
			taskPanel_.getOwner().getOwner().getViewPanel().showSubPanel(ViewPanel.INFO_VIEW);
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}
}
