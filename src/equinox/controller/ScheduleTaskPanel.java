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
package equinox.controller;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.ResourceBundle;

import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;

import equinox.data.EquinoxTheme;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import javafx.util.StringConverter;

/**
 * Class for schedule task panel controller.
 *
 * @author Murat Artim
 * @date Oct 7, 2015
 * @time 11:20:01 AM
 */
public class ScheduleTaskPanel implements Initializable {

	/** The owner panel. */
	private SchedulingPanel owner_;

	/** The owner pop-over. */
	private PopOver popOver_;

	@FXML
	private VBox root_;

	@FXML
	private DatePicker date_;

	@FXML
	private Spinner<Integer> hour_, minute_;

	@FXML
	private Button ok_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		// disable past days
		final Callback<DatePicker, DateCell> dayCellFactory = datePicker -> new DateCell() {

			@Override
			public void updateItem(LocalDate item, boolean empty) {
				super.updateItem(item, empty);
				if (item.isBefore(LocalDate.now())) {
					setDisable(true);
					setStyle("-fx-background-color: #ffc0cb;");
				}
			}
		};
		date_.setDayCellFactory(dayCellFactory);

		// create and set converter to generation date picker
		date_.setConverter(new StringConverter<LocalDate>() {

			/** Date formatter. */
			private final DateTimeFormatter formatter_ = DateTimeFormatter.ofPattern("dd/MM/yyyy");

			@Override
			public String toString(LocalDate date) {
				return date != null ? formatter_.format(date) : "";
			}

			@Override
			public LocalDate fromString(String string) {
				return string != null && !string.isEmpty() ? LocalDate.parse(string, formatter_) : null;
			}
		});

		// set current date
		date_.setValue(LocalDate.now());

		// setup time fields
		Calendar now = Calendar.getInstance();
		hour_.setValueFactory(new IntegerSpinnerValueFactory(1, 24, now.get(Calendar.HOUR_OF_DAY)));
		hour_.getValueFactory().setWrapAround(true);
		minute_.setValueFactory(new IntegerSpinnerValueFactory(1, 59, now.get(Calendar.MINUTE)));
		minute_.getValueFactory().setWrapAround(true);
	}

	@FXML
	private void onCancelClicked() {
		popOver_.hide();
	}

	@FXML
	private void onOkClicked() {

		// get inputs
		LocalDate localDate = date_.getValue();
		Integer hour = hour_.getValue();
		Integer minute = minute_.getValue();

		// invalid values
		if (localDate == null || hour == null || minute == null) {
			String message = "Invalid date specified. Please select a valid date to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(ok_);
			return;
		}

		// create calendar
		Calendar c = Calendar.getInstance();
		c.setTime(Date.from(localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
		c.set(Calendar.HOUR_OF_DAY, hour);
		c.set(Calendar.MINUTE, minute);

		// past time
		if (c.compareTo(Calendar.getInstance()) <= 0) {
			String message = "Invalid date specified. Please select a valid date to proceed.";
			PopOver popOver = new PopOver();
			popOver.setArrowLocation(ArrowLocation.TOP_LEFT);
			popOver.setDetachable(false);
			popOver.setContentNode(NotificationPanel1.load(message, 30, NotificationPanel1.WARNING));
			popOver.setHideOnEscape(true);
			popOver.setAutoHide(true);
			popOver.show(ok_);
			return;
		}

		// submit analysis
		owner_.setTaskScheduleDate(false, c.getTime());

		// hide panel
		popOver_.hide();
	}

	/**
	 * Loads and returns file CDF set panel.
	 *
	 * @param popOver
	 *            The owner pop-over.
	 * @param owner
	 *            The owner panel.
	 * @param initialDate
	 *            Initial date (can be null).
	 * @return The newly loaded file CDF set panel.
	 */
	public static VBox load(PopOver popOver, SchedulingPanel owner, Date initialDate) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("ScheduleTaskPanel.fxml"));
			fxmlLoader.load();

			// get controller
			ScheduleTaskPanel controller = (ScheduleTaskPanel) fxmlLoader.getController();

			// set attributes
			controller.popOver_ = popOver;
			controller.owner_ = owner;

			// set initial date
			if (initialDate != null) {
				Calendar now = Calendar.getInstance();
				now.setTime(initialDate);
				controller.date_.setValue(LocalDate.of(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH)));
				controller.hour_.setValueFactory(new IntegerSpinnerValueFactory(1, 24, now.get(Calendar.HOUR_OF_DAY)));
				controller.hour_.getValueFactory().setWrapAround(true);
				controller.minute_.setValueFactory(new IntegerSpinnerValueFactory(1, 59, now.get(Calendar.MINUTE)));
				controller.minute_.getValueFactory().setWrapAround(true);
			}

			// return controller
			return controller.root_;
		}

		// exception occurred during loading
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Interface for scheduling panels.
	 *
	 * @author Murat Artim
	 * @date Oct 9, 2015
	 * @time 9:39:18 AM
	 */
	public interface SchedulingPanel {

		/**
		 * Set new schedule date for scheduled tasks.
		 *
		 * @param runNow
		 *            True if task(s) should be run right now.
		 * @param scheduleDate
		 *            Schedule date (can be null).
		 */
		void setTaskScheduleDate(boolean runNow, Date scheduleDate);
	}
}
