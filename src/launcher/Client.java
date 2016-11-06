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
    
    private static final String host	= "127.0.0.1";
    private static final int    port 	= 2227;			// TODO Link this to Server#port
    
    private static String		message = "";
    
    private static int			TN		= 0;
    
    public static void main(String[] args){
    	try {
    		socket = new Socket(host, port);
    	} catch (IOException e) {
    		System.err.println("ERROR IN CLIENT. Cannot open a socket to the proxy server.");
			e.printStackTrace();
    	}
    	
    	ClientReceiver	receiver	= new ClientReceiver(socket, approvedUpdates);
    	ClientSender	sender		= new ClientSender(socket, TN);
    	
    	receiver.run();
    	sender.run();
    }
    
    public static void performIncomingUpdate(DocumentUpdate incomingUpdate) {
    	TN++;
    	performOutgoingUpdate(incomingUpdate);
    }
    
    public static void performOutgoingUpdate(DocumentUpdate outgoingUpdate) {
    	int intendedPosition;
    	if((intendedPosition = outgoingUpdate.getPosition(PositionType.Intended)) == Client.message.length()) {
    		if(outgoingUpdate.getChar() == DocumentUpdate.BACKSPACE) {
    			if(Client.message.length() > 0) {
    				Client.message = Client.message.substring(0, Client.message.length() - 1);
    			}
    		} else {
    			Client.message = Client.message + outgoingUpdate.getChar();
    		}
    	} else if(intendedPosition == 0) {
    		if(outgoingUpdate.getChar() != DocumentUpdate.BACKSPACE) {
    			Client.message = outgoingUpdate.getChar() + Client.message;
    		}
    	} else {	// The update is happening at an index in the middle
    		if(outgoingUpdate.getChar() == DocumentUpdate.BACKSPACE) {
    			Client.message = Client.message.substring(0, intendedPosition) +
    					Client.message.substring(intendedPosition + 1);
    		} else {
    			Client.message = Client.message.substring(0, intendedPosition) + outgoingUpdate.getChar() +
    					Client.message.substring(intendedPosition + 1);
    		}
    	}
    }
    
    public static String getMessage() {
    	return Client.message;
    }
}