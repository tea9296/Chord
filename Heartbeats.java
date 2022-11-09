import java.util.Random;
import java.net.InetSocketAddress;


/**
 * Heartbeats thread periodically check if predecessor and successor alive, and randomly check
 * a key of finger table is correct or not.
 *
 *
 */

public class Heartbeats extends Thread{

	private Node local;
	Random random;
	boolean alive;

	public Heartbeats(Node node) {
		local = node;
		alive = true;
		random = new Random();
	}


	// num =  random number from 1 to 32 //
	@Override
	public void run() {
		while (alive) {
			int interval = 200;

			checkPredecessor();
			checkSuccessor();
			checkFingerTable();


			// sleep //
			try {
				Thread.sleep(interval);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}
	}

	/**
	 * randomly check a key of finger table to see if it's the right key
	 */
	private void checkFingerTable(){
		int num = random.nextInt(31) + 2;
		InetSocketAddress ithFinger = local.findSuccessor(Helper.ithStart(local.getId(), num));
		local.updateFingers(num, ithFinger);

	}


	/**
	 * check if predecessor is alive
	 */
	private void checkPredecessor() {
		InetSocketAddress pred = local.getPredecessor();
		if(pred!=null){

			String rev = Helper.sendRequest(pred,"KEEP");

			if(rev == null){
				local.setPredecessor(null);
			}

		}
	}


	/**
	 * check and update successor
	 */
	private void checkSuccessor(){
		InetSocketAddress suc = local.getSuccessor();

		// fill successor if it's null
		if(suc == null || suc.equals(local.getAddress())){
			local.updateFingers(-3,null);
			suc = local.getSuccessor();
		}

		if (suc != null && !suc.equals(local.getAddress())) {

			// delete successor if the connection fail
			InetSocketAddress pre = Helper.requestAddress(suc,"YOURPRE");
			if(pre == null) local.updateFingers(-1,null);

			// update successor if the successor's predecessor closer to current node
			else if(!pre.equals(suc)){
				long localID = Helper.hashSocketAddress(local.getAddress());
				long preRID = Helper.computeRelativeId(Helper.hashSocketAddress(pre),localID);
				long sucRID = Helper.computeRelativeId(Helper.hashSocketAddress(suc),localID);
				if(preRID > 0 && sucRID > preRID) local.updateFingers(1,pre);
			}

			else local.notify(suc);


		}
	}

	public void toDie() {
		alive = false;
	}

}
