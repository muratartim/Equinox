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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

/**
 * Utility class for readable byte channel wrapper.
 *
 * @author Murat Artim
 * @date May 15, 2016
 * @time 3:49:02 PM
 */
public final class RBCWrapper implements ReadableByteChannel {

	/** Download listener. */
	private final DownloadListener listener_;

	/** Expected data size. */
	private final long expectedSize_;

	/** Readable byte channel. */
	private final ReadableByteChannel rbc_;

	/** Read bytes. */
	private long readSoFar_;

	/**
	 * Creates readable byte channel wrapper.
	 *
	 * @param rbc
	 *            Readable byte channel.
	 * @param expectedSize
	 *            Expected data size.
	 * @param listener
	 *            Download listener.
	 */
	public RBCWrapper(ReadableByteChannel rbc, long expectedSize, DownloadListener listener) {
		this.listener_ = listener;
		this.expectedSize_ = expectedSize;
		this.rbc_ = rbc;
	}

	/**
	 * Returns the read bytes.
	 *
	 * @return The read bytes.
	 */
	public long getReadSoFar() {
		return readSoFar_;
	}

	@Override
	public boolean isOpen() {
		return rbc_.isOpen();
	}

	@Override
	public void close() throws IOException {
		rbc_.close();
	}

	@Override
	public int read(ByteBuffer bb) throws IOException {
		int n;
		double progress;
		if ((n = rbc_.read(bb)) > 0) {
			readSoFar_ += n;
			progress = expectedSize_ > 0 ? (double) readSoFar_ / (double) expectedSize_ * 100.0 : -1.0;
			listener_.setDownloadProgress(this, progress);
		}
		return n;
	}
}
