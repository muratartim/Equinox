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
import java.nio.file.Files;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang3.RandomUtils;

import equinox.Equinox;
import equinox.controller.ViewPanel;
import equinox.controller.WebViewPanel;
import equinox.task.InternalEquinoxTask.ShortRunningTask;
import equinox.utility.DownloadListener;
import equinox.utility.RBCWrapper;

/**
 * Class for show news feed task.
 *
 * @author Murat Artim
 * @date 21 Aug 2017
 * @time 12:09:16
 *
 */
public class ShowNewsFeed extends TemporaryFileCreatingTask<String> implements ShortRunningTask, DownloadListener {

	/** Background color. */
	private String bgColor_ = "transparent";

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	public String getTaskTitle() {
		return "Show newsfeed";
	}

	@Override
	public void setDownloadProgress(RBCWrapper rbc, double progress) {
		updateProgress(progress, 100.0);
	}

	@Override
	protected String call() throws Exception {

		// download feed mapping from web server
		updateMessage("Loading newsfeed...");

		// read the news feed mapping
		HashMap<String, String> pages = new HashMap<>();
		try (BufferedReader reader = Files.newBufferedReader(Equinox.NEWSFEED_MAP_FILE, Charset.defaultCharset())) {

			// read file till the end
			String line;
			while ((line = reader.readLine()) != null) {

				// empty line
				if (line.trim().isEmpty()) {
					continue;
				}

				// comment line
				if (line.trim().startsWith("//")) {
					continue;
				}

				// split page
				String[] split = line.trim().split("\t");

				// add page
				pages.put(split[0].trim(), split[1].trim());
			}
		}

		// randomly select a page
		String page = pages.keySet().toArray(new String[] {})[RandomUtils.nextInt(0, pages.size())];
		bgColor_ = pages.get(page);
		page = page.replaceAll("\\s", "%20");

		// return address to page
		return Equinox.NEWSFEED_DIR.resolve(page).toAbsolutePath().toUri().toString();
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		try {

			// get newsfeed page address
			String address = get();

			// show page
			WebViewPanel panel = (WebViewPanel) taskPanel_.getOwner().getOwner().getViewPanel().getSubPanel(ViewPanel.WEB_VIEW);
			panel.showAddress(address, null, "Newsfeed", bgColor_);
			taskPanel_.getOwner().getOwner().getViewPanel().showSubPanel(ViewPanel.WEB_VIEW);
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}
}
