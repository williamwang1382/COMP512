package comp512st.paxos;

// Access to the GCL layer
import comp512.gcl.*;

import comp512.utils.*;

// Any other imports that you may need.
import java.io.*;
import java.util.logging.*;
import java.net.UnknownHostException;

// ANY OTHER classes, etc., that you add must be private to this package and not visible to the application layer.

// extend / implement whatever interface, etc. as required.
// NO OTHER public members / methods allowed. broadcastTOMsg, acceptTOMsg, and shutdownPaxos must be the only visible methods to the application layer.
//	You should also not change the signature of these methods (arguments and return value) other aspects maybe changed with reasonable design needs.
public class Paxos
{
	GCL gcl;
	FailCheck failCheck;

	public Paxos(String myProcess, String[] allGroupProcesses, Logger logger, FailCheck failCheck) throws IOException, UnknownHostException
	{
		// Rember to call the failCheck.checkFailure(..) with appropriate arguments throughout your Paxos code to force fail points if necessary.
		this.failCheck = failCheck;

		// Initialize the GCL communication system as well as anything else you need to.
		this.gcl = new GCL(myProcess, allGroupProcesses, null, logger) ;

	}

	// This is what the application layer is going to call to send a message/value, such as the player and the move
	public void broadcastTOMsg(Object val)
	{
		// This is just a place holder.
		// Extend this to build whatever Paxos logic you need to make sure the messaging system is total order.
		// Here you will have to ensure that the CALL BLOCKS, and is returned ONLY when a majority (and immediately upon majority) of processes have accepted the value.
		gcl.broadcastMsg(val);
		
		
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
		 * Getter and Setter Methods:
		 * getType(), getBID(),getVal(), getSenderID(),
		 * setType(MsgType type), setBID(int BID), setVal(GCMessage val), setSenderID(String senderID)
		 * Testing Aid: toString()
		 */
		public MsgType getType() {
			return type;
		}
	
		public void setType(MsgType type) {
			this.type = type;
		}
	
		public int getBID() {
			return BID;
		}
	
		public void setBID(int BID) {
			this.BID = BID;
		}
	
		public GCMessage getVal() {
			return val;
		}
	
		public void setVal(GCMessage val) {
			this.val = val;
		}
	
		public String getSenderID() {
			return senderID;
		}
	
		public void setSenderID(String senderID) {
			this.senderID = senderID;
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

	


}

