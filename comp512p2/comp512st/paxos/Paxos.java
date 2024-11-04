package comp512st.paxos;

// Access to the GCL layer
import comp512.gcl.*;

import comp512.utils.*;

// Any other imports that you may need.
import java.io.*;
import java.util.logging.*;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.*;
import java.util.stream.Collectors;


// ANY OTHER classes, etc., that you add must be private to this package and not visible to the application layer.

// extend / implement whatever interface, etc. as required.
// NO OTHER public members / methods allowed. broadcastTOMsg, acceptTOMsg, and shutdownPaxos must be the only visible methods to the application layer.
//	You should also not change the signature of these methods (arguments and return value) other aspects maybe changed with reasonable design needs.
public class Paxos
{
	GCL gcl;
	FailCheck failCheck;
	LinkedBlockingQueue<Object> incoming;
	// final int port;
	final int majority;


	Acceptor acceptor;
	int count;
	int maxBID;

	int promises;
	int refuses;
	int acceptacks;
	int denies;
	int processNum;

	int prevID;

	Object newMove;


	int paxosID;




	public Paxos(String myProcess, String[] allGroupProcesses, Logger logger, FailCheck failCheck) throws IOException, UnknownHostException
	{
		// Rember to call the failCheck.checkFailure(..) with appropriate arguments throughout your Paxos code to force fail points if necessary.
		this.failCheck = failCheck;

		// paxosID is determined by the order in which each process is listed in allGroupProcesses
		this.paxosID = Arrays.asList(allGroupProcesses).indexOf(myProcess);

		incoming = new LinkedBlockingQueue<>();
		// this.port = Integer.parseInt(myProcess.split(":")[1]);
		this.majority = (allGroupProcesses.length /2) + 1;
		// this.majority = (allGroupProcesses.length /2);
		System.out.println("Majority value: "+ majority);
		this.count = 0;
		this.maxBID = 0;

		this.processNum = allGroupProcesses.length;

		// Initialize the GCL communication system as well as anything else you need to.
		this.gcl = new GCL(myProcess, allGroupProcesses, null, logger) ;
		acceptor = new Acceptor();
		acceptor.start();
	}



	private int generateBID(){

		// TODO: think about whether we should instead implement a scaling factor for the increment so that the increment value increases for each propose attempt failed for the same message
		
		// Increment the maxBID by the smallest multiple of processNum that is >= maxBID
		double x = (double) maxBID / (double) processNum;

		int increment = (int)(x) * processNum;
		return paxosID + increment;
		
	}

	// This is what the application layer is going to call to send a message/value, such as the player and the move
	public void broadcastTOMsg(Object val)
	{


		// This is just a place holder.
		// Extend this to build whatever Paxos logic you need to make sure the messaging system is total order.
		// Here you will have to ensure that the CALL BLOCKS, and is returned ONLY when a majority (and immediately upon majority) of processes have accepted the value.
		// gcl.broadcastMsg(val);

		/*
		*	Paxos Implementation:
		* 	1. Messages will be placed in a thread-safe queue (ConcurrentLinkedQueue?)
		* 	2. A Proposer thread will continuously process messages in the queue
		* 	3. An Acceptor thread will continuously read paxosmessages, and place any confirmed messages into a thread-safe queue
		* 	**Each Process can only have one Proposer/Acceptor Thread running concurrently**
		*/	
		maxBID = generateBID();
		System.out.println("New move id: "+ maxBID+" and value "+ val);
		newMove = val;
		PaxosMessage msg = new PaxosMessage(MsgType.PROPOSE, maxBID, 0, null, String.valueOf(paxosID));
		gcl.broadcastMsg(msg);
		failCheck.checkFailure(FailCheck.FailureType.AFTERSENDPROPOSE);		
		
		// Initialize the counters
		this.promises = 0;
		this.refuses = 0;
		this.acceptacks = 0;
		this.denies = 0;
	}

	// This is what the application layer is calling to figure out what is the next message in the total order.
	// Messages delivered in ALL the processes in the group should deliver this in the same order.
	public Object acceptTOMsg() throws InterruptedException
	{

		return incoming.take();
	}

	// Add any of your own shutdown code into this method.
	public void shutdownPaxos()
	{
		gcl.shutdownGCL();
	}

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


	// Implements Serializable so it can be sent accross network using gcl.sendMsg(). 
	// Uses ENUM to identify types, contains all information each type of message requires
	// If some info not necessary, eg. "val" for "propose", the field is left as null. 
	private static class PaxosMessage implements Serializable
	{

		/*
		 * Propose: SenderID, BID
		 * Promise: SenderID, BID -- OR -- BID, acceptedBID, acceptedVal(gcmessage)
		 * Refuse: SenderID, BID
		 * Accept: SenderID, BID, val
		 * AcceptAck: SenderID, BID
		 * Deny: SenderID, BID
		 * Confirm: SenderID, BID
		 */
		MsgType type; //All Messages contain
    	int BID;
		int BID2;
    	Object val;
		String senderID;

		// This constructur is used for {Propose, Promise, 	}
		PaxosMessage(MsgType type, int BID, int BID2, Object val, String senderprocess)
		{
			this.type = type;
			this.BID = BID;
			this.BID2 = BID2;
			this.val = val;
			this.senderID = senderprocess;
		}
		
		/*
		 * Getter Methods:
		 * getType(), getBID(),getVal(), getSenderID(),
		 * Testing Aid: toString()
		 * No Setter Methods as PaxosMessage should be an immutable class
		 */
		MsgType getType() {
			return this.type;
		}
	
		int getBID() {
			return this.BID;
		}
	
		int getBID2() {
			return this.BID2;
		}
	
	
		Object getVal() {
			return this.val;
		}
	
		String getSenderID() {
			return this.senderID;
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



	private class Acceptor extends Thread
	{	
		int promisedBID = 0;
		int acceptedBID = 0;
		Object proposedValue;
		Object acceptedValue;


		@Override
		public void run() {
			while (true) {

				GCMessage gcmsg = null;

				// Check if there is a message
				try {

					gcmsg = gcl.readGCMessage();

				} catch (InterruptedException e) { // Catch an InterruptedException per the documentation in the instructions
					e.printStackTrace();
					continue;
				}


				// Extract the PaxosMessage from the message received, and respond. 
				System.out.println("Received message: "+ gcmsg.val);
				PaxosMessage pxmsg = (PaxosMessage) gcmsg.val;

				int receivedBID = pxmsg.getBID();
				int receivedAcceptBID = pxmsg.getBID2();
				Object msgVal = pxmsg.getVal();


				switch (pxmsg.getType()) {
					case PROPOSE:
						// Check for failures as soon as we receive a PROPOSE message
						failCheck.checkFailure(FailCheck.FailureType.RECEIVEPROPOSE);

						// Ensure that the proposed ballotID is the highest value
						// Sends out a PROMISE Paxos message to the proposer thread
						if (receivedBID >= maxBID) {
							this.promisedBID = receivedBID;
							maxBID = receivedBID;
							PaxosMessage newmsg = new PaxosMessage(MsgType.PROMISE, this.promisedBID,this.acceptedBID, this.acceptedValue, String.valueOf(paxosID));
						   	gcl.sendMsg(newmsg, gcmsg.senderProcess);

						   	// Check for failures after sending the PROMISE message for proposer election
						   	failCheck.checkFailure(FailCheck.FailureType.AFTERSENDVOTE);
						} 
						
						// Refuses the proposal if the ballotID isn't the highest
						// Sends out a REFUSE Paxos message to the proposer thread, the refuse contains the maxBallotID for that process
						else {
							PaxosMessage newmsg = new PaxosMessage(MsgType.REFUSE, this.promisedBID, this.acceptedBID, this.acceptedValue, String.valueOf(paxosID));
							gcl.sendMsg(newmsg,gcmsg.senderProcess);

							// Check for failures after sending the PROMISE message for leader election
							failCheck.checkFailure(FailCheck.FailureType.AFTERSENDVOTE);
						}
						break;


					case PROMISE:

						// Important to note that once we reach majority votes, we need to ignore the rest of the promises from the threads that have yet to be delivered to avoid sending out multiple ACCEPT messages
						if (promises > majority) break;


						promises += 1;

						// If we find at least one promise(ballotID, bid2, v'), we need to keep track of it so that we can send accept?(ballotID, v')
						if (receivedAcceptBID > prevID) {
							prevID = receivedAcceptBID;

							// If we have a value, we need to keep track of it so that we can send accept?(ballotID, v'), if its null we ignore
							if (msgVal != null) this.proposedValue = msgVal;
						}


						// Check whether we have majority of promises, if so, send out an ACCEPT? message
						if (promises >= majority) {

							// If we have a proposed value, we send that value, otherwise we send the value we have stored
							if (this.proposedValue != null) {

								PaxosMessage newmsg =new PaxosMessage(MsgType.ACCEPT, this.promisedBID, receivedAcceptBID, this.proposedValue, String.valueOf(paxosID));
								gcl.broadcastMsg(newmsg);

							} 
							  
							 
							else {

								PaxosMessage newmsg = new PaxosMessage(MsgType.ACCEPT, this.promisedBID, receivedAcceptBID, newMove, String.valueOf(paxosID));
								gcl.broadcastMsg(newmsg);
							}
						}
						break;



					case REFUSE: 

						// If the majority refuse, we need to try again next round with a new ballotID
						refuses += 1;

						if (refuses > majority) {

							// reset values
							refuses = 0;
							acceptacks = 0;
							promises = 0;
							denies = 0;

							// Send out a new PROPOSE message with a new ballotID
							PaxosMessage newmsg = new PaxosMessage(MsgType.PROPOSE, generateBID(), 0, null, String.valueOf(paxosID));
							gcl.broadcastMsg(newmsg);
							
							// Check for failures after sending the new PROPOSE message
							failCheck.checkFailure(FailCheck.FailureType.AFTERSENDPROPOSE);
						}

						break;



					case ACCEPT:
						
						// If the ballotID is the same as the promised ballotID, we accept the value
						if (receivedBID == promisedBID) {

							// Since we accept, we update the accepted value and accepted ballotID
							this.promisedBID = receivedBID;
							this.acceptedValue = msgVal;
							this.acceptedBID = receivedAcceptBID;
							PaxosMessage newmsg = new PaxosMessage(MsgType.ACCEPTACK, this.acceptedBID, this.acceptedBID, this.acceptedValue, String.valueOf(paxosID));
							gcl.sendMsg(newmsg, gcmsg.senderProcess);
						} 
						
						// If the ballotID is not the same as the promised ballotID, we deny the value
						else {

							PaxosMessage newmsg = new PaxosMessage(MsgType.DENY, receivedBID, this.acceptedBID, this.acceptedValue, String.valueOf(paxosID));
							gcl.sendMsg(newmsg, gcmsg.senderProcess);
						}

						break;


					case ACCEPTACK:

						acceptacks += 1;

						// If we have majority of acceptacks, we can confirm the value
						if (acceptacks >= majority) {

							// Check for failures after receiving an ACCEPTACK messages and becoming leader
							failCheck.checkFailure(FailCheck.FailureType.AFTERBECOMINGLEADER);

							PaxosMessage newmsg = new PaxosMessage(MsgType.CONFIRM, receivedBID, acceptedBID, acceptedValue, String.valueOf(paxosID));
							gcl.broadcastMsg(newmsg);

						}
						break;
					case DENY:

						denies += 1;

						// If we have majority denies, we propose again next round
						if (denies >= majority) {

							// Reset values
							refuses = 0;
							acceptacks = 0;
							promises = 0;
							denies = 0;

							PaxosMessage newmsg = new PaxosMessage(MsgType.PROPOSE, generateBID(), 0, null, String.valueOf(paxosID));
							gcl.broadcastMsg(newmsg);
							
							// Check for failures after sending the new PROPOSE message
							failCheck.checkFailure(FailCheck.FailureType.AFTERSENDPROPOSE);

						}
						break;


					case CONFIRM:

						//Put in incoming queue
						try {

							incoming.put(pxmsg.getVal());

						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
							e.printStackTrace();
						}

						// Check for failures after receiving CONFIRM message and majority accepted the proposed value
						failCheck.checkFailure(FailCheck.FailureType.AFTERVALUEACCEPT);
						break;
						
				}
			}
		}
	}
}
