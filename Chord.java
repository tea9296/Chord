import java.util.Scanner;
import java.net.UnknownHostException;
import java.net.InetAddress;
import java.net.InetSocketAddress;



/**
 * Main program to create a chord cycle or join a node into a chord cycle
 * (hash ip address+port number into 32 bits name)
 * usage:
 * 		new chord : Chord (port)
 * 		join chord : Chord (port) (ip address we want to join) (port number we want to join)
 *
 */

public class Chord {

	private static InetSocketAddress conn;
	private static Node node;

	private static Helper helpers;

	public static void main (String[] args) {
		
		helpers = new Helper();

		
		// get current ip address
		String curIp = null;
		try {
			curIp = InetAddress.getLocalHost().getHostAddress();

		} catch (UnknownHostException e1) {

			e1.printStackTrace();
		}
		
		// create node (ip + port num)
		node = new Node (Helper.createSocketAddress(curIp+":"+args[0]));
		
		// determine it's creating new chord or join chord
		if (args.length == 1) {
			conn = node.getAddress();
		}
		
		// join, contact is another node
		else if (args.length == 3) {
			conn = Helper.createSocketAddress(args[1]+":"+args[2]);
			if (conn == null) {
				System.out.println("\n\nCan't find the input ip address and port number, exiting.\n\n");
				System.exit(0);
			}	
		}
		
		else {
			System.out.println("\n\nInput format error, exiting.");
			System.exit(0);
		}
		
		// try to join ring from contact node
		boolean successJoin = node.join(conn);
		
		// fail to join contact node
		if (!successJoin) {
			System.out.println("\n\nFail to connect the node we try to join, exiting.\n\n");
			System.exit(0);
		}
		
		// print join info
		System.out.println("Join the Chord successfully.");
		System.out.println("Current IP address: "+curIp);
		node.printNeighbors();
		
		// begin to take user input, "info" or "quit"
		Scanner userinput = new Scanner(System.in);
		while(true) {
			System.out.println("\nType \"table\" to get the node's finger table \nType \"quit\"to leave the chord ring: ");

			String userInput = "";
			userInput = userinput.next();
			if (userInput.startsWith("quit")) {
				node.stopAllThreads();
				System.out.println("Successfully Leaves the chord ring...");
				System.exit(0);
				
			}
			else if (userInput.startsWith("table")) {
				node.printDataStructure();
			}
			else{
				System.out.println("Can't find the command \""+userInput+"\"");

			}
		}
	}
}
