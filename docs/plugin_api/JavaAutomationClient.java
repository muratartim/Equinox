package equinox.utility;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.StringTokenizer;

public class JavaAutomationClient {

	public static void main(String[] args) throws Exception {

		// inputs: port number and path to instruction set
		int portNumber = 1789;
		String inputPath = "/Users/aurora/EclipseWorkspace/Equinox/docs/plugin_api/NewFile.xml";

		// create network socket, writer and reader
		try (Socket s = new Socket("localhost", portNumber); PrintWriter out = new PrintWriter(s.getOutputStream(), true); BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {

			// send run request
			out.println(inputPath + "|run");

			// wait for server response
			String response;
			while ((response = in.readLine()) != null) {

				// print server response
				System.out.println(response);

				// get response header
				String header = new StringTokenizer(response, "|").nextToken();

				// completed or failed
				if (header.equals("Completed") || header.equals("Failed")) {
					break;
				}
			}
		}
	}
}