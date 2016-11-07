package launcher;

import util.DocumentUpdate;
import util.DocumentUpdate.PositionType;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

import network.ClientReceiver;
import network.ClientSender;

/**
 * Launcher for a client application.
 */
public class Client{
    private static ArrayList<DocumentUpdate> approvedUpdates = new ArrayList<>();
    private static ArrayList<DocumentUpdate> unapprovedUpdates = new ArrayList<>();
    
    private static Socket socket;
    
    private static final String host			= "10.13.85.118";
    private static final int    port 			= 2227;			// TODO Link this to Server#port
    
    private static String		message 		= "";
    
    private static int			TN				= 0;
    
    private static Thread		receiverThread	= null;
    private static Thread		senderThread	= null;
    
    public static void main(String[] args){
    	try {
    		socket = new Socket(host, port);
    	} catch (IOException e) {
    		System.err.println("ERROR IN CLIENT. Cannot open a socket to the proxy server.");
			e.printStackTrace();
    	}
    	
    	ClientReceiver	receiver	= new ClientReceiver(socket, approvedUpdates, unapprovedUpdates);
    	ClientSender	sender		= new ClientSender(socket);
    	
    	receiverThread	= new Thread(receiver);
    	senderThread	= new Thread(sender);
    	
    	receiverThread.start();
    	senderThread.start();
    }
    
    public static void performIncomingUpdate(DocumentUpdate incomingUpdate) {
    	if(!incomingUpdate.getMAC().matches(DocumentUpdate.getSelfMAC())) {
    		TN++;
    		performOutgoingUpdate(incomingUpdate);
    	} else {
    		Client.removeUnapprovedUpdate(incomingUpdate);
    	}
    }
    
    public static void performOutgoingUpdate(DocumentUpdate outgoingUpdate) {
    	System.out.println("updating: " + outgoingUpdate);
    	int intendedPosition	= outgoingUpdate.getPosition(PositionType.Intended);
    	int actualPosition		= outgoingUpdate.getPosition(PositionType.Actual);
    	
    	int position = (actualPosition < 0) ? intendedPosition : actualPosition;
    	
    	if(position == Client.message.length()) {
    		if(outgoingUpdate.getChar() == DocumentUpdate.BACKSPACE) {
    			if(Client.message.length() > 0) {
    				Client.message = Client.message.substring(0, Client.message.length() - 1);
    			}
    		} else {
    			Client.message = Client.message + outgoingUpdate.getChar();
    		}
    	} else if(position == 0) {
    		if(outgoingUpdate.getChar() != DocumentUpdate.BACKSPACE) {
    			Client.message = outgoingUpdate.getChar() + Client.message;
    		}
    	} else {	// The update is happening at an index in the middle
    		if(outgoingUpdate.getChar() == DocumentUpdate.BACKSPACE) {
    			Client.message = Client.message.substring(0, position) +
    					Client.message.substring(position + 1);
    		} else {
    			Client.message = Client.message.substring(0, position) + outgoingUpdate.getChar() +
    					Client.message.substring(position + 1);
    		}
    	}
    	System.out.println("Current Message: " + Client.message);
    }
    
    public static String getMessage() {
    	return Client.message;
    }
    
    public static int getAndIncreaseTransformationNumber() {
    	return ++Client.TN;
    }
    
    public static void addUnapprovedUpdate(DocumentUpdate update) {
    	unapprovedUpdates.add(update);
    }
    
    public static void removeUnapprovedUpdate(DocumentUpdate update) {
    	unapprovedUpdates.remove(update); // FIXME
    }
}