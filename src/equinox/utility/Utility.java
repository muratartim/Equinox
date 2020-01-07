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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.commons.lang3.RandomUtils;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfAction;
import com.itextpdf.text.pdf.PdfAnnotation;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfFileSpecification;
import com.itextpdf.text.pdf.PdfWriter;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.zaxxer.hikari.HikariConfig;

import equinox.Equinox;
import equinox.data.Settings;
import equinox.data.fileType.SpectrumItem;
import equinox.plugin.FileType;
import equinox.serverUtilities.FilerConnection;
import equinox.serverUtilities.ServerUtility;
import equinox.task.InternalEquinoxTask;
import equinox.task.TemporaryFileCreatingTask;
import javafx.concurrent.Task;
import javafx.scene.CacheHint;
import javafx.scene.ImageCursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.TreeItem;

/**
 * Utility class.
 *
 * @author Murat Artim
 * @date Dec 6, 2013
 * @time 10:30:27 AM
 */
public class Utility {

	/** Buffer size for extracting zipped files. */
	private static final int BUFSIZE = 2048;

	/**
	 * Loads and returns image resource.
	 *
	 * @param fileName
	 *            Name of image file (including file extension).
	 * @return Image resource.
	 */
	public static javafx.scene.image.Image getImage(String fileName) {
		return new javafx.scene.image.Image(Equinox.class.getResource("image/" + fileName).toExternalForm());
	}

	/**
	 * Sets up database connection pool for connecting to local database.
	 *
	 * @param dbPath
	 *            Path to local database.
	 *
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	public static void setupLocalDBPool(Path dbPath) throws Exception {

		// set properties to configuration
		HikariConfig config = new HikariConfig();
		config.setPoolName("Local DCP");
		config.setDataSourceClassName("org.apache.derby.jdbc.EmbeddedDataSource");
		config.setMaximumPoolSize(10);
		config.setMaxLifetime(60000);
		config.setIdleTimeout(30000);
		config.setJdbcUrl("jdbc:derby:" + dbPath.toString());
		config.setUsername("aurora");
		config.setPassword("17891917");
		config.addDataSourceProperty("databaseName", dbPath.toString());

		// create pool
		Equinox.DBC_POOL = new HikariEngine(config);

		// check connection
		try (Connection connection = Equinox.DBC_POOL.getConnection()) {
			try (Statement statement = connection.createStatement()) {
				try (ResultSet resultSet = statement.executeQuery("select * from aurora.db_version")) {
					Equinox.LOGGER.log(Level.INFO, "Successfully connected to local database '" + dbPath.getFileName().toString() + "'");
				}
			}
		}
	}

	/**
	 * Builds and returns connection to filer SFTP server. Note that, the supplied session, channel and sftpChannel objects must be disconnected after usage.
	 *
	 * @param settings
	 *            Application settings.
	 * @return Filer connection. parameters.
	 * @throws JSchException
	 *             If filer connection cannot be established.
	 */
	public static FilerConnection createFilerConnection(Settings settings) throws JSchException {

		// set connection properties
		String username = (String) settings.getValue(Settings.FILER_USERNAME);
		String hostname = (String) settings.getValue(Settings.FILER_HOSTNAME);
		int port = Integer.parseInt((String) settings.getValue(Settings.FILER_PORT));
		String password = (String) settings.getValue(Settings.FILER_PASSWORD);
		String filerRoot = (String) settings.getValue(Settings.FILER_ROOT_PATH);

		// create session
		JSch jsch = new JSch();
		Session session = jsch.getSession(username, hostname, port);
		session.setConfig("StrictHostKeyChecking", "no");
		session.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password");
		session.setPassword(password);
		session.connect();

		// open channel and connect
		Channel channel = session.openChannel("sftp");
		channel.connect();
		ChannelSftp sftpChannel = (ChannelSftp) channel;

		// create and return connection object
		return new FilerConnection(session, channel, sftpChannel, Equinox.LOGGER, filerRoot);
	}

	/**
	 * Generates and returns container package file name, including file extension.
	 *
	 * @param osType
	 *            Operating system type.
	 * @param osArch
	 *            Operating system architecture.
	 * @return Container package file name including file extension.
	 */
	public static String getContainerFileName(String osType, String osArch) {
		String name = "dataAnalyst";
		name += "_" + osType + "_" + osArch;
		name += osType.equals(ServerUtility.LINUX) ? ".tar.gz" : ".zip";
		return name;
	}

	/**
	 * Returns the container version description file name.
	 *
	 * @return The container version description file name.
	 */
	public static String getContainerVersionDescriptionFileName() {
		return "dataAnalyst_versionDesc.html";
	}

	/**
	 * Wrapper method to throw exceptions from lambda expressions. Use this wrapper if a lambda expression has to throw exception.
	 *
	 * @param throwingConsumer
	 *            Throwing consumer.
	 * @return Consumer.
	 */
	public static <T> Consumer<T> exceptionThrowingLambda(ThrowingConsumer<T, Exception> throwingConsumer) {
		return i -> {
			try {
				throwingConsumer.accept(i);
			}
			catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		};
	}

	/**
	 * Returns human readable file size.
	 *
	 * @param size
	 *            Size in bytes.
	 * @return Human readable file size.
	 */
	public static String readableFileSize(long size) {
		if (size <= 0)
			return "0";
		final String[] units = new String[] { "bytes", "KB", "MB", "GB", "TB" };
		int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
		return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
	}

	/**
	 * Sets up PDF report document.
	 *
	 * @param task
	 *            The calling task.
	 * @param document
	 *            PDF document.
	 * @param outputStream
	 *            Output stream.
	 * @param title
	 *            Report title.
	 * @return PDF writer.
	 * @throws DocumentException
	 *             If exception occurs during process.
	 */
	public static PdfWriter setupPDFReportDocument(InternalEquinoxTask<?> task, Document document, OutputStream outputStream, String title) throws DocumentException {

		// progress info
		task.updateMessage("Setting up PDF report document...");

		// create PDF writer
		PdfWriter writer = PdfWriter.getInstance(document, outputStream);

		// set document properties
		document.addTitle(title);
		document.addAuthor("Equinox version " + Equinox.VERSION.toString());
		document.addSubject(title);
		document.addCreationDate();
		document.addLanguage("English");

		// set margins
		document.setMargins(36, 36, 46, 56);

		// add footers
		writer.setPageEvent(new ReportFooter(title));

		// return PDF writer
		return writer;
	}

	/**
	 * Creates PDF report end page.
	 *
	 * @param task
	 *            Calling task.
	 * @param document
	 *            PDF document.
	 * @param writer
	 *            PDF writer.
	 * @param title
	 *            Report title.
	 * @param hostname
	 *            Web server host name.
	 * @param port
	 *            Web server port number.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	public static void createPDFReportEndPage(InternalEquinoxTask<?> task, Document document, PdfWriter writer, String title, String hostname, String port) throws Exception {

		// progress info
		task.updateMessage("Creating end page...");

		// rotate page to portrait
		document.setPageSize(PageSize.A4);
		document.newPage();

		// get canvas
		PdfContentByte canvas = writer.getDirectContent();

		// create top rectangle
		float ystart = (document.top() - document.bottom()) / 2 + 250;
		float yend = ystart - 110;
		Rectangle topRectangle = new Rectangle(0, ystart, document.right() + document.rightMargin(), yend);
		topRectangle.setBackgroundColor(new BaseColor(70, 130, 180));
		canvas.rectangle(topRectangle);

		// create Equinox image
		Image img = Image.getInstance(Equinox.class.getResource("image/EquinoxReportImage.png").toString());
		img.setAbsolutePosition(document.right() - document.rightMargin() - 50, ystart - 105);
		canvas.addImage(img);

		// create header 1
		Font font1 = FontFactory.getFont(FontFactory.HELVETICA, 26, Font.BOLDITALIC, BaseColor.WHITE);
		Phrase header1 = new Phrase(title, font1);
		ColumnText.showTextAligned(canvas, Element.ALIGN_RIGHT, header1, document.right() - 110, ystart - 50, 0);

		// create header 2
		Font font2 = FontFactory.getFont(FontFactory.HELVETICA, 14, Font.BOLDITALIC, BaseColor.WHITE);
		Phrase header2 = new Phrase("Generated by Equinox version " + Equinox.VERSION.toString(), font2);
		ColumnText.showTextAligned(canvas, Element.ALIGN_RIGHT, header2, document.right() - 110, ystart - 80, 0);

		// add installations header
		Font font3 = FontFactory.getFont(FontFactory.HELVETICA, 16, Font.BOLDITALIC, new BaseColor(112, 128, 144));
		Paragraph p1 = new Paragraph("\n\n\n\n\n\n\n\n\n\n\n\n\nDon't have Equinox yet?", font3);
		p1.setAlignment(Element.ALIGN_RIGHT);
		p1.setLeading(24);
		p1.setSpacingAfter(24);
		document.add(p1);

		// create windows download button
		Paragraph p2 = new Paragraph();
		p2.setAlignment(Element.ALIGN_RIGHT);
		p2.setLeading(24);
		p2.setSpacingAfter(16);
		Image windows = Image.getInstance(Equinox.class.getResource("image/downloadButtonForWindows.png").toString());
		String url = "http://" + hostname + ":" + port + "/2B03/EquinoxWeb/files/" + getContainerFileName(ServerUtility.WINDOWS, ServerUtility.X86);
		Chunk windowsButton = new Chunk(windows, 0, 0, true);
		windowsButton.setAction(new PdfAction(url));
		p2.add(windowsButton);
		document.add(p2);

		// create windows 64 bit download button
		Paragraph p3 = new Paragraph();
		p3.setAlignment(Element.ALIGN_RIGHT);
		p3.setLeading(24);
		p3.setSpacingAfter(16);
		Image windows64 = Image.getInstance(Equinox.class.getResource("image/downloadButtonForWindows64.png").toString());
		url = "http://" + hostname + ":" + port + "/2B03/EquinoxWeb/files/" + getContainerFileName(ServerUtility.WINDOWS, ServerUtility.X64);
		Chunk windows64Button = new Chunk(windows64, 0, 0, true);
		windows64Button.setAction(new PdfAction(url));
		p3.add(windows64Button);
		document.add(p3);

		// create linux download button
		Paragraph p4 = new Paragraph();
		p4.setAlignment(Element.ALIGN_RIGHT);
		p4.setLeading(24);
		p4.setSpacingAfter(16);
		Image linux = Image.getInstance(Equinox.class.getResource("image/downloadButtonForLinux.png").toString());
		url = "http://" + hostname + ":" + port + "/2B03/EquinoxWeb/files/" + getContainerFileName(ServerUtility.LINUX, ServerUtility.X86);
		Chunk linuxButton = new Chunk(linux, 0, 0, true);
		linuxButton.setAction(new PdfAction(url));
		p4.add(linuxButton);
		document.add(p4);

		// create mac download button
		Paragraph p5 = new Paragraph();
		p5.setAlignment(Element.ALIGN_RIGHT);
		p5.setLeading(24);
		p5.setSpacingAfter(4);
		Image mac = Image.getInstance(Equinox.class.getResource("image/downloadButtonForMac.png").toString());
		url = "http://" + hostname + ":" + port + "/2B03/EquinoxWeb/files/" + getContainerFileName(ServerUtility.MACOS, ServerUtility.X64);
		Chunk macButton = new Chunk(mac, 0, 0, true);
		macButton.setAction(new PdfAction(url));
		p5.add(macButton);
		document.add(p5);

		// create bottom rectangle
		ystart -= 175;
		yend = ystart - 375;
		Rectangle bottomRectangle = new Rectangle(340, ystart, document.right() + document.rightMargin(), yend);
		bottomRectangle.setBorder(Rectangle.BOTTOM + Rectangle.TOP + Rectangle.LEFT);
		bottomRectangle.setBorderColor(new BaseColor(112, 128, 144));
		bottomRectangle.setBorderWidth(2f);
		canvas.rectangle(bottomRectangle);
	}

	/**
	 * Creates PDF report cover page.
	 *
	 * @param task
	 *            Calling task.
	 * @param document
	 *            PDF document.
	 * @param writer
	 *            PDF writer.
	 * @param title
	 *            Report title.
	 * @param chapterNames
	 *            Array containing the chapter names.
	 * @param fileAttachments
	 *            File attachment mapping. The key is the name of attachment, the value is the path to attached file. Null should be given if there is no attachment.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	public static void createPDFReportCoverPage(InternalEquinoxTask<?> task, Document document, PdfWriter writer, String title, String[] chapterNames, HashMap<String, Path> fileAttachments) throws Exception {

		// progress info
		task.updateMessage("Creating cover page...");

		// get canvas
		PdfContentByte canvas = writer.getDirectContent();

		// create top rectangle
		float ystart = (document.top() - document.bottom()) / 2 + 250;
		float yend = ystart - 110;
		Rectangle topRectangle = new Rectangle(0, ystart, document.right() + document.rightMargin(), yend);
		topRectangle.setBackgroundColor(new BaseColor(70, 130, 180));
		canvas.rectangle(topRectangle);

		// create Equinox image
		Image img = Image.getInstance(Equinox.class.getResource("image/EquinoxReportImage.png").toString());
		img.setAbsolutePosition(document.right() - document.rightMargin() - 50, ystart - 105);
		canvas.addImage(img);

		// create header 1
		Font font1 = FontFactory.getFont(FontFactory.HELVETICA, 26, Font.BOLDITALIC, BaseColor.WHITE);
		Phrase header1 = new Phrase(title, font1);
		ColumnText.showTextAligned(canvas, Element.ALIGN_RIGHT, header1, document.right() - 110, ystart - 50, 0);

		// create header 2
		Font font2 = FontFactory.getFont(FontFactory.HELVETICA, 14, Font.BOLDITALIC, BaseColor.WHITE);
		Phrase header2 = new Phrase("Generated by Equinox version " + Equinox.VERSION.toString(), font2);
		ColumnText.showTextAligned(canvas, Element.ALIGN_RIGHT, header2, document.right() - 110, ystart - 80, 0);

		// add table of contents header
		Font font3 = FontFactory.getFont(FontFactory.HELVETICA, 16, Font.BOLDITALIC, new BaseColor(112, 128, 144));
		Paragraph p1 = new Paragraph("\n\n\n\n\n\n\n\n\n\n\n\n\n\nTable of Contents", font3);
		p1.setAlignment(Element.ALIGN_RIGHT);
		p1.setLeading(24);
		p1.setSpacingAfter(-14);
		document.add(p1);

		// write table of contents
		Font font4 = FontFactory.getFont(FontFactory.HELVETICA, 12, Font.NORMAL, new BaseColor(70, 130, 180));
		Paragraph p2 = new Paragraph();
		p2.setFont(font4);
		p2.setAlignment(Element.ALIGN_RIGHT);
		p2.setLeading(24);
		for (String chapterName : chapterNames) {
			Chunk chunk = new Chunk("\n" + chapterName, font4);
			PdfAction action = PdfAction.gotoLocalPage(chapterName, false);
			chunk.setAction(action);
			p2.add(chunk);
		}

		// add table of contents paragraph to document
		document.add(p2);

		// there are file attachments
		if (fileAttachments != null) {

			// add file attachments header
			Paragraph p3 = new Paragraph("\n\nFile Attachments", font3);
			p3.setAlignment(Element.ALIGN_RIGHT);
			p3.setLeading(24);
			p3.setSpacingAfter(-14);
			document.add(p3);

			// write file attachments
			Paragraph p4 = new Paragraph();
			p4.setFont(font4);
			p4.setAlignment(Element.ALIGN_RIGHT);
			p4.setLeading(24);
			Iterator<Entry<String, Path>> attachments = fileAttachments.entrySet().iterator();
			while (attachments.hasNext()) {

				// get attachment
				Entry<String, Path> attachment = attachments.next();
				String name = attachment.getKey();
				Path file = attachment.getValue();

				// get file name
				Path fileNamePath = file.getFileName();
				if (fileNamePath == null)
					throw new Exception("Cannot get file name.");
				String fileName = fileNamePath.toString();

				// add attachment to document
				Phrase phrase = new Phrase("\n" + name + "  ", font4);
				Chunk chunk = new Chunk("\u00a0\u00a0", font4);
				PdfFileSpecification fs = PdfFileSpecification.fileEmbedded(writer, file.toString(), fileName, null);
				PdfAnnotation annotation = PdfAnnotation.createFileAttachment(writer, null, "Double click to open '" + fileName + "'", fs);
				chunk.setAnnotation(annotation);
				phrase.add(chunk);
				p4.add(phrase);
			}

			// add file attachments paragraph to document
			document.add(p4);
		}

		// new page
		document.newPage();
	}

	/**
	 * Creates and returns a new logger.
	 *
	 * @param level
	 *            Log level.
	 * @return The newly created logger.
	 */
	public static Logger createLogger(Level level) {

		try {

			// create logger
			Logger logger = Logger.getLogger(Equinox.class.getName());

			// create file handler
			FileHandler fileHandler = new FileHandler(Equinox.LOG_FILE.toString());

			// set simple formatter to file handler
			fileHandler.setFormatter(new SimpleFormatter());

			// add handler to logger
			logger.addHandler(fileHandler);

			// set log level (info, warning and severe are logged)
			logger.setLevel(level);

			// return logger
			return logger;
		}

		// exception occurred during creating logger
		catch (SecurityException | IOException e) {
			e.printStackTrace();
			System.exit(1);
			return null;
		}
	}

	/**
	 * Searches file tree recursively for given criteria.
	 *
	 * @param root
	 *            File tree root.
	 * @param searchID
	 *            ID of searched item.
	 * @param searchClass
	 *            Class of searched item.
	 * @return Searched item or null if item could not be found.
	 */
	public static TreeItem<String> searchFileTree(TreeItem<String> root, int searchID, Class<?> searchClass) {

		// class match
		if (root.getClass().equals(searchClass)) {

			// cast to spectrum item
			SpectrumItem spectrumItem = (SpectrumItem) root;

			// id match
			if (spectrumItem.getID() == searchID)
				return root;
		}

		// has children
		if (!root.isLeaf()) {

			// initialize search item
			TreeItem<String> searchItem = null;

			// loop over children
			for (TreeItem<String> child : root.getChildren()) {

				// search child
				searchItem = searchFileTree(child, searchID, searchClass);

				// item found
				if (searchItem != null)
					return searchItem;
			}
		}

		// item not found
		return null;
	}

	/**
	 * Shuts down the current workspace.
	 */
	public static void shutdownWorkspace() {

		// no valid current path
		if (!Equinox.WORKSPACE_PATHS.hasValidCurrentPath())
			return;

		// set database properties
		String name = "jdbc:derby:" + Equinox.WORKSPACE_PATHS.getCurrentPath().toString();
		String username = "aurora";
		String password = "17891917";
		String command = name + ";user=" + username + ";password=" + password + ";shutdown=true";

		// shutdown the workspace
		try {
			DriverManager.getConnection(command);
		}

		// shutdown successful
		catch (SQLException e) {
			Equinox.LOGGER.log(Level.INFO, "Current workspace successfully shutdown with the following exception:", e);
		}
	}

	/**
	 * Shuts down the database.
	 *
	 * @param dbPath
	 *            Path to database to shutdown.
	 */
	public static void shutdownDatabase(Path dbPath) {

		// set database properties
		String name = "jdbc:derby:" + dbPath.toString();
		String username = "aurora";
		String password = "17891917";
		String command = name + ";user=" + username + ";password=" + password + ";shutdown=true";

		// shutdown the database
		try {
			DriverManager.getConnection(command);
		}

		// shutdown successful
		catch (SQLException e) {
			Equinox.LOGGER.log(Level.INFO, "Database '" + dbPath.toString() + "' successfully shutdown with the following exception:", e);
		}
	}

	/**
	 * Shuts down the given thread executor in two phases, first by calling shutdown to reject incoming tasks, and then calling shutdownNow, if necessary, to cancel any lingering tasks.
	 *
	 * @param executor
	 *            Thread executor to shutdown.
	 */
	public static void shutdownThreadExecutor(ExecutorService executor) {

		// disable new tasks from being submitted
		executor.shutdown();

		try {

			// wait a while for existing tasks to terminate
			if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {

				// cancel currently executing tasks
				executor.shutdownNow();

				// wait a while for tasks to respond to being canceled
				if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
					Equinox.LOGGER.warning("Thread pool " + executor.toString() + " did not terminate.");
				}
			}
		}

		// exception occurred during shutting down the thread pool
		catch (InterruptedException ie) {

			// cancel if current thread also interrupted
			executor.shutdownNow();

			// preserve interrupt status
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Sorts given map by values in selected order.
	 *
	 * @param map
	 *            Map to be sorted.
	 * @param descending
	 *            True if the values should be sorted in descending order.
	 * @return The sorted map.
	 */
	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map, boolean descending) {
		if (descending)
			return map.entrySet().stream().sorted(Map.Entry.comparingByValue(Collections.reverseOrder())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
		return map.entrySet().stream().sorted(Map.Entry.comparingByValue(/* Collections.reverseOrder() */)).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
	}

	/**
	 * Utility method to get number of lines in the given text file.
	 *
	 * @param file
	 *            Text file.
	 * @param task
	 *            Task which called this method.
	 * @return The number of lines.
	 * @throws IOException
	 *             If exception occurs during reading the file.
	 */
	public static int countLines(Path file, Task<?> task) throws IOException {

		// initialize line count
		int count = 0;

		// create reader
		try (BufferedReader reader = Files.newBufferedReader(file, Charset.defaultCharset())) {

			// read file till the end
			while (reader.readLine() != null) {

				// task cancelled
				if (task.isCancelled()) {
					break;
				}

				// increment count
				count++;
			}
		}

		// return count
		return count;
	}

	/**
	 * Returns file count.
	 *
	 * @param directory
	 *            Path to directory where the files will be counted.
	 * @param fileExtension
	 *            File extension. Can be null if all files and sub-directories to be counted.
	 * @return File count.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	public static long countFiles(Path directory, String fileExtension) throws Exception {

		// all files and sub-directories
		if (fileExtension == null)
			return Files.list(directory).count();

		// initialize file count
		long fileCount = 0L;

		// create directory stream
		try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(directory, getFileFilter(fileExtension))) {

			// get iterator
			Iterator<Path> iterator = dirStream.iterator();

			// loop over files
			while (iterator.hasNext()) {
				iterator.next();
				fileCount++;
			}
		}

		// return file count
		return fileCount;
	}

	/**
	 * Sets up speed caching to given node and all its children. Speed caching enables fast and smooth animations.
	 *
	 * @param node
	 *            Node to set up.
	 * @param noSpeedID
	 *            ID of a node for NOT applying speed caching. This can be null.
	 */
	public static void setupSpeedCaching(Node node, String noSpeedID) {

		// no speed caching
		if (noSpeedID != null && node.getId() != null && noSpeedID.equals(node.getId())) {

			// setup children
			if (node instanceof Parent) {
				Parent parent = (Parent) node;
				for (Node child : parent.getChildrenUnmodifiable()) {
					setupSpeedCaching(child, noSpeedID);
				}
			}
		}

		// speed caching
		else {

			// setup node
			node.cacheProperty().set(true);
			node.cacheHintProperty().set(CacheHint.SPEED);

			// setup children
			if (node instanceof Parent) {
				Parent parent = (Parent) node;
				for (Node child : parent.getChildrenUnmodifiable()) {
					setupSpeedCaching(child, noSpeedID);
				}
			}
		}
	}

	/**
	 * Sets hand cursor to given nodes.
	 *
	 * @param nodes
	 *            Nodes to set cursor.
	 */
	public static void setHandCursor(Node... nodes) {
		for (Node node : nodes) {
			node.setCursor(ImageCursor.HAND);
		}
	}

	/**
	 * Creates and returns a working directory.
	 *
	 * @param namePrefix
	 *            Prefix for the directory name.
	 * @return Path to newly created working directory.
	 * @throws IOException
	 *             If exception occurs during process.
	 */
	public static Path createWorkingDirectory(String namePrefix) throws IOException {
		Path workingDirectory = Equinox.TEMP_DIR.resolve(namePrefix + "_" + RandomUtils.nextInt(0, 10000));
		while (Files.exists(workingDirectory)) {
			workingDirectory = Equinox.TEMP_DIR.resolve(namePrefix + "_" + RandomUtils.nextInt(0, 10000));
		}
		return Files.createDirectory(workingDirectory);
	}

	/**
	 * Creates and returns a new file filter.
	 *
	 * @param extension
	 *            File extension.
	 * @return Newly created file filter.
	 */
	public static DirectoryStream.Filter<Path> getFileFilter(String extension) {

		// create and return new file filter
		return entry -> {
			if (Files.isDirectory(entry))
				return false;
			Path fileName = entry.getFileName();
			if (fileName == null)
				return false;
			return fileName.toString().toUpperCase().endsWith(extension.toUpperCase());
		};
	}

	/**
	 * Extracts segment name from event name. <u>Note that, this code is NOT 100% reliable.</u>
	 *
	 * @param event
	 *            Event name.
	 * @return Segment name.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	public static String extractSegmentName(String event) throws Exception {

		// initialize segment name
		String segment = "";

		// segment name cannot be extracted
		if (!event.contains("_"))
			return segment;

		// split from underscore
		String[] split = event.trim().split("_");

		// 5 parts and last part is "L" (LAF segment)
		if (split.length == 5 && split[split.length - 1].equals("L")) {
			segment = split[split.length - 3];
		}
		else {
			segment = split[split.length - 2];
		}

		// remove initial 0 (if any)
		if (segment.startsWith("0")) {
			segment = segment.substring(1);
		}

		// return segment name
		return segment;
	}

	/**
	 * Extracts a file from GZIP package.
	 *
	 * @param gzipFile
	 *            Input GZIP file.
	 * @param outputFile
	 *            Extracted output file.
	 * @throws IOException
	 *             If exception occurs during process.
	 */
	public static void extractFileFromGZIP(Path gzipFile, Path outputFile) throws IOException {

		// create new buffer
		byte[] buffer = new byte[BUFSIZE];

		// create input stream
		try (GZIPInputStream gzis = new GZIPInputStream(new FileInputStream(gzipFile.toFile()))) {

			// create output stream
			try (FileOutputStream out = new FileOutputStream(outputFile.toFile())) {

				// read/write to buffer
				int len;
				while ((len = gzis.read(buffer)) > 0) {
					out.write(buffer, 0, len);
				}
			}
		}
	}

	/**
	 * Extracts and returns a file with the demanded file type from the given ZIP file. Note that, the first file with the given file type will be extracted and returned.
	 *
	 * @param zipFile
	 *            Path to ZIP file.
	 * @param task
	 *            Task which calls this method.
	 * @param type
	 *            The type of file to extract.
	 * @param outputDir
	 *            Output directory. If null is given, the working directory of the task will be used (if the task is not temporary file creating task, then a new working directory will be created).
	 * @return The extracted temporary file or null if no file with given file type could be found within the given ZIP file.
	 * @throws IOException
	 *             If exception occurs during process.
	 */
	public static Path extractFileFromZIP(Path zipFile, InternalEquinoxTask<?> task, FileType type, Path outputDir) throws IOException {

		// initialize output file
		Path output = null;

		// get working directory
		if (outputDir == null) {
			if (task instanceof TemporaryFileCreatingTask<?>) {
				TemporaryFileCreatingTask<?> task1 = (TemporaryFileCreatingTask<?>) task;
				outputDir = task1.getWorkingDirectory();
			}
			else {
				outputDir = createWorkingDirectory("extractFileFromZIP");
			}
		}

		// create zip input stream
		try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile.toString())), Charset.defaultCharset())) {

			// loop over zip entries
			ZipEntry ze;
			while ((ze = zis.getNextEntry()) != null) {

				// task cancelled
				if (task.isCancelled()) {
					break;
				}

				// not directory
				if (!ze.isDirectory())
					// required file type
					if (ze.getName().toUpperCase().endsWith(type.getExtension().toUpperCase())) {

						// create temporary output file
						output = outputDir.resolve(ze.getName());

						// create all necessary directories
						Path outputParentDir = output.getParent();
						if (outputParentDir != null) {
							Files.createDirectories(outputParentDir);
						}

						// create output stream
						try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(output.toString()))) {

							// create new buffer
							byte[] buffer = new byte[BUFSIZE];

							// write to output stream
							int len;
							while ((len = zis.read(buffer, 0, BUFSIZE)) != -1) {
								bos.write(buffer, 0, len);
							}
						}

						// close entry
						zis.closeEntry();
						break;
					}

				// close entry
				zis.closeEntry();
			}
		}

		// return output file
		return output;
	}

	/**
	 * Extracts and returns temporary files with the demanded file type from the given ZIP file. Note that, the first file with the given file type will be extracted and returned.
	 *
	 * @param zipPath
	 *            Path to ZIP file.
	 * @param task
	 *            Task which calls this method.
	 * @param type
	 *            The type of file to extract.
	 * @return The extracted temporary files or null if no file with given file type could be found within the given ZIP file.
	 * @throws IOException
	 *             If exception occurs during process.
	 */
	public static ArrayList<Path> extractFilesFromZIP(Path zipPath, TemporaryFileCreatingTask<?> task, FileType type) throws IOException {

		// update message
		task.updateMessage("Extracting all *" + type.getExtension() + " files from '" + zipPath.getFileName().toString() + "'...");

		// initialize output file
		ArrayList<Path> output = null;

		// open zip file
		try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {

			// get number of entries
			int numEntries = zipFile.size();

			// get entries
			Enumeration<? extends ZipEntry> entries = zipFile.entries();

			// loop over zip entries
			int entryCount = 0;
			while (entries.hasMoreElements()) {

				// get entry
				ZipEntry ze = entries.nextElement();

				// progress info
				task.updateProgress(entryCount, numEntries);
				entryCount++;

				// task cancelled
				if (task.isCancelled()) {
					break;
				}

				// not directory
				if (!ze.isDirectory()) {

					// required file type
					if (ze.getName().toUpperCase().endsWith(type.getExtension().toUpperCase())) {

						// create temporary output file
						Path file = task.getWorkingDirectory().resolve(ze.getName());

						// create all necessary directories
						Path fileParentDir = file.getParent();
						if (fileParentDir != null) {
							Files.createDirectories(fileParentDir);
						}

						// create output stream
						try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file.toString()))) {

							// create new buffer
							byte[] buffer = new byte[BUFSIZE];

							// open zip input stream
							try (InputStream zis = zipFile.getInputStream(ze)) {

								// write to output stream
								int len;
								while ((len = zis.read(buffer, 0, BUFSIZE)) != -1) {
									bos.write(buffer, 0, len);
								}
							}
						}

						// file is directory, doesn't exist or hidden
						if (!Files.exists(file) || Files.isDirectory(file) || Files.isHidden(file) || !Files.isRegularFile(file)) {
							continue;
						}

						// add file to output
						if (output == null) {
							output = new ArrayList<>();
						}
						output.add(file);
					}
				}
			}
		}

		// return output file
		return output;
	}

	/**
	 * Extracts and returns all files from the given ZIP file.
	 *
	 * @param zipPath
	 *            Path to ZIP file.
	 * @param task
	 *            Task which calls this method.
	 * @param outputDir
	 *            Output directory.
	 * @return The extracted temporary files or null if no file could be found within the given ZIP file.
	 * @throws IOException
	 *             If exception occurs during process.
	 */
	public static ArrayList<Path> extractAllFilesFromZIP(Path zipPath, InternalEquinoxTask<?> task, Path outputDir) throws IOException {

		// update message
		task.updateMessage("Extracting all files from '" + zipPath.getFileName().toString() + "'...");

		// initialize output file
		ArrayList<Path> output = null;

		// open zip file
		try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {

			// get number of entries
			int numEntries = zipFile.size();

			// get entries
			Enumeration<? extends ZipEntry> entries = zipFile.entries();

			// loop over zip entries
			int entryCount = 0;
			while (entries.hasMoreElements()) {

				// get entry
				ZipEntry ze = entries.nextElement();

				// progress info
				task.updateProgress(entryCount, numEntries);
				entryCount++;

				// task cancelled
				if (task.isCancelled()) {
					break;
				}

				// not directory
				if (!ze.isDirectory()) {

					// create temporary output file
					Path file = outputDir.resolve(ze.getName());

					// create all necessary directories
					Path fileParentDir = file.getParent();
					if (fileParentDir != null) {
						Files.createDirectories(fileParentDir);
					}

					// create output stream
					try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file.toString()))) {

						// create new buffer
						byte[] buffer = new byte[BUFSIZE];

						// open zip input stream
						try (InputStream zis = zipFile.getInputStream(ze)) {

							// write to output stream
							int len;
							while ((len = zis.read(buffer, 0, BUFSIZE)) != -1) {
								bos.write(buffer, 0, len);
							}
						}
					}

					// file is directory, doesn't exist or hidden
					if (!Files.exists(file) || Files.isDirectory(file) || Files.isHidden(file) || !Files.isRegularFile(file)) {
						continue;
					}

					// add file to output
					if (output == null) {
						output = new ArrayList<>();
					}
					output.add(file);
				}
			}
		}

		// return output file
		return output;
	}

	/**
	 * Deletes given file recursively.
	 *
	 * @param path
	 *            Path to directory where temporary files are kept.
	 * @param keep
	 *            Files to keep.
	 * @throws IOException
	 *             If exception occurs during process.
	 */
	public static void deleteTemporaryFiles(Path path, Path... keep) throws IOException {

		// null path
		if (path == null)
			return;

		// directory
		if (Files.isDirectory(path)) {

			// create directory stream
			try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(path)) {

				// get iterator
				Iterator<Path> iterator = dirStream.iterator();

				// loop over files
				while (iterator.hasNext()) {
					deleteTemporaryFiles(iterator.next(), keep);
				}
			}

			// delete directory (if not to be kept)
			try {
				if (!containsFile(path, keep)) {
					Files.delete(path);
				}
			}

			// directory not empty
			catch (DirectoryNotEmptyException e) {
				// ignore
			}
		}
		else if (!containsFile(path, keep)) {
			Files.delete(path);
		}
	}

	/**
	 * Returns true if given target file is contained within the given files array.
	 *
	 * @param target
	 *            Target file to search for.
	 * @param files
	 *            Array of files to search the target file.
	 * @return True if given target file is contained within the given files array.
	 */
	public static boolean containsFile(Path target, Path... files) {
		for (Path file : files)
			if (file.equals(target))
				return true;
		return false;
	}

	/**
	 * Checks and corrects (if necessary) the given file name to be valid for Windows OS.
	 *
	 * @param fileName
	 *            File name to be checked.
	 * @return Modified file name.
	 */
	public static String correctFileName(String fileName) {
		if (fileName.contains("\\")) {
			fileName = fileName.replaceAll(Matcher.quoteReplacement("\\"), "_");
		}
		if (fileName.contains("$")) {
			fileName = fileName.replaceAll(Matcher.quoteReplacement("$"), "_");
		}
		if (fileName.contains("/")) {
			fileName = fileName.replaceAll("/", "_");
		}
		if (fileName.contains(":")) {
			fileName = fileName.replaceAll(":", "_");
		}
		if (fileName.contains("*")) {
			fileName = fileName.replaceAll("\\*", "_");
		}
		if (fileName.contains("?")) {
			fileName = fileName.replaceAll("\\?", "_");
		}
		if (fileName.contains("\"")) {
			fileName = fileName.replaceAll("\"", "_");
		}
		if (fileName.contains("<")) {
			fileName = fileName.replaceAll("<", "_");
		}
		if (fileName.contains(">")) {
			fileName = fileName.replaceAll(">", "_");
		}
		if (fileName.contains("|")) {
			fileName = fileName.replaceAll("\\|", "_");
		}
		return fileName;
	}

	/**
	 * Checks the given file name if it is valid for Windows OS.
	 *
	 * @param fileName
	 *            File name to be checked.
	 * @return Warning message text if the file name is invalid, otherwise null.
	 */
	public static String isValidFileName(String fileName) {
		if (fileName.contains("\\") || fileName.contains("$") || fileName.contains("/") || fileName.contains(":") || fileName.contains("*") || fileName.contains("?") || fileName.contains("\"") || fileName.contains("<") || fileName.contains(">") || fileName.contains("|"))
			return "A file name can't contain any of the following characters: \\ $ / : * ? \" < > |";
		return null;
	}

	/**
	 * Validate the form of an email address.
	 *
	 * <P>
	 * Return <tt>true</tt> only if
	 * <ul>
	 * <li><tt>aEmailAddress</tt> can successfully construct an {@link javax.mail.internet.InternetAddress}
	 * <li>when parsed with "@" as delimiter, <tt>aEmailAddress</tt> contains two tokens which satisfy {@link equinox.utility.Utility#textHasContent}.
	 * </ul>
	 *
	 * <P>
	 * The second condition arises since local email addresses, simply of the form "<tt>albert</tt>", for example, are valid for {@link javax.mail.internet.InternetAddress}, but almost always undesired.
	 *
	 * @param email
	 *            Email address string to validate.
	 * @return True if given email address string is a valid email address.
	 */
	public static boolean isValidEmailAddress(String email) {

		// null email address
		if (email == null || email.trim().isEmpty())
			return false;

		// initialize result
		boolean result = true;

		try {

			// create internet address
			new InternetAddress(email);

			// doesn't have domain
			if (!hasNameAndDomain(email)) {
				result = false;
			}
		}

		// exception occurred during check
		catch (AddressException ex) {
			result = false;
		}

		// return result
		return result;
	}

	/**
	 * Saves the CLOB data as GZIP file.
	 *
	 * @param clob
	 *            CLOB data.
	 * @param output
	 *            Path to output GZIP file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	public static void gzipClobData(Clob clob, File output) throws Exception {

		// create buffer to store to be written bytes
		byte[] buffer = new byte[1024];

		// create output stream
		try (GZIPOutputStream gzos = new GZIPOutputStream(new FileOutputStream(output))) {

			// create stream to read file
			try (InputStream fis = clob.getAsciiStream()) {

				// read/write data
				int len;
				while ((len = fis.read(buffer)) > 0) {
					gzos.write(buffer, 0, len);
				}

				// finish
				gzos.finish();
			}
		}
	}

	/**
	 * Saves the CLOB data as ZIP file.
	 *
	 * @param clob
	 *            CLOB data.
	 * @param output
	 *            Path to output ZIP file.
	 * @param inputFileName
	 *            Input file name.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	public static void zipClobData(Clob clob, File output, String inputFileName) throws Exception {

		// create zip output stream
		try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(output)))) {

			// create buffer to store to be written bytes
			byte[] buf = new byte[1024];

			// create new zip entry
			zos.putNextEntry(new ZipEntry(inputFileName));

			// create stream to read file
			try (InputStream fis = clob.getAsciiStream()) {

				// read till the end of file
				int len;
				while ((len = fis.read(buf)) > 0) {
					zos.write(buf, 0, len);
				}
			}

			// close zip entry
			zos.closeEntry();
		}
	}

	/**
	 * Saves the BLOB data as GZIP file.
	 *
	 * @param blob
	 *            BLOB data.
	 * @param output
	 *            Path to output GZIP file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	public static void gzipBlobData(Blob blob, File output) throws Exception {

		// create buffer to store to be written bytes
		byte[] buffer = new byte[1024];

		// create output stream
		try (GZIPOutputStream gzos = new GZIPOutputStream(new FileOutputStream(output))) {

			// create stream to read file
			try (InputStream fis = blob.getBinaryStream()) {

				// read/write data
				int len;
				while ((len = fis.read(buffer)) > 0) {
					gzos.write(buffer, 0, len);
				}

				// finish
				gzos.finish();
			}
		}
	}

	/**
	 * Saves the BLOB data as ZIP file.
	 *
	 * @param blob
	 *            BLOB data.
	 * @param output
	 *            Path to output ZIP file.
	 * @param inputFileName
	 *            Input file name.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	public static void zipBlobData(Blob blob, File output, String inputFileName) throws Exception {

		// create zip output stream
		try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(output)))) {

			// create buffer to store to be written bytes
			byte[] buf = new byte[1024];

			// create new zip entry
			zos.putNextEntry(new ZipEntry(inputFileName));

			// create stream to read file
			try (InputStream fis = blob.getBinaryStream()) {

				// read till the end of file
				int len;
				while ((len = fis.read(buf)) > 0) {
					zos.write(buf, 0, len);
				}
			}

			// close zip entry
			zos.closeEntry();
		}
	}

	/**
	 * G-Zips given file to given output file.
	 *
	 * @param input
	 *            Input file to zip.
	 * @param output
	 *            Output GZIP file.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	public static void gzipFile(File input, File output) throws Exception {

		// create buffer to store to be written bytes
		byte[] buffer = new byte[1024];

		// create output stream
		try (GZIPOutputStream gzos = new GZIPOutputStream(new FileOutputStream(output))) {

			// create stream to read file
			try (FileInputStream in = new FileInputStream(input)) {

				// read/write data
				int len;
				while ((len = in.read(buffer)) > 0) {
					gzos.write(buffer, 0, len);
				}

				// finish
				gzos.finish();
			}
		}
	}

	/**
	 * Zips given file to given output file.
	 *
	 * @param file
	 *            File to zip.
	 * @param output
	 *            Output file path.
	 * @param task
	 *            The task calling this method.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	public static void zipFile(Path file, File output, InternalEquinoxTask<?> task) throws Exception {

		// update message
		task.updateMessage("Zipping file to '" + output.getName() + "'...");

		// create zip output stream
		try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(output)))) {

			// create buffer to store to be written bytes
			byte[] buf = new byte[1024];

			// task cancelled
			if (task.isCancelled())
				return;

			// get file name
			Path fileName = file.getFileName();
			if (fileName == null)
				throw new Exception("Cannot get file name.");

			// zip file
			zipFile(file, fileName.toString(), zos, buf, task);
		}
	}

	/**
	 * Zips given files to given output file.
	 *
	 * @param files
	 *            Files to zip.
	 * @param output
	 *            Output file path.
	 * @param task
	 *            The task calling this method.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	public static void zipFiles(ArrayList<Path> files, File output, InternalEquinoxTask<?> task) throws Exception {

		// update message
		task.updateMessage("Zipping files to '" + output.getName() + "'...");

		// create zip output stream
		try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(output)))) {

			// create buffer to store to be written bytes
			byte[] buf = new byte[1024];

			// loop over input files
			for (int i = 0; i < files.size(); i++) {

				// task cancelled
				if (task.isCancelled()) {
					break;
				}

				// get file
				Path file = files.get(i);

				// get file name
				Path fileName = file.getFileName();
				if (fileName == null)
					throw new Exception("Cannot get file name.");

				// zip file
				zipFile(file, fileName.toString(), zos, buf, task);

				// update progress
				if (files.size() > 3) {
					task.updateProgress(i, files.size());
				}
			}
		}
	}

	/**
	 * Zips given directory to given output file.
	 *
	 * @param directory
	 *            Directory to zip.
	 * @param output
	 *            Output file path.
	 * @param task
	 *            The task calling this method.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	public static void zipDirectory(Path directory, File output, InternalEquinoxTask<?> task) throws Exception {

		// update message
		task.updateMessage("Zipping files to '" + output.getName() + "'...");

		// create zip output stream
		try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(output)))) {

			// create buffer to store to be written bytes
			byte[] buf = new byte[1024];

			// create directory stream
			try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(directory)) {

				// get iterator
				Iterator<Path> iterator = dirStream.iterator();

				// loop over files
				while (iterator.hasNext()) {

					// get file
					Path file = iterator.next();

					// task cancelled
					if (task.isCancelled()) {
						break;
					}

					// get file name
					Path fileName = file.getFileName();
					if (fileName == null)
						throw new Exception("Cannot get file name.");

					// zip file
					zipFile(file, fileName.toString(), zos, buf, task);
				}
			}
		}
	}

	/**
	 * Zips given file recursively.
	 *
	 * @param path
	 *            Path to file.
	 * @param name
	 *            Name of file.
	 * @param zos
	 *            Zip output stream.
	 * @param buf
	 *            Byte buffer.
	 * @param task
	 *            The task calling this method.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static void zipFile(Path path, String name, ZipOutputStream zos, byte[] buf, InternalEquinoxTask<?> task) throws Exception {

		// update message
		task.updateMessage("Zipping file '" + path.getFileName() + "'...");

		// directory
		if (Files.isDirectory(path)) {

			// create and close new zip entry
			zos.putNextEntry(new ZipEntry(name + "/"));
			zos.closeEntry();

			// create directory stream
			try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(path)) {

				// get iterator
				Iterator<Path> iterator = dirStream.iterator();

				// loop over files
				while (iterator.hasNext()) {

					// get file
					Path file = iterator.next();

					// get file name
					Path fileName = file.getFileName();
					if (fileName == null)
						throw new Exception("Cannot get file name.");

					// zip file
					zipFile(file, name + "/" + fileName.toString(), zos, buf, task);
				}
			}
		}

		// file
		else {

			// create new zip entry
			zos.putNextEntry(new ZipEntry(name));

			// create stream to read file
			try (FileInputStream fis = new FileInputStream(path.toString())) {

				// read till the end of file
				int len;
				while ((len = fis.read(buf)) > 0) {
					zos.write(buf, 0, len);
				}
			}

			// close zip entry
			zos.closeEntry();
		}
	}

	/**
	 * Returns true if the given email address has name and domain.
	 *
	 * @param email
	 *            Email address string to check.
	 * @return True if the given email address has name and domain.
	 */
	private static boolean hasNameAndDomain(String email) {
		String[] tokens = email.split("@");
		return tokens.length == 2 && textHasContent(tokens[0]) && textHasContent(tokens[1]);
	}

	/**
	 * Return <tt>true</tt> only if <tt>aText</tt> is not null, and is not empty after trimming. (Trimming removes both leading/trailing whitespace and ASCII control characters. See {@link String#trim()}.)
	 *
	 * @param text
	 *            possibly-null.
	 * @return True if given text has content.
	 */
	private static boolean textHasContent(String text) {
		return text != null && text.trim().length() > 0;
	}
}
