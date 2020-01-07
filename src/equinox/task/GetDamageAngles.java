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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import equinox.Equinox;
import equinox.data.fileType.DamageAngle;
import equinox.task.InternalEquinoxTask.ShortRunningTask;

/**
 * Class for get damage angles task.
 *
 * @author Murat Artim
 * @date Nov 28, 2014
 * @time 2:16:05 PM
 */
public class GetDamageAngles extends InternalEquinoxTask<ArrayList<String>> implements ShortRunningTask {

	/** Requesting panel. */
	private final DamageAngleRequestingPanel panel_;

	/** Damage angle node to get the angles for. */
	private final DamageAngle angle_;

	/** Decimal formats. */
	private final DecimalFormat format_ = new DecimalFormat("0");

	/**
	 * Creates get damage angles panel.
	 *
	 * @param angle
	 *            Damage angle node to get the angles for.
	 * @param panel
	 *            Requesting panel.
	 */
	public GetDamageAngles(DamageAngle angle, DamageAngleRequestingPanel panel) {
		angle_ = angle;
		panel_ = panel;
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Get damage angles";
	}

	@Override
	protected ArrayList<String> call() throws Exception {

		// update progress info
		updateTitle("Retrieving damage angles...");
		updateMessage("Please wait...");

		// initialize list
		ArrayList<String> angles = new ArrayList<>();

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {
			getAngles(connection, angles);
		}

		// return list
		return angles;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set angles
		try {
			panel_.setDamageAngles(get());
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	/**
	 * Gets the damage angles in degrees from the database.
	 *
	 * @param connection
	 *            Database connection.
	 * @param angles
	 *            List containing the angles in degrees.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void getAngles(Connection connection, ArrayList<String> angles) throws Exception {
		String sql = "select (to_degrees(angle)) as damageAngle from damage_angles where angle_id = " + angle_.getID() + " order by damageAngle asc";
		try (Statement statement = connection.createStatement()) {
			try (ResultSet resultSet = statement.executeQuery(sql)) {
				while (resultSet.next()) {
					angles.add(format_.format(resultSet.getDouble("damageAngle")));
				}
			}
		}
	}

	/**
	 * Interface for damage angle requesting panels.
	 *
	 * @author Murat Artim
	 * @date Nov 28, 2014
	 * @time 2:18:37 PM
	 */
	public interface DamageAngleRequestingPanel {

		/**
		 * Sets angles to this panel.
		 *
		 * @param angles
		 *            List containing the angles in degrees.
		 */
		void setDamageAngles(ArrayList<String> angles);
	}
}
