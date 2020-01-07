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

import equinox.controller.MainScreen;
import equinox.data.fileType.SpectrumItem;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.input.DragEvent;

/**
 * Abstract class for drag listener.
 *
 * @author Murat Artim
 * @date Feb 24, 2015
 * @time 1:18:24 PM
 */
public abstract class DragListener implements EventHandler<DragEvent> {

	/** The owner cell implementation. */
	protected final DragCell cell_;

	/**
	 * Creates drag listener.
	 *
	 * @param cell
	 *            The owner cell implementation.
	 */
	public DragListener(DragCell cell) {
		cell_ = cell;
	}

	@Override
	public void handle(DragEvent event) {
		EventType<DragEvent> type = event.getEventType();
		if (type.equals(DragEvent.DRAG_OVER))
			onDragOver(event);
		else if (type.equals(DragEvent.DRAG_ENTERED))
			onDragEntered(event);
		else if (type.equals(DragEvent.DRAG_EXITED))
			onDragExited(event);
		else if (type.equals(DragEvent.DRAG_DROPPED))
			onDragDropped(event);
	}

	/**
	 * Called when drag over event happens.
	 *
	 * @param event
	 *            Drag event.
	 */
	protected abstract void onDragOver(DragEvent event);

	/**
	 * Called when drag entered event happens.
	 *
	 * @param event
	 *            Drag event.
	 */
	protected abstract void onDragEntered(DragEvent event);

	/**
	 * Called when drag exited event happens.
	 *
	 * @param event
	 *            Drag event.
	 */
	protected abstract void onDragExited(DragEvent event);

	/**
	 * Called when drag dropped event happens.
	 *
	 * @param event
	 *            Drag event.
	 */
	protected abstract void onDragDropped(DragEvent event);

	/**
	 * Interface for all drag cells.
	 *
	 * @author Murat Artim
	 * @date Feb 24, 2015
	 * @time 1:43:39 PM
	 */
	public interface DragCell {

		/**
		 * Returns graphic of this cell.
		 *
		 * @return Graphic of this cell.
		 */
		Node getGraphic();

		/**
		 * Returns true if cell is selected.
		 *
		 * @return True if cell is selected.
		 */
		boolean isSelected();

		/**
		 * Returns spectrum item of this cell.
		 *
		 * @return Spectrum item of this cell.
		 */
		SpectrumItem getSpectrumItem();

		/**
		 * Returns the owner main screen.
		 *
		 * @return The owner main screen.
		 */
		MainScreen getMainScreen();
	}
}
