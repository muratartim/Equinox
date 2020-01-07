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

import java.util.List;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Utility class for supplying UI animations.
 *
 * @author Murat Artim
 * @date Mar 25, 2013
 * @time 11:39:52 AM
 */
public class Animator {

	/**
	 * Creates and returns scaling animation of given nodes in a <u>sequential transition</u> within two steps as follows;
	 * <UL>
	 * <LI><i>First step:</i> Scales node from <code>startScale</code> to <code>midScale</code>,
	 * <LI><i>Second step:</i> Scales node from <code>midScale</code> to <code>endScale</code>.
	 * </UL>
	 *
	 * @param startDelay
	 *            Start delay in milliseconds.
	 * @param stepDuration
	 *            Step duration in milliseconds.
	 * @param startScale
	 *            Scaling factor at animation start.
	 * @param midScale
	 *            Scaling factor at the end of first animation step.
	 * @param endScale
	 *            Scaling factor at animation end.
	 * @param onFinished
	 *            Event handler to be notified when animation ends.
	 * @param nodes
	 *            List containing the animated nodes of the screen. Note that, nodes will be animated in given order.
	 * @return The newly created animation.
	 */
	public static Animation bouncingScale(double startDelay, double stepDuration, double startScale, final double midScale, final double endScale, EventHandler<ActionEvent> onFinished, List<? extends Node> nodes) {

		// create bouncing scale animation sequence
		SequentialTransition bouncingScale = new SequentialTransition();

		// set delay to sequence
		bouncingScale.setDelay(Duration.millis(startDelay));

		// loop over animated nodes
		for (Node node : nodes) {

			// scale down node
			node.setScaleX(startScale);
			node.setScaleY(startScale);
			node.setScaleZ(startScale);

			// create and add first scale animation
			ScaleTransition scale1 = new ScaleTransition();
			scale1.setFromX(startScale);
			scale1.setFromY(startScale);
			scale1.setFromZ(startScale);
			scale1.setToX(midScale);
			scale1.setToY(midScale);
			scale1.setToZ(midScale);
			scale1.setDuration(Duration.millis(stepDuration));
			scale1.setCycleCount(1);
			scale1.setNode(node);
			bouncingScale.getChildren().add(scale1);

			// create and add second scale animation
			ScaleTransition scale2 = new ScaleTransition();
			scale2.setFromX(midScale);
			scale2.setFromY(midScale);
			scale2.setFromZ(midScale);
			scale2.setToX(endScale);
			scale2.setToY(endScale);
			scale2.setToZ(endScale);
			scale2.setDuration(Duration.millis(stepDuration));
			scale2.setCycleCount(1);
			scale2.setNode(node);
			bouncingScale.getChildren().add(scale2);
		}

		// set end action to sequence
		if (onFinished != null) {
			bouncingScale.setOnFinished(onFinished);
		}

		// return animation sequence
		return bouncingScale;
	}

	/**
	 * Creates and returns scaling animation of given nodes in a <u>sequential transition</u> within two steps as follows;
	 * <UL>
	 * <LI><i>First step:</i> Scales node from <code>startScale</code> to <code>midScale</code>,
	 * <LI><i>Second step:</i> Scales node from <code>midScale</code> to <code>endScale</code>.
	 * </UL>
	 *
	 * @param startDelay
	 *            Start delay in milliseconds.
	 * @param stepDuration
	 *            Step duration in milliseconds.
	 * @param startScale
	 *            Scaling factor at animation start.
	 * @param midScale
	 *            Scaling factor at the end of first animation step.
	 * @param endScale
	 *            Scaling factor at animation end.
	 * @param onFinished
	 *            Event handler to be notified when animation ends.
	 * @param nodes
	 *            Array containing the animated nodes of the screen. Note that, nodes will be animated in given order.
	 * @return The newly created animation.
	 */
	public static Animation bouncingScale(double startDelay, double stepDuration, double startScale, final double midScale, final double endScale, EventHandler<ActionEvent> onFinished, Node... nodes) {

		// create bouncing scale animation sequence
		SequentialTransition bouncingScale = new SequentialTransition();

		// set delay to sequence
		bouncingScale.setDelay(Duration.millis(startDelay));

		// loop over animated nodes
		for (Node node : nodes) {

			// scale down node
			node.setScaleX(startScale);
			node.setScaleY(startScale);
			node.setScaleZ(startScale);

			// create and add first scale animation
			ScaleTransition scale1 = new ScaleTransition();
			scale1.setFromX(startScale);
			scale1.setFromY(startScale);
			scale1.setFromZ(startScale);
			scale1.setToX(midScale);
			scale1.setToY(midScale);
			scale1.setToZ(midScale);
			scale1.setDuration(Duration.millis(stepDuration));
			scale1.setCycleCount(1);
			scale1.setNode(node);
			bouncingScale.getChildren().add(scale1);

			// create and add second scale animation
			ScaleTransition scale2 = new ScaleTransition();
			scale2.setFromX(midScale);
			scale2.setFromY(midScale);
			scale2.setFromZ(midScale);
			scale2.setToX(endScale);
			scale2.setToY(endScale);
			scale2.setToZ(endScale);
			scale2.setDuration(Duration.millis(stepDuration));
			scale2.setCycleCount(1);
			scale2.setNode(node);
			bouncingScale.getChildren().add(scale2);
		}

		// set end action to sequence
		if (onFinished != null) {
			bouncingScale.setOnFinished(onFinished);
		}

		// return animation sequence
		return bouncingScale;
	}

	/**
	 * Creates and returns scaling animation of given nodes in a <u>sequential transition</u> within two steps as follows;
	 * <UL>
	 * <LI><i>First step:</i> Scales node from <code>startScale</code> to <code>midScale</code>,
	 * <LI><i>Second step:</i> Scales node from <code>midScale</code> to <code>endScale</code>.
	 * </UL>
	 *
	 * @param startDelay
	 *            Start delay in milliseconds.
	 * @param stepDuration
	 *            Step duration in milliseconds.
	 * @param startScale
	 *            Scaling factor at animation start.
	 * @param endScale
	 *            Scaling factor at animation end.
	 * @param onFinished
	 *            Event handler to be notified when animation ends.
	 * @param nodes
	 *            Array containing the animated nodes of the screen. Note that, nodes will be animated in given order.
	 * @return The newly created animation.
	 */
	public static Animation bouncingScale2(double startDelay, double stepDuration, double startScale, double endScale, EventHandler<ActionEvent> onFinished, Node... nodes) {

		// create bouncing scale animation sequence
		ParallelTransition bouncingScale = new ParallelTransition();

		// set delay to sequence
		bouncingScale.setDelay(Duration.millis(startDelay));

		// loop over animated nodes
		int i = 0;
		for (Node node : nodes) {

			// scale down node
			node.setScaleX(startScale);
			node.setScaleY(startScale);
			node.setScaleZ(startScale);

			// create and add first scale animation
			ScaleTransition scale1 = new ScaleTransition();
			scale1.setDelay(Duration.millis(i * stepDuration / 4.0));
			scale1.setFromX(startScale);
			scale1.setFromY(startScale);
			scale1.setFromZ(startScale);
			scale1.setToX(endScale);
			scale1.setToY(endScale);
			scale1.setToZ(endScale);
			scale1.setDuration(Duration.millis(stepDuration));
			scale1.setCycleCount(1);
			scale1.setNode(node);
			bouncingScale.getChildren().add(scale1);
			i++;
		}

		// set end action to sequence
		if (onFinished != null) {
			bouncingScale.setOnFinished(onFinished);
		}

		// return animation sequence
		return bouncingScale;
	}

	/**
	 * Creates and returns sequential fade animation sequence.
	 *
	 * @param isFadeIn
	 *            True to fade in, false for fade out.
	 * @param startDelay
	 *            Start delay in milliseconds.
	 * @param stepDuration
	 *            Step duration in milliseconds.
	 * @param onFinished
	 *            Event handler to be notified when animation ends.
	 * @param nodes
	 *            Array containing the animated nodes of the screen. Note that, nodes will be animated in given order.
	 * @return The newly created animation.
	 */
	public static Animation fade(boolean isFadeIn, double startDelay, double stepDuration, EventHandler<ActionEvent> onFinished, final Node... nodes) {

		// create animation sequence
		SequentialTransition seqTrans = new SequentialTransition();

		// set delay to sequence
		seqTrans.setDelay(Duration.millis(startDelay));

		// loop over animated nodes
		for (Node node : nodes) {

			// make nodes transparent
			node.setOpacity(isFadeIn ? 0.0 : 1.0);

			// create fade transition
			FadeTransition fade = new FadeTransition();

			// set first step fade values
			fade.setFromValue(isFadeIn ? 0.0 : 1.0);
			fade.setToValue(isFadeIn ? 1.0 : 0.0);

			// set duration and cycle count
			fade.setDuration(Duration.millis(stepDuration));
			fade.setCycleCount(1);

			// set node
			fade.setNode(node);

			// add to animation sequence
			seqTrans.getChildren().add(fade);
		}

		// set end action to sequence
		if (onFinished != null) {
			seqTrans.setOnFinished(onFinished);
		}

		// return animation sequence
		return seqTrans;
	}

	/**
	 * Animates main screen.
	 *
	 * @param inputPanel
	 *            Input panel.
	 * @param viewPanel
	 *            View panel.
	 * @param menuBarPanel
	 *            Menu bar panel.
	 * @param enter
	 *            True if components should enter the main screen.
	 * @param animation
	 *            Animation to be played in parallel to main screen animation. Can be null.
	 * @param onFinished
	 *            Event handler to be notified when animation ends. Can be null.
	 */
	public static void animateMainScreen(VBox inputPanel, VBox viewPanel, StackPane menuBarPanel, boolean enter, Animation animation, EventHandler<ActionEvent> onFinished) {

		// create transition
		ParallelTransition transition = new ParallelTransition();

		// create translation animation for files panel and add it to transition
		double translation = inputPanel.getWidth();
		TranslateTransition anim1 = new TranslateTransition();
		anim1.setDelay(Duration.millis(40));
		anim1.setFromX(enter ? -translation : 0);
		anim1.setToX(enter ? 0 : -translation);
		anim1.setDuration(Duration.millis(400));
		anim1.setCycleCount(1);
		anim1.setInterpolator(enter ? Interpolator.EASE_IN : Interpolator.EASE_OUT);
		anim1.setNode(inputPanel);
		transition.getChildren().add(anim1);

		// create translation animation for files panel and add it to transition
		translation = viewPanel.getWidth();
		TranslateTransition anim2 = new TranslateTransition();
		anim2.setDelay(Duration.millis(40));
		anim2.setFromX(enter ? translation : 0);
		anim2.setToX(enter ? 0 : translation);
		anim2.setDuration(Duration.millis(400));
		anim2.setCycleCount(1);
		anim2.setInterpolator(enter ? Interpolator.EASE_IN : Interpolator.EASE_OUT);
		anim2.setNode(viewPanel);
		transition.getChildren().add(anim2);

		// create translation animation for files panel and add it to transition
		translation = menuBarPanel.getHeight();
		TranslateTransition anim3 = new TranslateTransition();
		anim3.setDelay(Duration.millis(40));
		anim3.setFromY(enter ? -translation : 0);
		anim3.setToY(enter ? 0 : -translation);
		anim3.setDuration(Duration.millis(400));
		anim3.setCycleCount(1);
		anim3.setInterpolator(enter ? Interpolator.EASE_IN : Interpolator.EASE_OUT);
		anim3.setNode(menuBarPanel);
		transition.getChildren().add(anim3);

		// add given animation
		transition.getChildren().add(animation);

		// set on-finished action
		if (onFinished != null) {
			transition.setOnFinished(onFinished);
		}

		// play transition
		transition.play();
	}
}
