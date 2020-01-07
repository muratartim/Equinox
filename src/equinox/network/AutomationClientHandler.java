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
package equinox.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.logging.Level;

import org.apache.commons.lang3.RandomUtils;

import equinox.Equinox;
import equinox.exchangeServer.remote.data.ExchangeUser;
import equinox.plugin.FileType;
import equinox.task.RunInstructionSetOn;
import equinox.task.SaveTask;
import equinox.task.ShareInstructionSet;
import equinox.task.automation.CheckInstructionSet;
import equinox.task.automation.ConvertJSONFiletoXMLFile;
import equinox.task.automation.ConvertXMLFiletoJSONFile;
import javafx.application.Platform;

/**
 * Class for automation server client handler.
 *
 * @author Murat Artim
 * @date 23 Sep 2018
 * @time 23:34:08
 */
public class AutomationClientHandler implements Runnable {

	/** Client message token delimiter. */
	private static final String DELIMITER = "|";

	/** Command type. */
	private static final String RUN = "run", CHECK = "check", COMPILE = "compile", SAVE = "save", SCHEDULE = "schedule", SHARE = "share", RUN_ON = "runOn", CONVERT = "convert";

	/** Automation server. */
	private final AutomationServer server;

	/** Client socket to respond to. */
	private final Socket socket;

	/** Output writer. */
	private final PrintWriter out;

	/**
	 * Creates client handler.
	 *
	 * @param server
	 *            Automation server.
	 * @param socket
	 *            Client socket.
	 * @throws IOException
	 *             If exception occurs during creating output writer.
	 */
	public AutomationClientHandler(AutomationServer server, Socket socket) throws IOException {

		// set server and socket
		this.server = server;
		this.socket = socket;

		// create output writer
		out = new PrintWriter(socket.getOutputStream(), true);

		// log info
		Equinox.LOGGER.info("Automation client connection established from '" + socket.getInetAddress().toString() + "'.");
	}

	/**
	 * Notifies the automation client of task progress.
	 *
	 * @param requestId
	 *            Request Id.
	 * @param progress
	 *            Percent progress value.
	 */
	public void taskProgress(int requestId, double progress) {
		out.println("Progress|" + requestId + "|" + progress + "% complete.");
	}

	/**
	 * Notifies the automation client of task completion.
	 *
	 * @param requestId
	 *            Request Id.
	 */
	public void taskCompleted(int requestId) {
		out.println("Completed|" + requestId + "|Task completed.");
	}

	/**
	 * Notifies the automation client of task failure.
	 *
	 * @param requestId
	 *            Request Id.
	 */
	public void taskFailed(int requestId) {
		out.println("Failed|" + requestId + "|Task failed.");
	}

	@Override
	public void run() {

		// create writer and reader
		try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

			// send connected message
			out.println("Connected|Connected to automation server version " + Equinox.VERSION.toString() + ".");

			// block as long as client sends messages
			String request;
			while ((request = in.readLine()) != null) {

				// generate a request id
				int requestId = RandomUtils.nextInt(0, 10000);

				// build tokenizer
				StringTokenizer tokenizer = new StringTokenizer(request, DELIMITER);

				// check instruction set file path
				Path inputFile = getInputFile(tokenizer, out, requestId);
				if (inputFile == null) {
					continue;
				}

				// no token
				if (!tokenizer.hasMoreTokens()) {
					String response = "Failed|" + requestId + "|No command supplied to automation server.";
					out.println(response);
					Equinox.LOGGER.warning(response);
					continue;
				}

				// get command
				String command = tokenizer.nextToken().trim();

				// run
				switch (command) {
					case RUN:
						runCommand(inputFile, out, requestId);
						break;
					case CHECK:
						checkCommand(inputFile, out, requestId);
						break;
					case COMPILE:
						compileCommand(inputFile, out, requestId);
						break;
					case SAVE:
						saveCommand(inputFile, out, requestId);
						break;
					case SCHEDULE:
						scheduleCommand(inputFile, tokenizer, out, requestId);
						break;
					case SHARE:
						shareCommand(inputFile, tokenizer, out, requestId);
						break;
					case RUN_ON:
						runOnCommand(inputFile, tokenizer, out, requestId);
						break;
					case CONVERT:
						convertCommand(inputFile, out, requestId);
						break;
					default:
						String response = "Failed|" + requestId + "|Invalid command supplied to automation server.";
						out.println(response);
						Equinox.LOGGER.warning(response);
						break;
				}
			}
		}

		// exception occurred during process
		catch (Exception e) {
			Equinox.LOGGER.log(Level.WARNING, "Exception occurred during processing client inputs.", e);
		}

		// clean up
		finally {

			// close streams and client socket
			try {
				if (out != null) {
					out.close();
				}
				if (socket != null) {
					socket.close();
				}
			}

			// exception occurred during process
			catch (Exception e) {
				Equinox.LOGGER.log(Level.WARNING, "Exception occurred during client socket after client inputs processed.", e);
			}
		}
	}

	/**
	 * Performs convert command.
	 *
	 * @param inputFile
	 *            Path to input instruction set file.
	 * @param out
	 *            Output writer.
	 * @param requestId
	 *            Request id.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void convertCommand(Path inputFile, PrintWriter out, int requestId) throws Exception {

		// get file type
		FileType fileType = FileType.getFileType(inputFile.toFile());

		// convert to JSON
		if (fileType.equals(FileType.XML)) {
			Platform.runLater(() -> {
				Path outputJsonFile = inputFile.resolveSibling(FileType.getNameWithoutExtension(inputFile) + ".json");
				ConvertXMLFiletoJSONFile convert = new ConvertXMLFiletoJSONFile(inputFile, outputJsonFile);
				convert.setAutomationClientHandler(this, requestId);
				server.getOwner().getActiveTasksPanel().runTaskInParallel(convert);
			});
		}

		// convert to XML
		else if (fileType.equals(FileType.JSON)) {
			Platform.runLater(() -> {
				Path outputXmlFile = inputFile.resolveSibling(FileType.getNameWithoutExtension(inputFile) + ".xml");
				ConvertJSONFiletoXMLFile convert = new ConvertJSONFiletoXMLFile(inputFile, outputXmlFile);
				convert.setAutomationClientHandler(this, requestId);
				server.getOwner().getActiveTasksPanel().runTaskInParallel(convert);
			});
		}
		out.println("Submitted|" + requestId + "|Convert instruction set task submitted.");
	}

	/**
	 * Performs run-on command.
	 *
	 * @param inputFile
	 *            Path to input instruction set file.
	 * @param tokenizer
	 *            String tokenizer.
	 * @param out
	 *            Output writer.
	 * @param requestId
	 *            Request id.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void runOnCommand(Path inputFile, StringTokenizer tokenizer, PrintWriter out, int requestId) throws Exception {

		// no token
		if (!tokenizer.hasMoreTokens()) {
			String response = "Failed|" + requestId + "|No recipient username supplied for run instruction set on command.";
			out.println(response);
			Equinox.LOGGER.warning(response);
			return;
		}

		// get recipient username
		String username = tokenizer.nextToken().trim();

		// this user
		if (username.equals(Equinox.USER.getUsername())) {
			String response = "Failed|" + requestId + "|Invalid recipient username supplied for run instruction set on command.";
			out.println(response);
			Equinox.LOGGER.warning(response);
			return;
		}

		// recipient is offline
		if (!server.getOwner().isUserAvailable(username)) {
			String response = "Failed|" + requestId + "|Recipient supplied for run instruction set on command is not online.";
			out.println(response);
			Equinox.LOGGER.warning(response);
			return;
		}

		// get recipient
		ExchangeUser recipient = server.getOwner().getUser(username);
		if (recipient == null) {
			String response = "Failed|" + requestId + "|Invalid recipient username supplied for run instruction set on command.";
			out.println(response);
			Equinox.LOGGER.warning(response);
			return;
		}

		// submit
		Platform.runLater(() -> {
			RunInstructionSetOn run = new RunInstructionSetOn(inputFile, recipient);
			run.setAutomationClientHandler(this, requestId);
			server.getOwner().getActiveTasksPanel().runTaskInParallel(run);
		});
		out.println("Submitted|" + requestId + "|Run instruction set on task submitted.");
	}

	/**
	 * Performs share command.
	 *
	 * @param inputFile
	 *            Path to input instruction set file.
	 * @param tokenizer
	 *            String tokenizer.
	 * @param out
	 *            Output writer.
	 * @param requestId
	 *            Request id.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void shareCommand(Path inputFile, StringTokenizer tokenizer, PrintWriter out, int requestId) throws Exception {

		// no token
		if (!tokenizer.hasMoreTokens()) {
			String response = "Failed|" + requestId + "|No recipient username supplied for share instruction set command.";
			out.println(response);
			Equinox.LOGGER.warning(response);
			return;
		}

		// get recipient username
		String username = tokenizer.nextToken().trim();

		// this user
		if (username.equals(Equinox.USER.getUsername())) {
			String response = "Failed|" + requestId + "|Invalid recipient username supplied for share instruction set command.";
			out.println(response);
			Equinox.LOGGER.warning(response);
			return;
		}

		// recipient is offline
		if (!server.getOwner().isUserAvailable(username)) {
			String response = "Failed|" + requestId + "|Recipient supplied for share instruction set command is not online.";
			out.println(response);
			Equinox.LOGGER.warning(response);
			return;
		}

		// get recipient
		ExchangeUser recipient = server.getOwner().getUser(username);
		if (recipient == null) {
			String response = "Failed|" + requestId + "|Invalid recipient username supplied for share instruction set command.";
			out.println(response);
			Equinox.LOGGER.warning(response);
			return;
		}

		// submit
		Platform.runLater(() -> {
			ShareInstructionSet share = new ShareInstructionSet(inputFile, recipient);
			share.setAutomationClientHandler(this, requestId);
			server.getOwner().getActiveTasksPanel().runTaskInParallel(share);
		});
		out.println("Submitted|" + requestId + "|Share instruction set task submitted.");
	}

	/**
	 * Performs schedule command.
	 *
	 * @param inputFile
	 *            Path to input instruction set file.
	 * @param tokenizer
	 *            String tokenizer.
	 * @param out
	 *            Output writer.
	 * @param requestId
	 *            Request id.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void scheduleCommand(Path inputFile, StringTokenizer tokenizer, PrintWriter out, int requestId) throws Exception {

		// no token
		if (!tokenizer.hasMoreTokens()) {
			String response = "Failed|" + requestId + "|No date-time supplied for schedule task command.";
			out.println(response);
			Equinox.LOGGER.warning(response);
			return;
		}

		// get date-time
		String datetime = tokenizer.nextToken().trim();

		try {

			// parse date
			SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
			Date date = format.parse(datetime);

			// schedule
			Platform.runLater(() -> {
				SaveTask save = new SaveTask(new CheckInstructionSet(inputFile, CheckInstructionSet.RUN), date);
				save.setAutomationClientHandler(this, requestId);
				server.getOwner().getActiveTasksPanel().runTaskInParallel(save);
			});
			out.println("Submitted|" + requestId + "|Schedule instruction set task submitted.");
		}

		// exception occurred during parsing date
		catch (ParseException e) {
			String response = "Failed|" + requestId + "|Invalid date-time supplied for schedule task command. Please supply date-time in yyyy-MM-dd'T'HH:mm:ss format.";
			out.println(response);
			Equinox.LOGGER.warning(response);
			return;
		}
	}

	/**
	 * Performs save command.
	 *
	 * @param inputFile
	 *            Path to input instruction set file.
	 * @param out
	 *            Output writer.
	 * @param requestId
	 *            Request id.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void saveCommand(Path inputFile, PrintWriter out, int requestId) throws Exception {
		Platform.runLater(() -> {
			SaveTask save = new SaveTask(new CheckInstructionSet(inputFile, CheckInstructionSet.RUN), null);
			save.setAutomationClientHandler(this, requestId);
			server.getOwner().getActiveTasksPanel().runTaskInParallel(save);
		});
		out.println("Submitted|" + requestId + "|Save instruction set task submitted.");

	}

	/**
	 * Performs compile command.
	 *
	 * @param inputFile
	 *            Path to input instruction set file.
	 * @param out
	 *            Output writer.
	 * @param requestId
	 *            Request id.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void compileCommand(Path inputFile, PrintWriter out, int requestId) throws Exception {
		Platform.runLater(() -> {
			CheckInstructionSet check = new CheckInstructionSet(inputFile, CheckInstructionSet.GENERATE_EXECUTION_PLAN);
			check.setAutomationClientHandler(this, requestId);
			server.getOwner().getActiveTasksPanel().runTaskInParallel(check);
		});
		out.println("Submitted|" + requestId + "|Compile instruction set task submitted.");
	}

	/**
	 * Performs check command.
	 *
	 * @param inputFile
	 *            Path to input instruction set file.
	 * @param out
	 *            Output writer.
	 * @param requestId
	 *            Request id.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void checkCommand(Path inputFile, PrintWriter out, int requestId) throws Exception {
		Platform.runLater(() -> {
			CheckInstructionSet check = new CheckInstructionSet(inputFile, CheckInstructionSet.CHECK);
			check.setAutomationClientHandler(this, requestId);
			server.getOwner().getActiveTasksPanel().runTaskInParallel(check);
		});
		out.println("Submitted|" + requestId + "|Check instruction set task submitted.");
	}

	/**
	 * Performs run command.
	 *
	 * @param inputFile
	 *            Path to input instruction set file.
	 * @param out
	 *            Output writer.
	 * @param requestId
	 *            Request id.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private void runCommand(Path inputFile, PrintWriter out, int requestId) throws Exception {
		Platform.runLater(() -> {
			CheckInstructionSet check = new CheckInstructionSet(inputFile, CheckInstructionSet.RUN);
			check.setAutomationClientHandler(this, requestId);
			server.getOwner().getActiveTasksPanel().runTaskInParallel(check);
		});
		out.println("Submitted|" + requestId + "|Run instruction set task submitted.");
	}

	/**
	 * Parses and returns path to input instruction set file, or null if no valid path could be parsed from client message.
	 *
	 * @param tokenizer
	 *            String tokenizer.
	 * @param out
	 *            Output writer.
	 * @param requestId
	 *            Request id.
	 * @return Path to input instruction set file, or null if no valid path could be parsed from client message.
	 * @throws Exception
	 *             If exception occurs during process.
	 */
	private static Path getInputFile(StringTokenizer tokenizer, PrintWriter out, int requestId) throws Exception {

		// no token
		if (!tokenizer.hasMoreTokens()) {
			String response = "Failed|" + requestId + "|No instruction set file path supplied to automation server.";
			out.println(response);
			Equinox.LOGGER.warning(response);
			return null;
		}

		// get path to instruction set
		Path inputFile = Paths.get(tokenizer.nextToken().trim());

		// invalid path
		if (inputFile == null || !Files.exists(inputFile) || !Files.isRegularFile(inputFile)) {
			String response = "Failed|" + requestId + "|Invalid instruction set file path supplied to automation server.";
			out.println(response);
			Equinox.LOGGER.warning(response);
			return null;
		}

		// invalid file type
		FileType fileType = FileType.getFileType(inputFile.toFile());
		if (fileType == null || !fileType.equals(FileType.XML) && !fileType.equals(FileType.JSON)) {
			String response = "Failed|" + requestId + "|Invalid instruction set file type supplied to automation server. Please supply XML or JSON file.";
			out.println(response);
			Equinox.LOGGER.warning(response);
			return null;
		}

		// return file
		return inputFile;
	}
}