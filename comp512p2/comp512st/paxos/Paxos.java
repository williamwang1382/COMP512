package comp512st.paxos;

// Access to the GCL layer
import comp512.gcl.*;

import comp512.utils.*;

// Any other imports that you may need.
import java.io.*;
import java.util.logging.*;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;


// ANY OTHER classes, etc., that you add must be private to this package and not visible to the application layer.

// extend / implement whatever interface, etc. as required.
// NO OTHER public members / methods allowed. broadcastTOMsg, acceptTOMsg, and shutdownPaxos must be the only visible methods to the application layer.
//	You should also not change the signature of these methods (arguments and return value) other aspects maybe changed with reasonable design needs.
public class Paxos
{
	GCL gcl;
	FailCheck failCheck;
	ConcurrentLinkedQueue<GCMessage> outgoing;
	ConcurrentLinkedQueue<GCMessage> incoming;
	final int port;
	final int majority;

	Proposer proposer;

	public Paxos(String myProcess, String[] allGroupProcesses, Logger logger, FailCheck failCheck) throws IOException, UnknownHostException
	{
		// Rember to call the failCheck.checkFailure(..) with appropriate arguments throughout your Paxos code to force fail points if necessary.
		this.failCheck = failCheck;

		// Initialize the GCL communication system as well as anything else you need to.
		this.gcl = new GCL(myProcess, allGroupProcesses, null, logger) ;
		this.outgoing = new ConcurrentLinkedQueue<>();
		this.incoming = new ConcurrentLinkedQueue<>();
		this.port = Integer.parseInt(myProcess.split(":")[1]);
		this.majority = (allGroupProcesses.length /2) + 1;
	}

	// This is what the application layer is going to call to send a message/value, such as the player and the move
	public void broadcastTOMsg(Object val)
	{
		// This is just a place holder.
		// Extend this to build whatever Paxos logic you need to make sure the messaging system is total order.
		// Here you will have to ensure that the CALL BLOCKS, and is returned ONLY when a majority (and immediately upon majority) of processes have accepted the value.
		gcl.broadcastMsg(val);

		/*
		*	Paxos Implementation:
		* 	1. Messages will be placed in a thread-safe queue (ConcurrentLinkedQueue?)
		* 	2. A Proposer thread will continuously process messages in the queue
		* 	3. An Acceptor thread will continuously read paxosmessages, and place any confirmed messages into a thread-safe queue
		* 	**Each Process can only have one Proposer/Acceptor Thread running concurrently**
		*/		
	}

	// This is what the application layer is calling to figure out what is the next message in the total order.
	// Messages delivered in ALL the processes in the group should deliver this in the same order.
	public Object acceptTOMsg() throws InterruptedException
	{
		// This is just a place holder.
		GCMessage gcmsg = gcl.readGCMessage();
		return gcmsg.val;
	}

	// Add any of your own shutdown code into this method.
	public void shutdownPaxos()
	{
		gcl.shutdownGCL();
	}

	// Implements Serializable so it can be sent accross network using gcl.sendMsg(). 
	// Uses ENUM to identify types, contains all information each type of message requires
	// If some info not necessary, eg. "val" for "propose", the field is left as null. 
	private class PaxosMessage implements Serializable
	{
		/*  
		*	The MsgType will be piggybacked onto the messages so that processes can identify what the message type is when received
		* 	Message Types are based on the Paxos slides
		* 	DENY is used when the PROPOSE? ballot id suggested by the proposer is denied by a listener thread
		* 	REFUSE is used after the ballot id was accepted and the proposer sends an ACCEPT message with a value and a listener thread refuses
	 	*/ 
		private enum MsgType {
			PROPOSE, 
			PROMISE, 
			REFUSE,
			ACCEPT,
			ACCEPTACK,
			DENY,
			CONFIRM
		}

		/*
		 * Propose: SenderID, BID
		 * Promise: SenderID, BID -- OR -- BID, acceptedBID, acceptedVal(gcmessage)
		 * Refuse: SenderID, BID
		 * Accept: SenderID, BID, val
		 * AcceptAck: SenderID, BID
		 * Deny: SenderID, BID
		 * Confirm: SenderID, BID
		 */
		private MsgType type; //All Messages contain
    	private int BID;
    	private GCMessage val; //(A GCMessage)
		private String senderID;

		// This constructur is used for {Propose, Promise, 	}
		private PaxosMessage(MsgType type, int BID, GCMessage gcmessage, String senderprocess)
		{
			this.type = type;
			this.BID = BID;
			this.val = gcmessage;
			this.senderID = senderprocess;
		}
		
		/*
		 * Getter Methods:
		 * getType(), getBID(),getVal(), getSenderID(),
		 * Testing Aid: toString()
		 * No Setter Methods as PaxosMessage should be an immutable class
		 */
		public MsgType getType() {
			return type;
		}
	
		public int getBID() {
			return BID;
		}
	
	
		public GCMessage getVal() {
			return val;
		}
	
		public String getSenderID() {
			return senderID;
		}
	

		@Override
 		public String toString() {
        	return "PaxosMessage{" +
                "type=" + type +
                ", senderId=" + getSenderID() +
                ", BallotID=" + getBID() +
                '}';
		}
	}


	private class Proposer extends Thread
	{	
		//proposerID is used to generate unique BID's
		private int BID;
		private Object val;
		private int majority;
		private int proposerID;
		private int counter;

		private Proposer(int proposerID, int majority){
			this.BID = -1;
			this.val = null;
			this.majority = majority;
			this.proposerID = proposerID;
			this.counter = 0;
		}

		//The counter goes in the highest 20 bits, proposerID at the bottom.
		private int generateBID(){
			counter+=1;
			return (counter << 20) | proposerID;
			
		}

		@Override 
		public void run(){
			//While running, will constantly try to pull a message out of "outgoing", and if successful, initiates an instance of PAXOS
			while (true){
				GCMessage msg = outgoing.poll();

				if (msg != null) {
                    //Begin an instance of Paxos

                } else {
                    // No message available, pause for a short time
                    try {
                        Thread.sleep(50); // Avoid busy-waiting, adjust time as needed
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }

			}



		}
	}


	private class Acceptor extends Thread
	{	
		private int maxBID;
		private int acceptedBID;
		private Object acceptedVal;
		private final int acceptorID;
			
		//AcceptorID is just the process port
		private Acceptor(int acceptorID){
			this.acceptorID = acceptorID;
			this.maxBID = -1;
			this.acceptedBID = -1;
			this.acceptedVal = null;
		}

		@Override
		public void run(){
			while (true) {
				GCMessage msg = null;

				// Check if there is a message
				try {
					msg = gcl.readGCMessage();
				} catch (InterruptedException e){ // Catch an InterruptedException per the documentation in the doc
					e.printStackTrace();
				}

				// Extract the PaxosMessage from the message received, and respond. 
				PaxosMessage pxmsg = (PaxosMessage)(msg.val);
				

				switch (pxmsg.getType()){
					case PROPOSE:
						System.out.println("Handling PROPOSE message.");
						break;

					case PROMISE:
						System.out.println("Handling PROMISE message.");
						break;

					case REFUSE:
						System.out.println("Handling REFUSE message.");
						break;

					case ACCEPT:
						System.out.println("Handling ACCEPT message.");
						break;

					case ACCEPTACK:
						System.out.println("Handling ACCEPTACK message.");
						break;

					case DENY:
						System.out.println("Handling DENY message.");
						break;

					case CONFIRM:
						System.out.println("Handling CONFIRM message.");

						//Put in incoming queue

						break;

					default:
						System.out.println("Unknown message type.");
						break;
				}
			}

		}
	}
}
}