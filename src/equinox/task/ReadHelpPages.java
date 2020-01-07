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

import java.io.BufferedReader;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;

import org.controlsfx.control.textfield.TextFields;

import equinox.Equinox;
import equinox.data.ui.HelpItem;
import equinox.plugin.FileType;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

/**
 * Class for read help pages task.
 *
 * @author Murat Artim
 * @date May 11, 2014
 * @time 12:54:32 PM
 */
public class ReadHelpPages extends InternalEquinoxTask<ArrayList<TreeItem<String>>> implements ShortRunningTask {

	/** Help tree. */
	private final TreeView<String> helpTree_;

	/** Search text field. */
	private final TextField searchField_;

	/**
	 * Creates read help pages task.
	 *
	 * @param helpTree
	 *            Help tree.
	 * @param searchField
	 *            Search text field.
	 */
	public ReadHelpPages(TreeView<String> helpTree, TextField searchField) {
		helpTree_ = helpTree;
		searchField_ = searchField;
	}

	@Override
	public String getTaskTitle() {
		return "Load help pages";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	protected ArrayList<TreeItem<String>> call() throws Exception {

		// update progress info
		updateTitle("Loading help pages");

		// create array list
		ArrayList<TreeItem<String>> pages = new ArrayList<>();

		// create HTML file filter
		DirectoryStream.Filter<Path> filter = file -> {
			Path fileName = file.getFileName();
			if (fileName == null)
				return false;
			return !Files.isDirectory(file) && fileName.toString().endsWith(".html") && !fileName.toString().equals("EmptyPage.html");
		};

		// create directory stream
		try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(Equinox.HELP_DIR, filter)) {

			// get iterator
			Iterator<Path> iterator = dirStream.iterator();

			// loop over files
			while (iterator.hasNext()) {

				// get file
				Path file = iterator.next();

				// add to help tree
				addToTree(file, pages);
			}
		}

		// return pages
		return pages;
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// add pages to tree
		try {

			// get pages
			ArrayList<TreeItem<String>> pages = get();

			// set pages to help tree
			helpTree_.getRoot().getChildren().addAll(pages);

			// add pages to help search field
			addPagesToSearchField(pages);
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	/**
	 * Adds help pages to search text field. Note that, only root pages are added.
	 *
	 * @param pages
	 *            All help pages.
	 */
	private void addPagesToSearchField(ArrayList<TreeItem<String>> pages) {

		// create search field binding
		TextFields.bindAutoCompletion(searchField_, pages).setOnAutoCompleted(event -> {
			if (event == null)
				return;
			HelpItem item = (HelpItem) event.getCompletion();
			if (item == null)
				return;
			taskPanel_.getOwner().getOwner().showHelp(item.getPage(), item.getLocation());
		});
	}

	/**
	 * Adds given file to pages.
	 *
	 * @param file
	 *            Help file to add.
	 * @param pages
	 *            Pages.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static void addToTree(Path file, ArrayList<TreeItem<String>> pages) throws Exception {

		// create page
		HelpItem item = new HelpItem(FileType.getNameWithoutExtension(file), null);

		// create file reader
		try (BufferedReader reader = Files.newBufferedReader(file, Charset.defaultCharset())) {

			// read file till the end
			String line;
			while ((line = reader.readLine()) != null) {

				// header
				if (line.trim().startsWith("<h2>")) {
					int index1 = line.indexOf("\"");
					int index2 = line.lastIndexOf("\"");
					item.getChildren().add(new HelpItem(item.getPage(), line.substring(index1 + 1, index2)));
				}
			}
		}

		// add page to tree
		pages.add(item);
	}
}
