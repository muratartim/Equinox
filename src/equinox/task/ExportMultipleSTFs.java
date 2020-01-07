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
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.imageio.ImageIO;

import equinox.Equinox;
import equinox.data.fileType.STFFile;
import equinox.data.fileType.STFFileBucket;
import equinox.data.fileType.SpectrumItem;
import equinox.dataServer.remote.data.PilotPointImageType;
import equinox.plugin.FileType;
import equinox.process.SaveSTFFile;
import equinox.serverUtilities.Permission;
import equinox.task.InternalEquinoxTask.LongRunningTask;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
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
 * Class for export multiple STF files task.
 *
 * @author Murat Artim
 * @date Feb 8, 2016
 * @time 2:37:10 PM
 */
public class ExportMultipleSTFs extends InternalEquinoxTask<Void> implements LongRunningTask {

	/** Spectrum items to export. */
	private final SpectrumItem[] items_;

	/** Output directory. */
	private final Path outputDirectory_;

	/**
	 * Creates export multiple STF files task.
	 *
	 * @param items
	 *            Spectrum items to export.
	 * @param outputDirectory
	 *            Output directory.
	 */
	public ExportMultipleSTFs(ObservableList<TreeItem<String>> items, File outputDirectory) {
		items_ = new SpectrumItem[items.size()];
		for (int i = 0; i < items_.length; i++) {
			items_[i] = (SpectrumItem) items.get(i);
		}
		outputDirectory_ = outputDirectory.toPath();
	}

	@Override
	public boolean canBeCancelled() {
		return true;
	}

	@Override
	public String getTaskTitle() {
		return "Export STF files";
	}

	@Override
	protected Void call() throws Exception {

		// check permission
		checkPermission(Permission.EXPORT_PILOT_POINT);

		// update progress info
		updateTitle("Exporting STF files to '" + outputDirectory_.getFileName() + "'");

		// declare workbook
		WritableWorkbook workbook = null;

		try {

			// create workbook
			workbook = Workbook.createWorkbook(outputDirectory_.resolve("Pilot_Point_Info.xls").toFile());

			// create worksheet
			WritableSheet sheet = workbook.createSheet("Pilot Point Info", 0);

			// write headers
			updateMessage("Creating pilot point info Excel sheet...");
			writeHeaders(sheet);

			// task cancelled
			if (isCancelled())
				return null;

			// write data
			if (items_[0] instanceof STFFileBucket) {
				writeDataFromBucket(sheet, workbook);
			}
			else {
				writeData(sheet);
			}

			// task cancelled
			if (isCancelled())
				return null;

			// write data
			workbook.write();
		}

		// close workbook
		finally {
			if (workbook != null) {
				workbook.close();
			}
		}

		// return
		return null;
	}

	/**
	 * Writes pilot point data from STF file buckets.
	 *
	 * @param sheet
	 *            Pilot point info excel worksheet.
	 * @param workbook
	 *            Workbook.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeDataFromBucket(WritableSheet sheet, WritableWorkbook workbook) throws Exception {

		// get total number of STF files to be written
		int numSTFs = 0;
		for (SpectrumItem element : items_) {
			numSTFs += ((STFFileBucket) element).getNumberOfSTFs();
		}

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// prepare statement for getting STF file info
			String sql = "select * from stf_files where cdf_id = ? order by name";
			try (PreparedStatement getSTFs = connection.prepareStatement(sql)) {

				// prepare statement for getting pilot point images
				sql = "select image from pilot_point_image where id = ?";
				try (PreparedStatement getPPImage = connection.prepareStatement(sql)) {
					sql = "select image from pilot_point_da where id = ?";
					try (PreparedStatement getPPDA = connection.prepareStatement(sql)) {
						sql = "select image from pilot_point_dc where id = ?";
						try (PreparedStatement getPPDC = connection.prepareStatement(sql)) {
							sql = "select image from pilot_point_tf_dc where id = ?";
							try (PreparedStatement getPPTFDC = connection.prepareStatement(sql)) {
								sql = "select image from pilot_point_lc where id = ?";
								try (PreparedStatement getPPLC = connection.prepareStatement(sql)) {
									sql = "select image from pilot_point_mp where id = ?";
									try (PreparedStatement getPPMP = connection.prepareStatement(sql)) {
										sql = "select image from pilot_point_st_fo where id = ?";
										try (PreparedStatement getPPSTFO = connection.prepareStatement(sql)) {
											sql = "select image from pilot_point_st_nop where id = ?";
											try (PreparedStatement getPPSTNOP = connection.prepareStatement(sql)) {
												sql = "select image from pilot_point_st_rh where id = ?";
												try (PreparedStatement getPPSTRH = connection.prepareStatement(sql)) {
													sql = "select image from pilot_point_tf_ho where id = ?";
													try (PreparedStatement getPPTFHO = connection.prepareStatement(sql)) {
														sql = "select image from pilot_point_tf_hs where id = ?";
														try (PreparedStatement getPPTFHS = connection.prepareStatement(sql)) {
															sql = "select image from pilot_point_tf_l where id = ?";
															try (PreparedStatement getPPTFL = connection.prepareStatement(sql)) {

																// loop over buckets
																int rowIndex = 0, stfCount = 0, pageCount = 0;
																for (SpectrumItem element : items_) {

																	// get STF file
																	STFFileBucket bucket = (STFFileBucket) element;

																	// get STF files
																	getSTFs.setInt(1, bucket.getParentItem().getID());
																	try (ResultSet stfFiles = getSTFs.executeQuery()) {

																		// loop over STF files
																		while (stfFiles.next()) {

																			// task cancelled
																			if (isCancelled())
																				return;

																			// maximum row limit reached
																			if (rowIndex >= 50000) {

																				// increment page count
																				pageCount++;

																				// create worksheet
																				sheet = workbook.createSheet("Page " + (pageCount + 1), pageCount);

																				// write headers
																				writeHeaders(sheet);

																				// reset row index
																				rowIndex = 0;
																			}

																			// create pilot point directories
																			Path ppOutputDirectory = Files.createDirectory(outputDirectory_.resolve("PP_" + stfCount));

																			// get pilot point, spectrum names and fatigue mission
																			int stfID = stfFiles.getInt("file_id");
																			int stressTableID = stfFiles.getInt("stress_table_id");
																			boolean is2d = stfFiles.getBoolean("is_2d");
																			String ppName = FileType.getNameWithoutExtension(stfFiles.getString("name"));
																			String spectrumName = bucket.getParentItem().getName();
																			String mission = bucket.getParentItem().getMission();
																			String program = bucket.getParentItem().getProgram();
																			String section = bucket.getParentItem().getSection();

																			// write progress info
																			updateMessage("Writing data for pilot point '" + ppName + "'...");
																			updateProgress(stfCount, numSTFs);
																			stfCount++;

																			// write info
																			writeInfo(ppName, spectrumName, program, section, mission, stfFiles, sheet, rowIndex + 1, ppOutputDirectory);

																			// write STF file
																			new SaveSTFFile(this, stfID, stressTableID, is2d, ppOutputDirectory.resolve(ppName + FileType.STF.getExtension())).start(connection);

																			// write image files
																			writePNGFile(PilotPointImageType.IMAGE, stfID, getPPImage, ppOutputDirectory);
																			writePNGFile(PilotPointImageType.DAMAGE_ANGLE, stfID, getPPDA, ppOutputDirectory);
																			writePNGFile(PilotPointImageType.LOADCASE_DAMAGE_CONTRIBUTION, stfID, getPPDC, ppOutputDirectory);
																			writePNGFile(PilotPointImageType.FLIGHT_DAMAGE_CONTRIBUTION, stfID, getPPTFDC, ppOutputDirectory);
																			writePNGFile(PilotPointImageType.FLIGHT_OCCURRENCE, stfID, getPPSTFO, ppOutputDirectory);
																			writePNGFile(PilotPointImageType.FLIGHT_WITH_HIGHEST_OCCURRENCE, stfID, getPPTFHO, ppOutputDirectory);
																			writePNGFile(PilotPointImageType.FLIGHT_WITH_MAX_TOTAL_STRESS, stfID, getPPTFHS, ppOutputDirectory);
																			writePNGFile(PilotPointImageType.LEVEL_CROSSING, stfID, getPPLC, ppOutputDirectory);
																			writePNGFile(PilotPointImageType.LONGEST_FLIGHT, stfID, getPPTFL, ppOutputDirectory);
																			writePNGFile(PilotPointImageType.MISSION_PROFILE, stfID, getPPMP, ppOutputDirectory);
																			writePNGFile(PilotPointImageType.NUMBER_OF_PEAKS, stfID, getPPSTNOP, ppOutputDirectory);
																			writePNGFile(PilotPointImageType.RAINFLOW_HISTOGRAM, stfID, getPPSTRH, ppOutputDirectory);

																			// task cancelled
																			if (isCancelled())
																				return;

																			// increment row index
																			rowIndex++;
																		}
																	}
																}
															}
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Writes pilot point data.
	 *
	 * @param sheet
	 *            Pilot point info excel worksheet.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void writeData(WritableSheet sheet) throws Exception {

		// get connection to database
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {

			// prepare statement for getting STF file info
			String sql = "select * from stf_files where file_id = ?";
			try (PreparedStatement getInfo = connection.prepareStatement(sql)) {

				// prepare statement for getting pilot point images
				sql = "select image from pilot_point_image where id = ?";
				try (PreparedStatement getPPImage = connection.prepareStatement(sql)) {
					sql = "select image from pilot_point_da where id = ?";
					try (PreparedStatement getPPDA = connection.prepareStatement(sql)) {
						sql = "select image from pilot_point_dc where id = ?";
						try (PreparedStatement getPPDC = connection.prepareStatement(sql)) {
							sql = "select image from pilot_point_tf_dc where id = ?";
							try (PreparedStatement getPPTFDC = connection.prepareStatement(sql)) {
								sql = "select image from pilot_point_lc where id = ?";
								try (PreparedStatement getPPLC = connection.prepareStatement(sql)) {
									sql = "select image from pilot_point_mp where id = ?";
									try (PreparedStatement getPPMP = connection.prepareStatement(sql)) {
										sql = "select image from pilot_point_st_fo where id = ?";
										try (PreparedStatement getPPSTFO = connection.prepareStatement(sql)) {
											sql = "select image from pilot_point_st_nop where id = ?";
											try (PreparedStatement getPPSTNOP = connection.prepareStatement(sql)) {
												sql = "select image from pilot_point_st_rh where id = ?";
												try (PreparedStatement getPPSTRH = connection.prepareStatement(sql)) {
													sql = "select image from pilot_point_tf_ho where id = ?";
													try (PreparedStatement getPPTFHO = connection.prepareStatement(sql)) {
														sql = "select image from pilot_point_tf_hs where id = ?";
														try (PreparedStatement getPPTFHS = connection.prepareStatement(sql)) {
															sql = "select image from pilot_point_tf_l where id = ?";
															try (PreparedStatement getPPTFL = connection.prepareStatement(sql)) {

																// loop over STF files
																for (int i = 0; i < items_.length; i++) {

																	// task cancelled
																	if (isCancelled())
																		return;

																	// get STF file
																	STFFile stfFile = (STFFile) items_[i];

																	// create pilot point directories
																	Path ppOutputDirectory = Files.createDirectory(outputDirectory_.resolve("PP_" + i));

																	// get pilot point, spectrum names and fatigue mission
																	String ppName = FileType.getNameWithoutExtension(stfFile.getName());
																	String spectrumName = stfFile.getParentItem().getName();
																	String mission = stfFile.getMission();
																	String program = stfFile.getParentItem().getProgram();
																	String section = stfFile.getParentItem().getSection();

																	// update info
																	updateMessage("Writing data for pilot point '" + ppName + "'...");
																	updateProgress(i, items_.length);

																	// get info
																	getInfo.setInt(1, stfFile.getID());
																	try (ResultSet stfInfo = getInfo.executeQuery()) {
																		while (stfInfo.next()) {

																			// write info
																			writeInfo(ppName, spectrumName, program, section, mission, stfInfo, sheet, i + 1, ppOutputDirectory);

																			// task cancelled
																			if (isCancelled())
																				return;
																		}
																	}

																	// write STF file
																	new SaveSTFFile(this, stfFile, ppOutputDirectory.resolve(ppName + FileType.STF.getExtension())).start(connection);

																	// write image files
																	writePNGFile(PilotPointImageType.IMAGE, stfFile.getID(), getPPImage, ppOutputDirectory);
																	writePNGFile(PilotPointImageType.DAMAGE_ANGLE, stfFile.getID(), getPPDA, ppOutputDirectory);
																	writePNGFile(PilotPointImageType.LOADCASE_DAMAGE_CONTRIBUTION, stfFile.getID(), getPPDC, ppOutputDirectory);
																	writePNGFile(PilotPointImageType.FLIGHT_DAMAGE_CONTRIBUTION, stfFile.getID(), getPPTFDC, ppOutputDirectory);
																	writePNGFile(PilotPointImageType.FLIGHT_OCCURRENCE, stfFile.getID(), getPPSTFO, ppOutputDirectory);
																	writePNGFile(PilotPointImageType.FLIGHT_WITH_HIGHEST_OCCURRENCE, stfFile.getID(), getPPTFHO, ppOutputDirectory);
																	writePNGFile(PilotPointImageType.FLIGHT_WITH_MAX_TOTAL_STRESS, stfFile.getID(), getPPTFHS, ppOutputDirectory);
																	writePNGFile(PilotPointImageType.LEVEL_CROSSING, stfFile.getID(), getPPLC, ppOutputDirectory);
																	writePNGFile(PilotPointImageType.LONGEST_FLIGHT, stfFile.getID(), getPPTFL, ppOutputDirectory);
																	writePNGFile(PilotPointImageType.MISSION_PROFILE, stfFile.getID(), getPPMP, ppOutputDirectory);
																	writePNGFile(PilotPointImageType.NUMBER_OF_PEAKS, stfFile.getID(), getPPSTNOP, ppOutputDirectory);
																	writePNGFile(PilotPointImageType.RAINFLOW_HISTOGRAM, stfFile.getID(), getPPSTRH, ppOutputDirectory);

																	// task cancelled
																	if (isCancelled())
																		return;
																}
															}
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Writes out pilot point image file.
	 *
	 * @param imageType
	 *            Pilot point image type.
	 * @param stfID
	 *            STF file ID.
	 * @param statement
	 *            Database statement.
	 * @param ppOutputDirectory
	 *            Pilot point output directory.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static void writePNGFile(PilotPointImageType imageType, int stfID, PreparedStatement statement, Path ppOutputDirectory) throws Exception {

		// set file ID
		statement.setInt(1, stfID);

		// execute statement
		try (ResultSet resultSet = statement.executeQuery()) {
			while (resultSet.next()) {

				// set image
				Blob blob = resultSet.getBlob("image");
				if (blob != null) {
					byte[] imageBytes = blob.getBytes(1L, (int) blob.length());
					try (ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes)) {

						// create buffered image
						BufferedImage bufImg = ImageIO.read(ImageIO.createImageInputStream(inputStream));

						// write image to file
						ImageIO.write(bufImg, "png", ppOutputDirectory.resolve(imageType.getFileName()).toFile());
					}

					// free blob
					blob.free();
				}
			}
		}
	}

	/**
	 * Writes STF information in given worksheet.
	 *
	 * @param ppName
	 *            Pilot point name.
	 * @param spectrumName
	 *            Spectrum name.
	 * @param program
	 *            Aircraft program.
	 * @param section
	 *            Aircraft section.
	 * @param mission
	 *            Fatigue mission.
	 * @param resultSet
	 *            Database result set.
	 * @param sheet
	 *            Info worksheet.
	 * @param row
	 *            Info worksheet row index.
	 * @param ppOutputDirectory
	 *            Path to pilot point output directory.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static void writeInfo(String ppName, String spectrumName, String program, String section, String mission, ResultSet resultSet, WritableSheet sheet, int row, Path ppOutputDirectory) throws Exception {

		// get output directory name
		Path outputDirName = ppOutputDirectory.getFileName();
		if (outputDirName == null)
			throw new Exception("Cannot get output directory name.");

		// initialize column index
		int column = 0;

		// directory name
		sheet.addCell(new jxl.write.Label(column, row, outputDirName.toString(), getDataFormat(row, CellType.LABEL, false)));
		column++;

		// pilot point name
		sheet.addCell(new jxl.write.Label(column, row, ppName, getDataFormat(row, CellType.LABEL, false)));
		column++;

		// spectrum name
		sheet.addCell(new jxl.write.Label(column, row, spectrumName, getDataFormat(row, CellType.LABEL, false)));
		column++;

		// aircraft program
		sheet.addCell(new jxl.write.Label(column, row, program, getDataFormat(row, CellType.LABEL, false)));
		column++;

		// aircraft section
		sheet.addCell(new jxl.write.Label(column, row, section, getDataFormat(row, CellType.LABEL, false)));
		column++;

		// fatigue mission
		sheet.addCell(new jxl.write.Label(column, row, mission, getDataFormat(row, CellType.LABEL, false)));
		column++;

		// description
		sheet.addCell(new jxl.write.Label(column, row, resultSet.getString("description"), getDataFormat(row, CellType.LABEL, false)));
		column++;

		// data source
		sheet.addCell(new jxl.write.Label(column, row, resultSet.getString("data_source"), getDataFormat(row, CellType.LABEL, false)));
		column++;

		// generation source
		sheet.addCell(new jxl.write.Label(column, row, resultSet.getString("generation_source"), getDataFormat(row, CellType.LABEL, false)));
		column++;

		// delivery reference
		String deliveryRef = resultSet.getString("delivery_ref_num");
		sheet.addCell(new jxl.write.Label(column, row, deliveryRef == null ? "DRAFT" : deliveryRef, getDataFormat(row, CellType.LABEL, false)));
		column++;

		// pilot point issue
		sheet.addCell(new jxl.write.Label(column, row, resultSet.getString("issue"), getDataFormat(row, CellType.LABEL, false)));
		column++;

		// element ID
		sheet.addCell(new jxl.write.Label(column, row, resultSet.getString("eid"), getDataFormat(row, CellType.LABEL, false)));
		column++;

		// element type
		sheet.addCell(new jxl.write.Label(column, row, resultSet.getString("element_type"), getDataFormat(row, CellType.LABEL, false)));
		column++;

		// frame/rib position
		sheet.addCell(new jxl.write.Label(column, row, resultSet.getString("frame_rib_position"), getDataFormat(row, CellType.LABEL, false)));
		column++;

		// stringer position
		sheet.addCell(new jxl.write.Label(column, row, resultSet.getString("stringer_position"), getDataFormat(row, CellType.LABEL, false)));
		column++;

		// fatigue material
		sheet.addCell(new jxl.write.Label(column, row, resultSet.getString("fatigue_material"), getDataFormat(row, CellType.LABEL, false)));
		column++;

		// preffas material
		sheet.addCell(new jxl.write.Label(column, row, resultSet.getString("preffas_material"), getDataFormat(row, CellType.LABEL, false)));
		column++;

		// linear material
		sheet.addCell(new jxl.write.Label(column, row, resultSet.getString("linear_material"), getDataFormat(row, CellType.LABEL, false)));
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
