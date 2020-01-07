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
package equinox.data;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;

import equinox.dataServer.remote.data.ServerPluginInfo.PluginInfoType;
import javafx.scene.image.Image;

/**
 * Class for client plugin info.
 *
 * @author Murat Artim
 * @date Mar 31, 2015
 * @time 9:57:33 AM
 */
public class ClientPluginInfo {

	/** Map containing the info. */
	private final HashMap<PluginInfoType, Object> info_ = new HashMap<>();

	/** Array containing the pixels of the image. */
	private byte[] imageBytes_;

	/** Sample inputs archive. */
	private File sampleInputs_;

	/**
	 * Sets help video info.
	 *
	 * @param type
	 *            Info type.
	 * @param info
	 *            Info to set.
	 */
	public void setInfo(PluginInfoType type, Object info) {
		info_.put(type, info);
	}

	/**
	 * Reads the given image file and sets its bytes.
	 *
	 * @param imageFile
	 *            Image file to read.
	 * @throws IOException
	 *             If exception occurs during reading and retrieving bytes of image.
	 */
	public void setImage(File imageFile) throws IOException {
		imageBytes_ = new byte[(int) imageFile.length()];
		try (ImageInputStream imgStream = ImageIO.createImageInputStream(imageFile)) {
			imgStream.read(imageBytes_);
		}
	}

	/**
	 * Sets sample inputs of the plugin.
	 *
	 * @param sampleInputs
	 *            Archive containing the sample inputs of the plugin.
	 */
	public void setSampleInputs(File sampleInputs) {
		sampleInputs_ = sampleInputs;
	}

	/**
	 * Returns the demanded help video info.
	 *
	 * @param type
	 *            Info type.
	 * @return The demanded help video info.
	 */
	public Object getInfo(PluginInfoType type) {
		return info_.get(type);
	}

	/**
	 * Returns image or null if there is no image.
	 *
	 * @return Image.
	 */
	public Image getImage() {
		return imageBytes_ == null ? null : new Image(new ByteArrayInputStream(imageBytes_));
	}

	/**
	 * Returns image bytes.
	 *
	 * @return Image bytes.
	 */
	public byte[] getImageBytes() {
		return imageBytes_;
	}

	/**
	 * Returns sample inputs of the plugin.
	 *
	 * @return Sample inputs of the plugin.
	 */
	public File getSampleInputs() {
		return sampleInputs_;
	}
}
