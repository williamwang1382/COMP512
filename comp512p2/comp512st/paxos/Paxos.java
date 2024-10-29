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
//		You should also not change the signature of these methods (arguments and return value) other aspects maybe changed with reasonable design needs.
public class Paxos
{
	GCL gcl;
	FailCheck failCheck;

	private int ballotID;
	

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



	// Every player is only the proposer for their own moves, they need to be the acceptor for the moves proposed by the other threads
	private class AcceptorThread extends Thread{
		

		public void run(){

			while (true) {

				GCMessage msg = null;

				// Check if there is a message
				try {
					msg = gcl.readGCMessage();
				} catch (InterruptedException e){ // Catch an InterruptedException per the documentation in the doc
					e.printStackTrace();
				}

				// Extract data from the message received
				MyMessage mymsg = (MyMessage)(msg.val);

				switch (mymsg.mtype){
					case ACCEPT:
					// TODO
					break;

					case ACKNOWLEDGE:
					// TODO
					break;

					case CONFIRM:
					// TODO
					break;

					case DENY:
					// TODO
					break;

					case REFUSE:
					// TODO
					break;

					case PROMISE:
					// TODO
					break;

					case PROPOSE:
					// TODO
					break;
				}



			}
		}
	}


	// Need to create a new class to define the type of message objects the Paxos threads send between each other
	private class MyMessage {
		MsgType mtype;
		Object value;


		MyMessage(MsgType mtype, Object value){
			this.mtype = mtype;
			this.value = value;

		}
		
	}

	// The MsgType will be piggybacked onto the messages so that processes can identify what the message type is when received
	// Message Types are based on the Paxos slides
	// DENY is used when the PROPOSE? ballot id suggested by the proposer is denied by a listener thread
	// REFUSE is used after the ballot id was accepted and the proposer sends an ACCEPT message with a value and a listener thread refuses
	private enum MsgType {ACCEPT, ACKNOWLEDGE, CONFIRM, DENY, PROMISE, PROPOSE, REFUSE}
}

