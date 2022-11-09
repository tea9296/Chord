import java.net.InetSocketAddress;
import java.util.HashMap;

/**
 * Node class that implements the node data structure
 * and functionalities of a chord node includes update finger table and response table,
 * find predecessor and successor, key query functions(based on closest node or lowest response time),
 * and print finger table information.
 *
 *
 */

public class Node {

	private long localId;
	private InetSocketAddress localAddress;
	private InetSocketAddress predecessor;
	private HashMap<Integer, InetSocketAddress> finger;

	private HashMap<Integer, Long> responseTime;
	private Listener listener;

	private Heartbeats heartBeat;


	/**
	 * Constructor
	 * @param address: this node's local address
	 */
	public Node (InetSocketAddress address) {

		localAddress = address;
		localId = Helper.hashSocketAddress(localAddress);

		// initialize an empty finge table
		finger = new HashMap<Integer, InetSocketAddress>();
		responseTime = new HashMap<Integer,Long>();
		for (int i = 1; i <= 32; i++) {
			updateIthFinger (i, null);

		}

		// initialize predecessor
		predecessor = null;

		// initialize threads
		listener = new Listener(this);
		heartBeat = new Heartbeats(this);

	}

	/**
	 * Create or join a ring 
	 * @param contact
	 * @return true if successfully create a ring
	 * or join a ring via contact
	 */
	public boolean join (InetSocketAddress contact) {

		// if contact is other node (join ring), try to contact that node
		// (contact will never be null)
		if (contact != null && !contact.equals(localAddress)) {
			InetSocketAddress successor = Helper.requestAddress(contact, "FINDSUCC_" + localId);
			if (successor == null)  {
				System.out.println("\nCannot find node you are trying to contact. Please exit.\n");
				return false;
			}
			updateIthFinger(1, successor);
		}
		InetSocketAddress prevSock = null;
		long prevTime = 0;


		// build finger table  and  response time table//
		for(int i=1;i<=32;i++){
			long startTime = System.nanoTime();
			InetSocketAddress ithFinger = findSuccessor(Helper.ithStart(getId(),i));
			long elapsedTime = System.nanoTime() - startTime;
			if(prevSock!=null && prevSock==ithFinger) elapsedTime = prevTime;
			updateFingers(i,ithFinger);
			responseTime.put(i,elapsedTime);
			//updateResponse(i,elapsedTime);
			prevSock = ithFinger;
			prevTime = elapsedTime;
		}


		// start all threads	
		listener.start();

		heartBeat.start();


		return true;
	}

	/**
	 * update the response time of ith server in the finger table
	 * @param i int value ith key
	 * @param addr the socket address of the node we want to test response time
	 */
	private void updateResponse(int i, InetSocketAddress addr){
		long Rtime = -1;
		if(addr==null){
			//responseTime.put(i,Rtime);
			//return;
		}
		else if(addr.equals(localAddress)){
			//responseTime.put(i,(long)0);
			Rtime = 0;
		}
		else if(Rtime==-1){
			long startTime = System.nanoTime();
			String respon = Helper.sendRequest(addr,"KEEP");
			//InetSocketAddress target = find_successor(Helper.hashSocketAddress(addr)-1);
			//InetSocketAddress target = Helper.requestAddress(addr,"KEEP");
			if(respon!=null) Rtime = System.nanoTime() - startTime;

		}


		for(int j=1;j<=32;j++){
			InetSocketAddress cur = finger.get(j);
			if(cur==null) responseTime.put(j,(long)-1);
			else if(cur.equals(addr)) responseTime.put(j,Rtime);
			else if(j==i) responseTime.put(j,Rtime);
		}

	}


	/**
	 * Notify successor that this node should be its predecessor
	 * @param successor
	 * @return successor's response
	 */
	public String notify(InetSocketAddress successor) {
		if (successor!=null && !successor.equals(localAddress))
			return Helper.sendRequest(successor, "IAMPRE_"+localAddress.getAddress().toString()+":"+localAddress.getPort());
		else
			return null;
	}

	/**
	 * Being notified by another node, set it as my predecessor if it is.
	 * @param newpre
	 */
	public void notified (InetSocketAddress newpre) {
		if (predecessor == null || predecessor.equals(localAddress)) {
			this.setPredecessor(newpre);
		}
		else {
			long oldpre_id = Helper.hashSocketAddress(predecessor);
			long local_relative_id = Helper.computeRelativeId(localId, oldpre_id);
			long newpre_relative_id = Helper.computeRelativeId(Helper.hashSocketAddress(newpre), oldpre_id);
			if (newpre_relative_id > 0 && newpre_relative_id < local_relative_id)
				this.setPredecessor(newpre);
		}
	}


	/**
	 * From the finger table of the node, choose the closest node that small or equal target id
	 * @param id
	 * @return ip address and port
	 */
	public InetSocketAddress findClosestNode (long id){
		long target = Helper.computeRelativeId(id,localId);  // get distance
		//InetSocketAddress targetAddr = null;

		// from larget to small to check finger table
		for(int i=32; i>=1;i--){
			InetSocketAddress ithFinger = finger.get(i);
			if (ithFinger == null) {
				continue;
			}
			long ithFingerID = Helper.hashSocketAddress(ithFinger);
			long ithFingerRela = Helper.computeRelativeId(ithFingerID,localId);

			if(ithFingerRela>0 && ithFingerRela<target ){

				String respon = Helper.sendRequest(ithFinger,"KEEP");
				if(respon!=null) return ithFinger;
				else updateFingers(-2,ithFinger);
			}
		}


		return localAddress;
	}

	public InetSocketAddress findLeastResponseTime(long id){

		long target = Helper.computeRelativeId(id,localId);  // get distance
		InetSocketAddress targetAddr = null;
		long targetTime = Long.MAX_VALUE;

		// from larget to small to check finger table
		for(int i=1; i<=32;i++){
			InetSocketAddress ithFinger = finger.get(i);
			if (ithFinger == null) {
				continue;
			}
			long ithFingerID = Helper.hashSocketAddress(ithFinger);
			long ithFingerRela = Helper.computeRelativeId(ithFingerID,localId);
			long ithTime = responseTime.get(i);
			if(ithFingerRela > 0 && ithFingerRela < target && ithTime < targetTime){

				//String respon = Helper.sendRequest(ithFinger,"KEEP");
				//if(respon!=null){
				targetAddr = ithFinger;
				targetTime = ithTime;
				//}
				//else updateFingers(-2,ithFinger);
			}
		}
		System.out.println(targetTime);
		if(targetTime!=Long.MAX_VALUE) return targetAddr;


		return localAddress;
	}








	/**
	 * Ask current node to find id's successor.
	 * @param id
	 * @return id's successor's socket address
	 */
	public InetSocketAddress findSuccessor(long id) {

		// initialize return value as this node's successor (might be null)
		InetSocketAddress res = this.getSuccessor();

		// find predecessor
		InetSocketAddress pre = findPredecessor(id);

		// if other node found, ask it for its successor
		if (!pre.equals(localAddress))
			res = Helper.requestAddress(pre, "YOURSUCC");

		// if res is still null, set it as local node, return
		if (res == null)
			res = localAddress;

		return res;
	}






	/**
	 * Calculate relative distance between current node and target id to find id's predecessor
	 * @param id
	 * @return id's successor's socket address
	 */
	private InetSocketAddress findPredecessor(long id) {
		InetSocketAddress cur = this.localAddress;

		InetSocketAddress mostRecLlive = this.localAddress;
		InetSocketAddress curSuc = this.getSuccessor();
		long curSucID = 0;
		if (curSuc != null)
			curSucID = Helper.computeRelativeId(Helper.hashSocketAddress(curSuc), Helper.hashSocketAddress(cur));

		long idRelatID = Helper.computeRelativeId(id, Helper.hashSocketAddress(cur));

		while (!(idRelatID > 0 && idRelatID <= curSucID)) {

			// temporarily save current node
			InetSocketAddress preCur = cur;

			// if current node is local node, find my closest
			if (cur.equals(this.localAddress)) {

				cur = this.findClosestNode(id);
			}

			// else current node is remote node, sent request to it for its closest
			else {
				InetSocketAddress result = Helper.requestAddress(cur, "FINDCLOS_" + id);

				// if fail to get response, set cur to most recently
				if (result == null) {
					cur = mostRecLlive;
					curSuc = Helper.requestAddress(cur, "YOURSUCC");
					if (curSuc==null) {
						System.out.println("It's not possible.");
						return localAddress;
					}
					continue;
				}

				// if cur's closest is itself, return cur
				else if (result.equals(cur))
					return result;

				// else cur's closest is other node "result"
				else {	
					// set cur as most recently alive
					mostRecLlive = cur;
					// ask "result" for its successor
					curSuc = Helper.requestAddress(result, "YOURSUCC");
					// if we can get its response, then "result" must be our next cur
					if (curSuc!=null) {
						cur = result;
					}
					// else cur sticks, ask cur's successor
					else {
						curSuc = Helper.requestAddress(cur, "YOURSUCC");
					}
				}

				// compute relative ids for while loop judgement
				curSucID = Helper.computeRelativeId(Helper.hashSocketAddress(curSuc), Helper.hashSocketAddress(cur));
				idRelatID = Helper.computeRelativeId(id, Helper.hashSocketAddress(cur));
			}
			if (preCur.equals(cur))
				break;
		}
		return cur;
	}


	/**
	 * Update the finger table based on parameters.
	 * Synchronize, all threads trying to modify 
	 * finger table only through this method. 
	 * @param i: index or command code
	 * @param value
	 */
	public synchronized void updateFingers(int i, InetSocketAddress value) {

		// valid index in [1, 32], just update the ith finger
		if (i >= 1  && i <= 32) {
			updateIthFinger(i, value);
		}

		// caller wants to delete the successor
		else if (i == -1) {

			InetSocketAddress suc = getSuccessor();
			if(suc==null) return;

			// if predeccessor equals to successor, delete it
			if(predecessor!=null && predecessor.equals(suc)) setPredecessor(null);


			//delete the node from the finger table into null if it's equals to successor
			for(int j=1;j<=32;j++){
				InetSocketAddress ithFinger = finger.get(j);
				if(ithFinger==null) continue;
				else if(ithFinger.equals(suc)) updateIthFinger(j,null);
			}


			// try to update new successor base on current finger table
			fillSuccessor();
			suc = getSuccessor();


			// fill the successor by the predecessor or its predecessor if the successor still null
			if(suc==null && predecessor!=null && !predecessor.equals(localAddress)){
				InetSocketAddress pre = predecessor;
				InetSocketAddress prePre = null;

				while(true){

					prePre = Helper.requestAddress(pre,"YOURPRE");
					if(prePre==null) break;

					if(prePre.equals(pre) || prePre.equals(localAddress) || prePre.equals(suc)) break;
					else pre = prePre;
				}

				updateIthFinger(1, pre);
			}

		}




		// caller wants to delete a finger in table
		else if (i == -2) {


			for(int j=1;j<=32;j++){
				InetSocketAddress ithFinger = finger.get(j);
				if(ithFinger==null) continue;
				else if(ithFinger.equals(value)) updateIthFinger(j,null);
			}

		}

		// caller wants to fill successor
		else if (i == -3) {
			fillSuccessor();
		}

	}

	/**
	 * Update ith finger in finger table using new value
	 * @param i: index
	 * @param value node value
	 */
	private void updateIthFinger(int i, InetSocketAddress value) {
		finger.put(i, value);
		updateResponse(i,value);
		// if the updated one is successor, notify the new successor
		if (i == 1 && value != null && !value.equals(localAddress)) {
			notify(value);
		}
	}




	/**
	 * Try to fill successor with candidates in finger table or even predecessor
	 */
	private void fillSuccessor() {
		InetSocketAddress suc = this.getSuccessor();
		if (suc == null || suc.equals(localAddress)) {
			for (int i = 2; i <= 32; i++) {
				InetSocketAddress ithFinger = finger.get(i);
				if (ithFinger!=null && !ithFinger.equals(localAddress)) {
					for (int j = i-1; j >=1; j--) {
						updateIthFinger(j, ithFinger);
					}
					break;
				}
			}
		}
		suc = getSuccessor();
		if ((suc == null || suc.equals(localAddress)) && predecessor!=null && !predecessor.equals(localAddress)) {
			updateIthFinger(1, predecessor);
		}

	}



	/**
	 * Set predecessor using a new value.
	 * @param pre
	 */
	public synchronized void setPredecessor(InetSocketAddress pre) {
		predecessor = pre;
	}


	/**
	 * Get private variables
	 * @return the variable caller wants
	 */


	public InetSocketAddress getAddress() {
		return localAddress;
	}

	public long getId() {
		return localId;
	}




	public InetSocketAddress getSuccessor() {
		if (finger != null && finger.size() > 0) {
			return finger.get(1);
		}
		return null;
	}

	public InetSocketAddress getPredecessor() {
		return predecessor;
	}



	/**
	 * Print the predecessor and the successor of the node
	 */

	public void printNeighbors () {
		System.out.println("\nYou are listening on port "+localAddress.getPort()+"."
				+ "\nYour position is "+Helper.hexIdAndPosition(localAddress)+".");
		InetSocketAddress successor = finger.get(1);
		
		// if it cannot find both predecessor and successor
		if ((predecessor == null || predecessor.equals(localAddress)) && (successor == null || successor.equals(localAddress))) {
			System.out.println("Your predecessor is yourself.");
			System.out.println("Your successor is yourself.");

		}
		
		// else, it can find either predecessor or successor
		else {
			if (predecessor != null) {
				System.out.println("Your predecessor is node "+predecessor.getAddress().toString()+", "
						+ "port "+predecessor.getPort()+ ", position "+Helper.hexIdAndPosition(predecessor)+".");
			}
			else {
				System.out.println("Your predecessor is updating.");
			}

			if (successor != null) {
				System.out.println("Your successor is node "+successor.getAddress().toString()+", "
						+ "port "+successor.getPort()+ ", position "+Helper.hexIdAndPosition(successor)+".");
			}
			else {
				System.out.println("Your successor is updating.");
			}
		}
	}




	/**
	 * Print current node's Chord finger table and the response time
	 */
	public void printDataStructure () {
		System.out.println("\n==============================================================");
		System.out.println("\nLOCAL:\t\t\t\t"+localAddress.toString()+"\t"+Helper.hexIdAndPosition(localAddress));
		if (predecessor != null)
			System.out.println("\nPREDECESSOR:\t\t\t"+predecessor.toString()+"\t"+Helper.hexIdAndPosition(predecessor));
		else 
			System.out.println("\nPREDECESSOR:\t\t\tNULL");
		System.out.println("\nFINGER TABLE:\n");
		for (int i = 1; i <= 32; i++) {
			long ithstart = Helper.ithStart(Helper.hashSocketAddress(localAddress),i);
			InetSocketAddress f = finger.get(i);
			StringBuilder sb = new StringBuilder();
			sb.append(i+"\t"+ Helper.longTo8DigitHex(ithstart)+"\t\t");
			// node ip and hex code and percentage
			if (f!= null)
				sb.append(f.toString()+"\t"+Helper.hexIdAndPosition(f));

			else 
				sb.append("NULL");

			sb.append("\t"+responseTime.get(i).toString()+"ns");

			System.out.println(sb.toString());
		}
		System.out.println("\n==============================================================\n");
	}

	/**
	 * Stop this node's all threads.
	 */
	public void stopAllThreads() {
		if (listener != null)
			listener.toDie();
		if (heartBeat != null)
			heartBeat.toDie();

	}

}
