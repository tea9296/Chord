import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Handler thread which processes request from listener and writes
 * response to socket.
 * Accepted requests: "KEEP" "YOURSUCC" "YOURPRE" "FINDSUCC" "IAMPRE" "FINDCLOS" "FINDRES"
 *
 */

public class Handler implements Runnable{

	Socket talkSoc;
	private Node localNode;

	public Handler(Socket handSoc, Node loc)
	{
		talkSoc = handSoc;
		localNode = loc;
	}
	@Override
	public void run()
	{
		InputStream input = null;
		OutputStream output = null;
		try {
			input = talkSoc.getInputStream();
			String request = Helper.inputStreamToString(input);
			String response = processRequest(request);

			if (response != null) {
				output = talkSoc.getOutputStream();
				output.write(response.getBytes());
			}
			input.close();
		} catch (IOException e) {
			throw new RuntimeException(
					"Can't respond.\nServer port: "+ localNode.getAddress().getPort()+"; handler port: "+ talkSoc.getPort(), e);
		}
	}

	private String processRequest(String request)
	{

		InetSocketAddress result = null;
		String ret = null;
		if (request  == null) {
			return null;
		}

		if (request.startsWith("YOURSUCC")) {
			result = localNode.getSuccessor();
			if (result != null) {
				String ip = result.getAddress().toString();
				int port = result.getPort();
				ret = "MYSUCC_"+ip+":"+port;
			}
			else {
				ret = "NOTHING";
			}
		}
		else if (request.startsWith("YOURPRE")) {
			result = localNode.getPredecessor();
			if (result != null) {
				String ip = result.getAddress().toString();
				int port = result.getPort();
				ret = "MYPRE_"+ip+":"+port;
			}
			else {
				ret = "NOTHING";
			}
		}
		else if (request.startsWith("FINDSUCC")) {
			long id = Long.parseLong(request.split("_")[1]);
			result = localNode.findSuccessor(id);
			String ip = result.getAddress().toString();
			int port = result.getPort();
			ret = "FOUNDSUCC_"+ip+":"+port;

		}
		else if (request.startsWith("IAMPRE")) {
			InetSocketAddress new_pre = Helper.createSocketAddress(request.split("_")[1]);
			localNode.notified(new_pre);
			ret = "NOTIFIED";
		}
		else if (request.startsWith("KEEP")) {
			ret = "ALIVE";
		}
		else if (request.startsWith("FINDCLOS")){
			long id = Long.parseLong(request.split("_")[1]);
			result = localNode.findClosestNode(id);
			String ip = result.getAddress().toString();
			int port = result.getPort();
			ret = "FOUNDCLOS_"+ip+":"+port;

		}
		else if (request.startsWith("FINDRES")){
			long id = Long.parseLong(request.split("_")[1]);
			result = localNode.findLeastResponseTime(id);
			String ip = result.getAddress().toString();
			int port = result.getPort();
			ret = "FOUNDRES_"+ip+":"+port;

		}
		return ret;
	}
}