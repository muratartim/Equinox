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

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

import equinox.Equinox;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;

/**
 * Class for add files to file list task.
 *
 * @author Murat Artim
 * @date Jan 8, 2015
 * @time 1:31:42 PM
 */
public class AddFiles extends Task<ArrayList<TreeItem<String>>> {

	/** Root node of file tree. */
	private final TreeItem<String> root_;

	/** List of files. */
	private final ObservableList<TreeItem<String>> files_;

	/** File list component. */
	private final ListView<TreeItem<String>> fileList_;

	/** File search field. */
	private final TextField search_;

	/**
	 * Creates add files to file list task.
	 *
	 * @param root
	 *            Root node of file tree.
	 * @param files
	 *            List of files.
	 * @param fileList
	 *            File list component.
	 * @param search
	 *            File search field.
	 */
	public AddFiles(TreeItem<String> root, ObservableList<TreeItem<String>> files, ListView<TreeItem<String>> fileList, TextField search) {
		root_ = root;
		files_ = files;
		fileList_ = fileList;
		search_ = search;
	}

	@Override
	protected ArrayList<TreeItem<String>> call() throws Exception {
		ArrayList<TreeItem<String>> list = new ArrayList<>();
		addFiles(root_, list);
		return list;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// update UI components
		try {
			files_.setAll(get());
			fileList_.setItems(files_);
			search_.clear();
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			Equinox.LOGGER.log(Level.WARNING, "Exception occurred during adding files to file list: ", e);
		}
	}

	@Override
	protected void failed() {

		// call ancestor
		super.failed();

		// log exception
		Equinox.LOGGER.log(Level.WARNING, "Exception occurred during adding files to file list: ", getException());
	}

	/**
	 * Adds files from the file tree recursively to the file list.
	 *
	 * @param treeItem
	 *            Start file to add.
	 * @param list
	 *            List to add files.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void addFiles(TreeItem<String> treeItem, ArrayList<TreeItem<String>> list) throws Exception {

		// null values
		if (treeItem == null || list == null)
			return;

		// add to files (if not root)
		if (!treeItem.equals(root_)) {
			list.add(treeItem);
		}

		// get children
		ObservableList<TreeItem<String>> children = treeItem.getChildren();
		if (children == null)
			return;

		// add files
		for (TreeItem<String> item : children) {
			addFiles(item, list);
		}
	}
}
