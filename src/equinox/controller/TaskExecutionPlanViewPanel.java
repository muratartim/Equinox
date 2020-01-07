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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.ResourceBundle;

import javax.swing.SwingUtilities;

import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.TreeLayout;
import edu.uci.ics.jung.graph.DelegateForest;
import edu.uci.ics.jung.graph.Forest;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import equinox.controller.ViewPanel.InternalViewSubPanel;
import equinox.data.EmbeddedTask;
import equinox.data.EquinoxTheme;
import equinox.data.ExecutionMode;
import equinox.data.InstructedTask;
import equinox.plugin.FileType;
import equinox.task.InternalEquinoxTask;
import equinox.task.SaveImage;
import equinox.task.automation.AutomaticTask;
import equinox.task.automation.AutomaticTaskOwner;
import equinox.task.automation.RunInstructionSet;
import equinox.utility.Utility;
import javafx.embed.swing.SwingNode;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 * Class for graph view panel controller.
 *
 * @author Murat Artim
 * @date 23 Sep 2018
 * @time 15:25:37
 */
public class TaskExecutionPlanViewPanel implements InternalViewSubPanel {

	/** The owner panel. */
	private ViewPanel owner_;

	/** Edge index. */
	private Integer edgeIndex = 0;

	@FXML
	private VBox root_;

	@FXML
	private SwingNode container_;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// no implementation
	}

	@Override
	public ViewPanel getOwner() {
		return owner_;
	}

	@Override
	public Parent getRoot() {
		return root_;
	}

	@Override
	public HBox getControls() {
		return null;
	}

	@Override
	public String getHeader() {
		return "Execution Plan View";
	}

	@Override
	public boolean canSaveView() {
		return true;
	}

	@Override
	public void saveView() {

		// get file chooser
		FileChooser fileChooser = owner_.getOwner().getFileChooser(FileType.PNG.getExtensionFilter());

		// show save dialog
		fileChooser.setInitialFileName("Execution Plan" + FileType.PNG.getExtension());
		File selectedFile = fileChooser.showSaveDialog(owner_.getOwner().getOwner().getStage());

		// no file selected
		if (selectedFile == null)
			return;

		// set initial directory
		owner_.getOwner().setInitialDirectory(selectedFile);

		// append extension if necessary
		File file = FileType.appendExtension(selectedFile, FileType.PNG);

		// take snapshot of the scene
		WritableImage snapshot = container_.snapshot(null, null);

		// create and start task in progress panel
		owner_.getOwner().getActiveTasksPanel().runTaskInParallel(new SaveImage(file, snapshot));
	}

	@Override
	public String getViewName() {
		return "Execution Plan";
	}

	@Override
	public WritableImage getViewImage() {
		return container_.snapshot(null, null);
	}

	@Override
	public void start() {
		// no implementation
	}

	@Override
	public void showing() {
		// no implementation
	}

	@Override
	public void hiding() {
		// no implementation
	}

	/**
	 * Sets automatic tasks to generate and plot the execution plan.
	 *
	 * @param rootTask
	 *            Root task.
	 * @param automaticTasks
	 *            Automatic tasks.
	 */
	public void setAutomaticTasks(RunInstructionSet rootTask, HashMap<String, InstructedTask> automaticTasks) {

		// no tasks found
		if (automaticTasks == null || automaticTasks.isEmpty())
			return;

		// create delegate forest graph
		Graph<InternalEquinoxTask<?>, Integer> graph = new DelegateForest<>();

		// add root task
		graph.addVertex(rootTask);

		// loop over tasks
		Iterator<Entry<String, InstructedTask>> iterator = automaticTasks.entrySet().iterator();
		while (iterator.hasNext()) {

			// get instructed task
			InstructedTask instructedTask = iterator.next().getValue();

			// embedded
			if (instructedTask.isEmbedded()) {
				continue;
			}

			// get task
			InternalEquinoxTask<?> task = instructedTask.getTask();

			// add task to graph
			graph.addVertex(task);
			graph.addEdge(edgeIndex, rootTask, task);
			edgeIndex++;

			// automatic task owner (add automatic tasks)
			if (task instanceof AutomaticTaskOwner) {
				addAutomaticTasksToGraph((AutomaticTaskOwner<?>) task, graph);
			}

			// add follower tasks to graph (if any)
			addFollowerTasksToGraph(task, graph);
		}

		// create layout
		Layout<InternalEquinoxTask<?>, Integer> layout = new TreeLayout<>((Forest<InternalEquinoxTask<?>, Integer>) graph);

		// create viewer
		VisualizationViewer<InternalEquinoxTask<?>, Integer> viewer = new VisualizationViewer<>(layout);

		// set edge label transformer (no label display for edges)
		viewer.getRenderContext().setEdgeLabelTransformer(arg0 -> "");

		// set vertex label transformer (simple class names to be displayed)
		viewer.getRenderContext().setVertexLabelTransformer(arg0 -> arg0.getClass().getSimpleName());

		// create and set mouse adapter
		DefaultModalGraphMouse<InternalEquinoxTask<?>, Integer> mouseAdapter = new DefaultModalGraphMouse<>();
		viewer.setGraphMouse(mouseAdapter);
		mouseAdapter.setMode(ModalGraphMouse.Mode.TRANSFORMING);

		// create swing node content
		SwingUtilities.invokeLater(() -> container_.setContent(viewer));

	}

	/**
	 * Adds follower tasks of the given task to graph.
	 *
	 * @param sourceTask
	 *            Source task.
	 * @param graph
	 *            Task forest graph.
	 */
	private void addFollowerTasksToGraph(InternalEquinoxTask<?> sourceTask, Graph<InternalEquinoxTask<?>, Integer> graph) {

		// no follower task
		if (sourceTask.getFollowerTasks() == null || sourceTask.getFollowerTasks().isEmpty())
			return;

		// loop over follower tasks
		Iterator<Entry<InternalEquinoxTask<?>, ExecutionMode>> iterator = sourceTask.getFollowerTasks().entrySet().iterator();
		while (iterator.hasNext()) {

			// get entry
			Entry<InternalEquinoxTask<?>, ExecutionMode> entry = iterator.next();

			// already in graph
			if (graph.containsVertex(entry.getKey())) {
				continue;
			}

			// add to graph
			graph.addVertex(entry.getKey());
			graph.addEdge(edgeIndex, sourceTask, entry.getKey());
			edgeIndex++;

			// automatic task owner (add automatic tasks)
			if (entry.getKey() instanceof AutomaticTaskOwner) {
				addAutomaticTasksToGraph((AutomaticTaskOwner<?>) entry.getKey(), graph);
			}

			// add follower tasks (if any)
			addFollowerTasksToGraph(entry.getKey(), graph);
		}
	}

	/**
	 * Adds automatic tasks of the given task to graph.
	 *
	 * @param sourceTask
	 *            Source task.
	 * @param graph
	 *            Task forest graph.
	 */
	@SuppressWarnings("rawtypes")
	private void addAutomaticTasksToGraph(AutomaticTaskOwner sourceTask, Graph<InternalEquinoxTask<?>, Integer> graph) {

		// no automatic task
		if (sourceTask.getAutomaticTasks() == null || sourceTask.getAutomaticTasks().isEmpty())
			return;

		// loop over automatic tasks
		Iterator<EmbeddedTask> iterator = sourceTask.getAutomaticTasks().values().iterator();
		while (iterator.hasNext()) {

			// get task
			AutomaticTask task = iterator.next().getTask();

			// already in graph
			if (graph.containsVertex((InternalEquinoxTask<?>) task)) {
				continue;
			}

			// add to graph
			graph.addVertex((InternalEquinoxTask<?>) task);
			graph.addEdge(edgeIndex, (InternalEquinoxTask<?>) sourceTask, (InternalEquinoxTask<?>) task);
			edgeIndex++;

			// automatic task owner (add automatic tasks)
			if (task instanceof AutomaticTaskOwner) {
				addAutomaticTasksToGraph((AutomaticTaskOwner) task, graph);
			}

			// add follower tasks (if any)
			addFollowerTasksToGraph((InternalEquinoxTask<?>) task, graph);
		}
	}

	/**
	 * Loads and returns plot column panel.
	 *
	 * @param owner
	 *            The owner panel.
	 * @return The newly loaded plot column panel.
	 */
	public static TaskExecutionPlanViewPanel load(ViewPanel owner) {

		try {

			// load fxml file
			FXMLLoader fxmlLoader = new FXMLLoader(EquinoxTheme.getFXMLResource("TaskExecutionPlanViewPanel.fxml"));
			Parent root = (Parent) fxmlLoader.load();

			// speed caching
			Utility.setupSpeedCaching(root, null);

			// get controller
			TaskExecutionPlanViewPanel controller = (TaskExecutionPlanViewPanel) fxmlLoader.getController();

			// set owner
			controller.owner_ = owner;

			// return controller
			return controller;
		}

		// exception occurred during loading
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}