import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Listener thread that keeps listening to a port and asks talker thread to process
 * when a request is accepted.
 * @author Chuan Xia
 *
 */

public class Listener extends Thread {

	private Node local;
	private ServerSocket serverSocket;
	private boolean alive;

	public Listener (Node n) {
		local = n;
		alive = true;
		InetSocketAddress localAddress = local.getAddress();
		int port = localAddress.getPort();

		//open server/listener socket
		try {
			serverSocket = new ServerSocket(port);
		} catch (IOException e) {
			throw new RuntimeException("\nCannot open listener port "+port+". Now exit.\n", e);
		}
	}

	@Override
	public void run() {
		while (alive) {
			Socket handleSoc = null;

			try {
				handleSoc = serverSocket.accept();
			} catch (IOException e) {
				throw new RuntimeException(
						"Can't accept the connection", e);
			}

			//new handler
			new Thread(new Handler(handleSoc, local)).start();
		}
	}

	public void toDie() {
		alive = false;
	}
}
