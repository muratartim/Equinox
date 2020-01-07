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

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import javax.imageio.ImageIO;

import equinox.Equinox;
import equinox.data.EmbeddedTask;
import equinox.data.Triple;
import equinox.data.fileType.STFFile;
import equinox.dataServer.remote.data.PilotPointImageType;
import equinox.plugin.FileType;
import equinox.process.SaveSTFFile;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import equinox.task.automation.AutomaticTaskOwner;
import equinox.task.automation.SingleInputTask;
import equinox.utility.Utility;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import jxl.CellType;
import jxl.Workbook;
import jxl.format.Border;
import jxl.format.BorderLineStyle;
import jxl.format.Colour;
import jxl.write.NumberFormats;
import jxl.write.WritableCellFormat;
import jxl.write.WritableFont;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;

/**
 * Class for export pilot point task.
 *
 * @author Murat Artim
 * @date Feb 5, 2016
 * @time 1:10:31 PM
 */
public class ExportSTF extends TemporaryFileCreatingTask<Path> implements LongRunningTask, SingleInputTask<Triple<STFFile, String[], HashMap<PilotPointImageType, Image>>>, AutomaticTaskOwner<Path> {

	/** STF file. */
	private STFFile stfFile_;

	/** Pilot point and spectrum names. */
	private String ppName_, spectrumName_, mission_, userSetMission_;

	/** Info array. */
	private String[] info_, userSetInfo_;

	/** Pilot point images. */
	private HashMap<PilotPointImageType, Image> images_, userSetImages_;

	/** Path to output ZIP file. */
	private final File output_;

	/** Automatic tasks. */
	private HashMap<String, EmbeddedTask<Path>> automaticTasks_ = null;

	/**
	 * Creates save pilot point info task.
	 *
	 * @param stfFile
	 *            STF file.
	 * @param ppName
	 *            Pilot point name.
	 * @param spectrumName
	 *            Spectrum name.
	 * @param mission
	 *            Fatigue mission.
	 * @param info
	 *            Info array.
	 * @param images
	 *            Pilot point images.
	 * @param output
	 *            Path to output ZIP file.
	 */
	public ExportSTF(STFFile stfFile, String ppName, String spectrumName, String mission, String[] info, HashMap<PilotPointImageType, Image> images, File output) {
		stfFile_ = stfFile;
		ppName_ = ppName;
		spectrumName_ = spectrumName;
		mission_ = mission;
		info_ = info;
		images_ = images;
		output_ = output;
	}

	/**
	 * Creates export pilot point task. Note that, this constructor is intended to be used for automatic task execution.
	 *
	 * @param output
	 *            Path to output ZIP file.
	 */
	public ExportSTF(File output) {
		output_ = output;
	}

	/**
	 * Sets fatigue mission.
	 *
	 * @param mission
	 *            Fatigue mission.
	 */
	public void setMission(String mission) {
		userSetMission_ = mission;
	}

	/**
	 * Sets pilot point info.
	 *
	 * @param info
	 *            Pilot point info.
	 */
	public void setInfo(String[] info) {
		userSetInfo_ = info;
	}

	/**
	 * Sets pilot point images.
	 *
	 * @param images
	 *            Images to set.
	 */
	public void setImages(HashMap<PilotPointImageType, Image> images) {
		userSetImages_ = images;
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	public String getTaskTitle() {
		return "Export pilot point";
	}

	@Override
	public void setAutomaticInput(Triple<STFFile, String[], HashMap<PilotPointImageType, Image>> input) {
		stfFile_ = input.getElement1();
		info_ = input.getElement2();
		images_ = input.getElement3();
		ppName_ = FileType.getNameWithoutExtension(stfFile_.getName());
		spectrumName_ = stfFile_.getParentItem().getName();
		mission_ = stfFile_.getMission();
	}

	@Override
	public void addAutomaticTask(String taskID, EmbeddedTask<Path> task) {
		if (automaticTasks_ == null) {
			automaticTasks_ = new HashMap<>();
		}
		automaticTasks_.put(taskID, task);
	}

	@Override
	public HashMap<String, EmbeddedTask<Path>> getAutomaticTasks() {
		return automaticTasks_;
	}

	@Override
	protected Path call() throws Exception {

		// check permission
		checkPermission(Permission.EXPORT_PILOT_POINT);

		// update progress info
		updateTitle("Exporting STF file to '" + output_.getName() + "'");

		// create array to store input files
		ArrayList<Path> inputFiles = new ArrayList<>();
		Path ppOutputDirectory = null;

		// get database connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// write info file
			inputFiles.add(writeInfoFile(connection));

			// task cancelled
			if (isCancelled())
				return null;

			// output directory
			ppOutputDirectory = Files.createDirectory(getWorkingDirectory().resolve("PP_0"));

			// write STF file
			new SaveSTFFile(this, stfFile_, ppOutputDirectory.resolve(ppName_ + FileType.STF.getExtension())).start(connection);
		}

		// task cancelled
		if (isCancelled())
			return null;

		// write images
		Iterator<Entry<PilotPointImageType, Image>> iterator = getImages();
		while (iterator.hasNext()) {

			// write image file
			writeImageFile(ppOutputDirectory, iterator.next());

			// task cancelled
			if (isCancelled())
				return null;
		}

		// add directory to input files
		inputFiles.add(ppOutputDirectory);

		// zip exported files
		updateMessage("Zipping exported files...");
		Utility.zipFiles(inputFiles, output_, this);

		// return output file
		return output_.toPath();
	}

	@Override
	protected void succeeded() {

		// call ancestor
		super.succeeded();

		// set file info
		try {

			// get output path
			Path output = get();

			// manage automatic tasks
			automaticTaskOwnerSucceeded(output, automaticTasks_, taskPanel_);
		}

		// exception occurred
		catch (InterruptedException | ExecutionException e) {
			handleResultRetrievalException(e);
		}
	}

	@Override
	protected void failed() {

		// call ancestor
		super.failed();

		// manage automatic tasks
		automaticTaskOwnerFailed(automaticTasks_);
	}

	@Override
	protected void cancelled() {

		// call ancestor
		super.cancelled();

		// manage automatic tasks
		automaticTaskOwnerFailed(automaticTasks_);
	}

	/**
	 * Returns image mapping.
	 *
	 * @return Image mapping.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Iterator<Entry<PilotPointImageType, Image>> getImages() throws Exception {

		// no user set image
		if (userSetImages_ == null)
			return images_.entrySet().iterator();

		// no database-found image
		if (images_ == null)
			return userSetImages_.entrySet().iterator();

		// create new image mapping
		HashMap<PilotPointImageType, Image> images = new HashMap<>();
		if (images_ != null) {
			images.putAll(images_);
		}
		if (userSetImages_ != null) {
			images.putAll(userSetImages_);
		}

		// return images
		return images.entrySet().iterator();
	}

	/**
	 * Writes out image file to working directory.
	 *
	 * @param ppOutputDirectory
	 *            Pilot point output directory.
	 * @param image
	 *            Image.
	 * @return Path to image file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Path writeImageFile(Path ppOutputDirectory, Entry<PilotPointImageType, Image> image) throws Exception {

		// update progress info
		updateMessage("Writing image file...");

		// create image file
		Path imageFile = ppOutputDirectory.resolve(image.getKey().getFileName());

		// create buffered image
		BufferedImage bufImg = SwingFXUtils.fromFXImage(image.getValue(), null);

		// write image to file
		ImageIO.write(bufImg, "png", imageFile.toFile());

		// return file
		return imageFile;
	}

	/**
	 * Writes out info XLS file to working directory.
	 *
	 * @param connection
	 *            Database connection.
	 * @return Path to info XLS file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private Path writeInfoFile(Connection connection) throws Exception {

		// update progress info
		updateMessage("Writing info Excel file...");

		// create info file
		Path infoFile = getWorkingDirectory().resolve("Pilot_Point_Info.xls");

		// declare workbook
		WritableWorkbook workbook = null;

		try {

			// create workbook
			workbook = Workbook.createWorkbook(infoFile.toFile());

			// create pilot point info worksheet
			WritableSheet sheet = workbook.createSheet("Pilot Point Info", 0);

			// write headers
			writeHeaders(sheet);

			// write info
			writeInfo(sheet);

			// write data
			workbook.write();
		}

		// close workbook
		finally {
			if (workbook != null) {
				workbook.close();
			}
		}

		// return info file
		return infoFile;
	}

	/**
	 * Writes table info.
	 *
	 * @param sheet
	 *            Worksheet.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeInfo(WritableSheet sheet) throws Exception {

		// initialize column index
		int column = 0;

		// directory name
		sheet.addCell(new jxl.write.Label(column, 1, "PP_0", getDataFormat(1, CellType.LABEL, false)));
		column++;

		// pilot point name
		sheet.addCell(new jxl.write.Label(column, 1, ppName_, getDataFormat(1, CellType.LABEL, false)));
		column++;

		// spectrum name
		sheet.addCell(new jxl.write.Label(column, 1, spectrumName_, getDataFormat(1, CellType.LABEL, false)));
		column++;

		// aircraft program
		sheet.addCell(new jxl.write.Label(column, 1, stfFile_.getParentItem().getProgram(), getDataFormat(1, CellType.LABEL, false)));
		column++;

		// aircraft section
		sheet.addCell(new jxl.write.Label(column, 1, stfFile_.getParentItem().getSection(), getDataFormat(1, CellType.LABEL, false)));
		column++;

		// fatigue mission
		String mission = userSetMission_ == null ? mission_ : userSetMission_;
		sheet.addCell(new jxl.write.Label(column, 1, mission, getDataFormat(1, CellType.LABEL, false)));
		column++;

		// get info
		String[] info = getInfo();

		// description
		sheet.addCell(new jxl.write.Label(column, 1, info[GetSTFInfo2.DESCRIPTION], getDataFormat(1, CellType.LABEL, false)));
		column++;

		// data source
		sheet.addCell(new jxl.write.Label(column, 1, info[GetSTFInfo2.DATA_SOURCE], getDataFormat(1, CellType.LABEL, false)));
		column++;

		// generation source
		sheet.addCell(new jxl.write.Label(column, 1, info[GetSTFInfo2.GEN_SOURCE], getDataFormat(1, CellType.LABEL, false)));
		column++;

		// delivery reference
		String deliveryRef = info[GetSTFInfo2.DELIVERY_REF] == null ? "DRAFT" : info[GetSTFInfo2.DELIVERY_REF];
		sheet.addCell(new jxl.write.Label(column, 1, deliveryRef, getDataFormat(1, CellType.LABEL, false)));
		column++;

		// pilot point issue
		sheet.addCell(new jxl.write.Label(column, 1, info[GetSTFInfo2.ISSUE], getDataFormat(1, CellType.LABEL, false)));
		column++;

		// element ID
		sheet.addCell(new jxl.write.Label(column, 1, info[GetSTFInfo2.EID], getDataFormat(1, CellType.LABEL, false)));
		column++;

		// element type
		sheet.addCell(new jxl.write.Label(column, 1, info[GetSTFInfo2.ELEMENT_TYPE], getDataFormat(1, CellType.LABEL, false)));
		column++;

		// frame/rib position
		sheet.addCell(new jxl.write.Label(column, 1, info[GetSTFInfo2.FRAME_RIB_POS], getDataFormat(1, CellType.LABEL, false)));
		column++;

		// stringer position
		sheet.addCell(new jxl.write.Label(column, 1, info[GetSTFInfo2.STRINGER_POS], getDataFormat(1, CellType.LABEL, false)));
		column++;

		// fatigue material
		sheet.addCell(new jxl.write.Label(column, 1, info[GetSTFInfo2.FATIGUE_MATERIAL], getDataFormat(1, CellType.LABEL, false)));
		column++;

		// preffas material
		sheet.addCell(new jxl.write.Label(column, 1, info[GetSTFInfo2.PREFFAS_MATERIAL], getDataFormat(1, CellType.LABEL, false)));
		column++;

		// linear material
		sheet.addCell(new jxl.write.Label(column, 1, info[GetSTFInfo2.LINEAR_MATERIAL], getDataFormat(1, CellType.LABEL, false)));
	}

	/**
	 * Returns pilot point info array.
	 *
	 * @return Pilot point info array.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private String[] getInfo() throws Exception {

		// no user set info
		if (userSetInfo_ == null || userSetInfo_.length == 0)
			return checkInfo(info_);

		// create info array
		String[] info = new String[info_.length];
		for (int i = 0; i < info_.length; i++) {
			info[i] = userSetInfo_[i] == null || userSetInfo_[i].trim().isEmpty() ? info_[i] : userSetInfo_[i];
		}

		// return info array
		return checkInfo(info);
	}

	/**
	 * Checks pilot point info array for required attributes.
	 *
	 * @param info
	 *            Info array to check.
	 * @return Info array.
	 * @throws IllegalArgumentException
	 *             If check fails.
	 */
	private static String[] checkInfo(String[] info) throws IllegalArgumentException {

		// null
		if (info == null || info.length != 12)
			throw new IllegalArgumentException("Invalid pilot point info supplied for exporting pilot point. Export aborted.");

		// check required attributes
		if (info[GetSTFInfo2.DESCRIPTION] == null || info[GetSTFInfo2.DESCRIPTION].trim().isEmpty())
			throw new IllegalArgumentException("Description is obligatory to export pilot point. Export aborted.");
		if (info[GetSTFInfo2.DATA_SOURCE] == null || info[GetSTFInfo2.DESCRIPTION].trim().isEmpty())
			throw new IllegalArgumentException("Data source is obligatory to export pilot point. Export aborted.");
		if (info[GetSTFInfo2.GEN_SOURCE] == null || info[GetSTFInfo2.DESCRIPTION].trim().isEmpty())
			throw new IllegalArgumentException("Generation source is obligatory to export pilot point. Export aborted.");
		if (info[GetSTFInfo2.ISSUE] == null || info[GetSTFInfo2.DESCRIPTION].trim().isEmpty())
			throw new IllegalArgumentException("Pilot point issue is obligatory to export pilot point. Export aborted.");

		// return info
		return info;
	}

	/**
	 * Writes table headers.
	 *
	 * @param sheet
	 *            Worksheet.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static void writeHeaders(WritableSheet sheet) throws Exception {

		// get header format
		WritableCellFormat format = getHeaderFormat();

		// initialize column index
		int column = 0;

		// pilot point directory name
		String header = "Directory Name";
		sheet.addCell(new jxl.write.Label(column, 0, header, format));
		sheet.setColumnView(column, header.length());
		column++;

		// pilot point name
		header = "Pilot Point Name";
		sheet.addCell(new jxl.write.Label(column, 0, header, format));
		sheet.setColumnView(column, header.length());
		column++;

		// spectrum name
		header = "Spectrum Name";
		sheet.addCell(new jxl.write.Label(column, 0, header, format));
		sheet.setColumnView(column, header.length());
		column++;

		// aircraft program
		header = "Aircraft Program";
		sheet.addCell(new jxl.write.Label(column, 0, header, format));
		sheet.setColumnView(column, header.length());
		column++;

		// aircraft section
		header = "Aircraft Section";
		sheet.addCell(new jxl.write.Label(column, 0, header, format));
		sheet.setColumnView(column, header.length());
		column++;

		// fatigue mission
		header = "Fatigue Mission";
		sheet.addCell(new jxl.write.Label(column, 0, header, format));
		sheet.setColumnView(column, header.length());
		column++;

		// description
		header = "Description";
		sheet.addCell(new jxl.write.Label(column, 0, header, format));
		sheet.setColumnView(column, header.length());
		column++;

		// data source
		header = "Data Source";
		sheet.addCell(new jxl.write.Label(column, 0, header, format));
		sheet.setColumnView(column, header.length());
		column++;

		// generation source
		header = "Generation Source";
		sheet.addCell(new jxl.write.Label(column, 0, header, format));
		sheet.setColumnView(column, header.length());
		column++;

		// delivery reference
		header = "Delivery Reference";
		sheet.addCell(new jxl.write.Label(column, 0, header, format));
		sheet.setColumnView(column, header.length());
		column++;

		// pilot point issue
		header = "Pilot Point Issue";
		sheet.addCell(new jxl.write.Label(column, 0, header, format));
		sheet.setColumnView(column, header.length());
		column++;

		// element ID
		header = "EID/LIQ/SG";
		sheet.addCell(new jxl.write.Label(column, 0, header, format));
		sheet.setColumnView(column, header.length());
		column++;

		// element type
		header = "Element Type";
		sheet.addCell(new jxl.write.Label(column, 0, header, format));
		sheet.setColumnView(column, header.length());
		column++;

		// frame/rib position
		header = "Frame/Rib Position";
		sheet.addCell(new jxl.write.Label(column, 0, header, format));
		sheet.setColumnView(column, header.length());
		column++;

		// stringer position
		header = "Stringer Position";
		sheet.addCell(new jxl.write.Label(column, 0, header, format));
		sheet.setColumnView(column, header.length());
		column++;

		// fatigue material
		header = "Fatigue Material";
		sheet.addCell(new jxl.write.Label(column, 0, header, format));
		sheet.setColumnView(column, header.length());
		column++;

		// preffas material
		header = "Preffas Material";
		sheet.addCell(new jxl.write.Label(column, 0, header, format));
		sheet.setColumnView(column, header.length());
		column++;

		// linear material
		header = "Linear Material";
		sheet.addCell(new jxl.write.Label(column, 0, header, format));
		sheet.setColumnView(column, header.length());
	}

	/**
	 * Returns header format.
	 *
	 * @return Header format.
	 * @throws WriteException
	 *             If exception occurs during process.
	 */
	private static WritableCellFormat getHeaderFormat() throws WriteException {
		WritableFont cellFont = new WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD);
		WritableCellFormat cellFormat = new WritableCellFormat(cellFont);
		cellFormat.setBorder(Border.ALL, BorderLineStyle.THIN);
		cellFormat.setBackground(Colour.ORANGE);
		return cellFormat;
	}

	/**
	 * Returns data format.
	 *
	 * @param rowIndex
	 *            Row index.
	 * @param ct
	 *            Cell type.
	 * @param isScientific
	 *            True if scientific format.
	 * @return Data format.
	 * @throws WriteException
	 *             If exception occurs during process.
	 */
	private static WritableCellFormat getDataFormat(int rowIndex, CellType ct, boolean isScientific) throws WriteException {
		WritableCellFormat cellFormat = ct == CellType.NUMBER ? new WritableCellFormat(isScientific ? NumberFormats.EXPONENTIAL : NumberFormats.FLOAT) : new WritableCellFormat();
		cellFormat.setBorder(Border.ALL, BorderLineStyle.THIN);
		cellFormat.setBackground(rowIndex % 2 == 0 ? Colour.WHITE : Colour.VERY_LIGHT_YELLOW);
		return cellFormat;
	}
}
