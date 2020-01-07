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

import java.nio.file.Path;
import java.sql.Connection;
import java.util.concurrent.ExecutionException;

import equinox.Equinox;
import equinox.controller.InputPanel;
import equinox.data.fileType.AircraftEquivalentStresses;
import equinox.data.fileType.SpectrumItem;
import equinox.data.input.AircraftEquivalentStressType;
import equinox.process.LoadAircraftEquivalentStresses;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.task.serializableTask.SerializableAddAircraftEquivalentStresses;

/**
 * Class for add aircraft model equivalent stresses task.
 *
 * @author Murat Artim
 * @date Sep 4, 2015
 * @time 2:21:53 PM
 */
public class AddAircraftEquivalentStresses extends InternalEquinoxTask<SpectrumItem> implements LongRunningTask, SavableTask {

	/** Input file. */
	private final Path inputFile_;

	/** Equivalent stress type. */
	private final AircraftEquivalentStressType stressType_;

	/** Equivalent stresses folder. */
	private final AircraftEquivalentStresses folder_;

	/**
	 * Creates add aircraft model equivalent stresses task.
	 *
	 * @param inputFile
	 *            Input file.
	 * @param stressType
	 *            Equivalent stress type.
	 * @param folder
	 *            Equivalent stresses folder.
	 */
	public AddAircraftEquivalentStresses(Path inputFile, AircraftEquivalentStressType stressType, AircraftEquivalentStresses folder) {
		inputFile_ = inputFile;
		stressType_ = stressType;
		folder_ = folder;
	}

	@Override
	public SerializableTask getSerializableTask() {
		return new SerializableAddAircraftEquivalentStresses(inputFile_.toFile(), stressType_, folder_);
	}

	@Override
	public String getTaskTitle() {
		return "Add A/C model equivalent stresses";
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	protected SpectrumItem call() throws Exception {

		// update progress info
		updateTitle("Adding A/C model equivalent stresses...");

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			try {

				// disable auto-commit
				connection.setAutoCommit(false);

				// initialize fatigue equivalent stress
				SpectrumItem equivalentStress = new LoadAircraftEquivalentStresses(this, inputFile_, stressType_, folder_).start(connection);

				// cannot load
				if (equivalentStress == null) {
					connection.rollback();
					connection.setAutoCommit(true);
					return null;
				}

				// commit updates
				connection.commit();
				connection.setAutoCommit(true);

				// return equivalent stress
				return equivalentStress;
			}

			// exception occurred during process
			catch (Exception e) {

				// roll back updates
				if (connection != null) {
					connection.rollback();
					connection.setAutoCommit(true);
				}

				// propagate exception
				throw e;
			}
		}
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		try {

			// show file view panel
			taskPanel_.getOwner().getOwner().getInputPanel().showSubPanel(InputPanel.FILE_VIEW_PANEL);

			// add equivalent stress to equivalent stresses folder
			folder_.getChildren().add(get());
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}
}
