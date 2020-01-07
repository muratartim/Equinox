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
package equinox.utility;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.SwingUtilities;

/**
 * Class for mouse event dispatcher.
 *
 * @author Murat Artim
 * @date Sep 15, 2015
 * @time 9:56:18 AM
 */
public class MouseDispatcher extends MouseAdapter {

	/** Component to dispatch mouse events. */
	private final Component component_;

	/**
	 * Creates mouse dispatcher.
	 *
	 * @param component
	 *            Component to dispatch mouse events.
	 */
	public MouseDispatcher(Component component) {
		super();
		component_ = component;
	}

	@Override
	public void mousePressed(MouseEvent e) {
		redispatchEvent(e);
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		redispatchEvent(e);
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		redispatchEvent(e);
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		redispatchEvent(e);
	}

	@Override
	public void mouseExited(MouseEvent e) {
		redispatchEvent(e);
	}

	/**
	 * Dispatches given mouse event to animation panel.
	 *
	 * @param e
	 *            Mouse event to dispatch.
	 */
	private void redispatchEvent(MouseEvent e) {

		// convert mouse point to bottom panel's coordinates
		Point componentPoint = SwingUtilities.convertPoint((Component) e.getSource(), e.getPoint(), component_);

		// dispatch event
		// DEPRECATION e.getModifiers() is deprecated in java 9 therefore e.getModifiersEx() is used instead
		component_.dispatchEvent(new MouseEvent(component_, e.getID(), e.getWhen(), e.getModifiersEx(), componentPoint.x, componentPoint.y, e.getClickCount(), e.isPopupTrigger()));
	}

	/**
	 * Sets mouse dispatcher to given source component.
	 *
	 * @param source
	 *            Source of mouse events.
	 * @param target
	 *            Target component to dispatch mouse events.
	 */
	public static void setMouseDispatcher(Component source, Component target) {
		MouseDispatcher dispatcher = new MouseDispatcher(target);
		source.addMouseListener(dispatcher);
		source.addMouseMotionListener(dispatcher);
	}
}
