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

import java.awt.Color;
import java.awt.Rectangle;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYDifferenceRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.renderer.xy.YIntervalRenderer;
import org.jfree.chart.urls.StandardXYURLGenerator;
import org.jfree.chart.util.ParamChecks;
import org.jfree.data.xy.XYDataset;

/**
 * Class for crosshair listener plot.
 *
 * @author Murat Artim
 * @date Sep 2, 2014
 * @time 2:25:06 PM
 */
public class CrosshairListenerXYPlot extends XYPlot {

	/** Serial ID. */
	private static final long serialVersionUID = 1L;

	/** Cross hair listener. */
	private final CrosshairListener listener_;

	/** Change count. */
	private int change_ = 0;

	/**
	 * Creates crosshair listener plot.
	 *
	 * @param dataset
	 *            Data set.
	 * @param xAxis
	 *            X axis.
	 * @param yAxis
	 *            Y axis.
	 * @param renderer
	 *            Renderer.
	 * @param listener
	 *            Crosshair listener.
	 */
	public CrosshairListenerXYPlot(XYDataset dataset, NumberAxis xAxis, NumberAxis yAxis, XYItemRenderer renderer, CrosshairListener listener) {
		super(dataset, xAxis, yAxis, renderer);
		listener_ = listener;
	}

	@Override
	public void setDomainCrosshairValue(double value, boolean notify) {
		super.setDomainCrosshairValue(value, notify);
		change_++;
		if (change_ == 2) {
			listener_.crosshairValueChanged(value, getRangeCrosshairValue());
			change_ = 0;
		}
	}

	@Override
	public void setRangeCrosshairValue(double value, boolean notify) {
		super.setRangeCrosshairValue(value, notify);
		change_++;
		if (change_ == 2) {
			listener_.crosshairValueChanged(getDomainCrosshairValue(), value);
			change_ = 0;
		}
	}

	public static JFreeChart createXYLineChart(String title, String xAxisLabel, String yAxisLabel, XYDataset dataset, PlotOrientation orientation,
			boolean legend, boolean tooltips, boolean urls, CrosshairListener listener) {

		ParamChecks.nullNotPermitted(orientation, "orientation");
		NumberAxis xAxis = new NumberAxis(xAxisLabel);
		xAxis.setAutoRangeIncludesZero(false);
		NumberAxis yAxis = new NumberAxis(yAxisLabel);
		XYItemRenderer renderer = new XYLineAndShapeRenderer(true, false);
		XYPlot plot = listener == null ? new XYPlot(dataset, xAxis, yAxis, renderer)
				: new CrosshairListenerXYPlot(dataset, xAxis, yAxis, renderer, listener);
		plot.setOrientation(orientation);
		if (tooltips) {
			renderer.setBaseToolTipGenerator(new StandardXYToolTipGenerator());
		}
		if (urls) {
			renderer.setURLGenerator(new StandardXYURLGenerator());
		}

		JFreeChart chart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, legend);
		ChartFactory.getChartTheme().apply(chart);
		return chart;
	}

	public static JFreeChart createXYDifferenceChart(String title, String xAxisLabel, String yAxisLabel, XYDataset dataset,
			PlotOrientation orientation, boolean legend, boolean tooltips, boolean urls, CrosshairListener listener) {

		// setup to not accept nulls for orientation
		ParamChecks.nullNotPermitted(orientation, "orientation");

		// setup axes
		NumberAxis xAxis = new NumberAxis(xAxisLabel);
		xAxis.setAutoRangeIncludesZero(false);
		NumberAxis yAxis = new NumberAxis(yAxisLabel);

		// create first renderer
		XYDifferenceRenderer renderer1 = new XYDifferenceRenderer(new Color(211, 211, 211, 180), new Color(178, 34, 34), false);
		renderer1.setRoundXCoordinates(true);
		if (tooltips)
			renderer1.setBaseToolTipGenerator(new StandardXYToolTipGenerator());
		if (urls)
			renderer1.setURLGenerator(new StandardXYURLGenerator());

		// create second renderer
		XYItemRenderer renderer2 = new XYLineAndShapeRenderer(true, false);
		if (tooltips)
			renderer2.setBaseToolTipGenerator(new StandardXYToolTipGenerator());
		if (urls)
			renderer2.setURLGenerator(new StandardXYURLGenerator());

		// create plot
		XYPlot plot = listener == null ? new XYPlot(dataset, xAxis, yAxis, null) : new CrosshairListenerXYPlot(dataset, xAxis, yAxis, null, listener);
		plot.setRenderer(renderer1);
		plot.setRenderer(1, renderer2);
		plot.setOrientation(orientation);
		plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);

		// create chart
		JFreeChart chart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, legend);
		ChartFactory.getChartTheme().apply(chart);
		return chart;
	}

	public static JFreeChart createMissionProfileChart(String title, String xAxisLabel, String yAxisLabel, XYDataset dataset,
			PlotOrientation orientation, boolean legend, boolean tooltips, boolean urls, CrosshairListener listener) {

		// setup to not accept nulls for orientation
		ParamChecks.nullNotPermitted(orientation, "orientation");

		// setup axes
		NumberAxis xAxis = new NumberAxis(xAxisLabel);
		xAxis.setAutoRangeIncludesZero(false);
		NumberAxis yAxis = new NumberAxis(yAxisLabel);

		// create first renderer
		YIntervalRenderer renderer1 = new YIntervalRenderer();
		if (tooltips)
			renderer1.setBaseToolTipGenerator(new StandardXYToolTipGenerator());
		if (urls)
			renderer1.setURLGenerator(new StandardXYURLGenerator());
		for (int i = 0; i < 2; i++)
			renderer1.setSeriesShape(i, new Rectangle());

		// create second renderer
		XYItemRenderer renderer2 = new XYLineAndShapeRenderer(true, false);
		if (tooltips)
			renderer2.setBaseToolTipGenerator(new StandardXYToolTipGenerator());
		if (urls)
			renderer2.setURLGenerator(new StandardXYURLGenerator());

		// create third renderer
		XYItemRenderer renderer3 = new XYLineAndShapeRenderer(false, false);
		if (tooltips)
			renderer3.setBaseToolTipGenerator(new StandardXYToolTipGenerator());
		if (urls)
			renderer3.setURLGenerator(new StandardXYURLGenerator());

		// create plot
		XYPlot plot = listener == null ? new XYPlot(dataset, xAxis, yAxis, null) : new CrosshairListenerXYPlot(dataset, xAxis, yAxis, null, listener);
		plot.setRenderer(renderer1);
		plot.setRenderer(1, renderer2);
		plot.setRenderer(2, renderer3);
		plot.setOrientation(orientation);
		plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);

		// create chart
		JFreeChart chart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, legend);
		ChartFactory.getChartTheme().apply(chart);
		return chart;
	}

	/**
	 * Interface for crosshair listener.
	 *
	 * @author Murat Artim
	 * @date Sep 2, 2014
	 * @time 3:44:56 PM
	 */
	public interface CrosshairListener {

		/**
		 * Called when crosshair value changed.
		 *
		 * @param x
		 *            X value.
		 * @param y
		 *            Y value.
		 */
		void crosshairValueChanged(double x, double y);
	}
}
