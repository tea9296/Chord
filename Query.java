import java.net.InetSocketAddress;
import java.util.Scanner;

/**
 * Query class that offers the interface by which users can do 
 * search by querying a valid chord node.
 *
 *
 */

public class Query {

	private static InetSocketAddress localAddress;
	private static Helper helper;

	public static void main (String[] args) {

		helper = new Helper();
		String mode = "FINDRES_";
		// check the length of input for connection
		if(args.length != 3 && args.length != 2) {
			System.out.println("\nInvalid input. Now exit.\n");
			System.exit(0);
		}


		// try to connect socket address from args, if fails, exit
		localAddress = Helper.createSocketAddress(args[0]+":"+args[1]);
		if (localAddress == null) {
			System.out.println("Can't find the address you are trying to contact. Please insert valid ip address and port");
			System.exit(0);;
		}

		// check if the server we connect can response
		String response = Helper.sendRequest(localAddress, "KEEP");

		// if the server dead, exit
		if (response == null || !response.equals("ALIVE"))  {
			System.out.println("\nCan't connect the chord node. Now exit.\n");
			System.exit(0);
		}


		// if it's alive, print connection info
		System.out.println("\nConnection to node "+localAddress.getAddress().toString()+", port "+localAddress.getPort()+", position "+Helper.hexIdAndPosition(localAddress)+".");




		// set the mode for selecting next node for querying, default mode is 2
		if(args.length==3 && (args[2].equals("1") || args[2].equals("closest"))){
			mode = "FINDCLOS_";
			System.out.println("You choose finding closest node mode\n");
		}





		// check if system is stable
		boolean Ispre = false;
		boolean Issuc = false;
		InetSocketAddress preAddr = Helper.requestAddress(localAddress, "YOURPRE");
		InetSocketAddress sucAddr = Helper.requestAddress(localAddress, "YOURSUCC");
		if (preAddr == null || sucAddr == null) {
			System.out.println("The node your are contacting is disconnected. Now exit.");
			System.exit(0);
		}
		if (preAddr.equals(localAddress))
			Ispre = true;
		if (sucAddr.equals(localAddress))
			Issuc = true;

		// we suppose the system is stable if (1) this node has both valid
		// predecessor and successor or (2) none of them
		while (Ispre^Issuc) {
			System.out.println("Waiting for the system to be stable...");
			preAddr = Helper.requestAddress(localAddress, "YOURPRE");
			sucAddr = Helper.requestAddress(localAddress, "YOURSUCC");
			if (preAddr == null || sucAddr == null) {
				System.out.println("The node your are contacting is disconnected. Now exit.");
				System.exit(0);
			}
			if (preAddr.equals(localAddress))
				Ispre = true;
			else
				Ispre = false;
			if (sucAddr.equals(localAddress))
				Issuc = true;
			else
				Issuc = false;
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
			}

		}





		// begin to take user input
		Scanner userinput = new Scanner(System.in);
		while(true) {
			System.out.println("\nEnter a search key (or type \"quit\" to leave): ");
			String command = null;
			command = userinput.nextLine();

			// quit
			if (command.startsWith("quit")) {
				System.exit(0);
			}

			// search
			else if (command.length() > 0){
				long hash = Helper.hashString(command);
				int hopCount = 0;

				System.out.println("\nHash value is "+Long.toHexString(hash));


				InetSocketAddress result = Helper.requestAddress(localAddress, "FINDSUCC_"+hash);
				// fail to connect socket, exit query
				if (result == null) {
					System.out.println("The node your are contacting is disconnected. Now exit.");
					System.exit(0);
				}


				long target = Helper.hashSocketAddress(result);
				double timeRes = 0.0;
				long startTime = System.nanoTime();


				if(target ==Helper.hashSocketAddress(localAddress)){
					System.out.println("Current node"+localAddress.getAddress()+", port "+localAddress.getPort()+", position "+Helper.hexIdAndPosition(localAddress)+" is the node that saves the key");
				}

				else{


					// debug get result //
					System.out.println("RESULT:\t\t "+result.getAddress().toString()+", port "+result.getPort()+", position "+Helper.hexIdAndPosition(result )+"\n\n");


					// start to find the target node which save the key//


					InetSocketAddress cur = Helper.requestAddress(localAddress, mode+target);
					hopCount += 1;
					// print out response
					System.out.println("\nhop "+hopCount+". Starts from node "+localAddress.getAddress().toString()+", port "+localAddress.getPort()+", position "+Helper.hexIdAndPosition(localAddress)+":");

					// fail to connect socket, exit query
					if (cur == null) {
						System.out.println("The node your are contacting is disconnected. Now exit.");
						System.exit(0);
					}
					else if(cur.equals(localAddress)) cur = Helper.requestAddress(cur,"YOURSUCC");

					System.out.println("\tto Node "+cur.getAddress().toString()+", port "+cur.getPort()+", position "+Helper.hexIdAndPosition(cur )+"\n\n");


					// recursive to get target node //
					while(!result.equals(cur)){
						InetSocketAddress prev = cur;
						cur = Helper.requestAddress(prev, mode+Helper.hashSocketAddress(result));
						hopCount+=1;

						// fail to connect socket, exit query
						if (cur == null) {
							System.out.println("The node your are contacting is disconnected. Now exit.");
							System.exit(0);
						}
						// if prev equals to cur, set cur to it's successor
						else if(prev.equals(cur)){
							cur = Helper.requestAddress(cur,"YOURSUCC");
						}

						System.out.println("\nhop "+hopCount+". Starts from node "+prev.getAddress().toString()+", port "+prev.getPort()+", position "+Helper.hexIdAndPosition(prev)+":");
						System.out.println("\tto Node "+cur.getAddress().toString()+", port "+cur.getPort()+", position "+Helper.hexIdAndPosition(cur )+"\n\n");

					}
				}
				long elapsedTime = System.nanoTime() - startTime;
				timeRes = (double)elapsedTime/100000;
				System.out.println("total hops are: "+hopCount+" hops, total take "+timeRes+" ms.\n");


			}
		}

	}
}
