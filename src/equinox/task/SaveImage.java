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

import java.io.File;

import javax.imageio.ImageIO;

import equinox.task.InternalEquinoxTask.ShortRunningTask;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.WritableImage;

/**
 * Class for save image task.
 *
 * @author Murat Artim
 * @date Jan 7, 2014
 * @time 10:57:47 AM
 */
public class SaveImage extends InternalEquinoxTask<Void> implements ShortRunningTask {

	/** Output file. */
	private final File output_;

	/** Plot image. */
	private final WritableImage image_;

	/**
	 * Creates save image task.
	 *
	 * @param output
	 *            Output file.
	 * @param image
	 *            Plot image.
	 */
	public SaveImage(File output, WritableImage image) {
		output_ = output;
		image_ = image;
	}

	@Override
	public String getTaskTitle() {
		return "Save image";
	}

	@Override
	public boolean canBeCancelled() {
		return false;
	}

	@Override
	protected Void call() throws Exception {
		updateTitle("Saving image");
		updateMessage("Saving image to '" + output_.getName() + "'...");
		ImageIO.write(SwingFXUtils.fromFXImage(image_, null), "png", output_);
		return null;
	}
}
