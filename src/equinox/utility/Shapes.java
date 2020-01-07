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

import javafx.scene.control.Button;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcTo;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.HLineTo;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;

/**
 * Utility class for generating various interface shapes.
 *
 * @author Murat Artim
 * @date Dec 14, 2014
 * @time 11:12:20 AM
 */
public class Shapes {

	/**
	 * Creates arrow button.
	 *
	 * @param button
	 *            Button to modify.
	 * @param isLeft
	 *            True if left arrow button.
	 */
	public static void createArrowButton(Button button, boolean isLeft) {

		// set arrow sizes
		double arrowWidth = 9, arrowHeight = 20;

		// create path
		Path path = new Path();

		// begin in the upper left corner
		MoveTo e1 = new MoveTo(0, 0);
		path.getElements().add(e1);

		// draw a horizontal line that defines the width of the shape
		HLineTo e2 = new HLineTo();
		e2.xProperty().bind(button.widthProperty().subtract(arrowWidth)); // bind the width of the shape to the width of the button
		path.getElements().add(e2);

		// draw upper part of right arrow
		LineTo e3 = new LineTo();
		e3.xProperty().bind(e2.xProperty().add(arrowWidth)); // the x end point of this line depends on the x property of line e2
		e3.setY(arrowHeight / 2.0);
		path.getElements().add(e3);

		// draw lower part of right arrow
		LineTo e4 = new LineTo();
		e4.xProperty().bind(e2.xProperty()); // the x end point of this line depends on the x property of line e2
		e4.setY(arrowHeight);
		path.getElements().add(e4);

		// draw lower horizontal line
		HLineTo e5 = new HLineTo(0);
		path.getElements().add(e5);

		// draw an arc for the first bread crumb
		ArcTo arcTo = new ArcTo();
		arcTo.setSweepFlag(true);
		arcTo.setX(0);
		arcTo.setY(0);
		arcTo.setRadiusX(15.0f);
		arcTo.setRadiusY(15.0f);
		path.getElements().add(arcTo);

		// close path
		ClosePath e7 = new ClosePath();
		path.getElements().add(e7);
		path.setFill(Color.BLACK); // this is a dummy color to fill the shape, it won't be visible

		// set shape path to button
		button.setShape(path);

		// left arrow
		if (isLeft)
			button.setRotate(180.0);
	}
}
