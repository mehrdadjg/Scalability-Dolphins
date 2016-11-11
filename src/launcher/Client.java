package launcher;

import util.DocumentUpdate;
import util.DocumentUpdate.PositionType;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Random;
import java.util.Scanner;

import network.ClientReceiver;
import network.ClientSender;

/**
 * Launcher for a client application.
 */
public class Client{
    private static ArrayList<DocumentUpdate> approvedUpdates = new ArrayList<>();
    private static ArrayList<DocumentUpdate> unapprovedUpdates = new ArrayList<>();
    
    private static Socket 		socket			= null;
    
    private static String 		host			= "127.0.0.1";
    private static int    		port 			= 2227;			// TODO Link this to Server#port
    
    private static String		message 		= "";
    
    private static int			TN				= 0;
    
    private static Thread		receiverThread	= null;
    private static Thread		senderThread	= null;
    
    public static final boolean debugging		= false;
    
    public static final String	id				= Client.getSelfMAC() + new Random().nextInt();
    
    public static void main(String[] args){
    	System.out.println("The default proxy server address is " + host + ":" + String.valueOf(port));
    	System.out.println("If this is incorrect give the actual address using the same format," + 
    			" otherwise type anything.");
    	@SuppressWarnings("resource")
		Scanner scanner = new Scanner(System.in);
    	String line = scanner.nextLine();
    	try {
    		String[] segments = line.split(":");
    		if(segments.length == 2) {
    			if(isInteger(segments[1])) {
    				host = segments[0];
    				port = Integer.parseInt(segments[1]);
    				
    			}
    		}
    	} catch(Exception e) {
    		System.err.println("ERROR IN CLIENT. The input could not be processed.");
			e.printStackTrace();
    	}
    	
    	System.out.println("Connecting to " + host + ":" + String.valueOf(port) + "...");
    	
    	try {
    		socket = new Socket(host, port);
    		System.out.println("Connected.");
    	} catch (IOException e) {
    		System.err.println("ERROR IN CLIENT. Cannot open a socket to the proxy server.");
    		if(debugging) {
    			e.printStackTrace();
    		} else {
    			return;
    		}
			
    	}
    	
    	ClientReceiver	receiver	= new ClientReceiver(socket, approvedUpdates, unapprovedUpdates);
    	ClientSender	sender		= new ClientSender(socket);
    	
    	receiverThread	= new Thread(receiver);
    	senderThread	= new Thread(sender);
    	
    	receiverThread.start();
    	senderThread.start();
    }
    
    public static void performIncomingUpdate(DocumentUpdate incomingUpdate) {
    	if(!incomingUpdate.getID().matches(Client.id)) {
    		TN++;
    		performOutgoingUpdate(incomingUpdate);
    	} else {
    		Client.removeUnapprovedUpdate(incomingUpdate);
    	}
    }
    
    public static void performOutgoingUpdate(DocumentUpdate outgoingUpdate) {
    	if(Client.debugging) {
    		System.out.print("updating: " + outgoingUpdate);
    	}
    	int intendedPosition	= outgoingUpdate.getPosition(PositionType.Intended);
    	int actualPosition		= outgoingUpdate.getPosition(PositionType.Actual);
    	
    	int position = (actualPosition < 0) ? intendedPosition : actualPosition;
    	if(Client.debugging) {
    		System.out.println("position: " + position);
    	}
    	
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
    					Client.message.substring(position);
    		}
    	}
    	System.out.println("Current Message: " + Client.message);
    }
    
    public static String getMessage() {
    	return Client.message;
    }
    
    /**
     * Gets the physical address of this client.
     * @return Returns the Base64 representation of the MAC address, or null
	 * if an exception occurs.
     */
    public static String getSelfMAC() {
    	InetAddress ip;
    	try {
    		ip = InetAddress.getLocalHost();
    		NetworkInterface network = NetworkInterface.getByInetAddress(ip);
    		return Base64.getEncoder().encodeToString(network.getHardwareAddress());
    	} catch(IOException e) {
    		return null;
    	}
	}
    
    private static boolean isInteger(String number) {
    	try{
    		Integer.parseInt(number);
    	} catch(NumberFormatException e) {
    		return false;
    	}
    	return true;
    }
    
    public static int getAndIncreaseTransformationNumber() {
    	return ++Client.TN;
    }
    
    public static void addUnapprovedUpdate(DocumentUpdate update) {
    	unapprovedUpdates.add(update);
    }
    
    public static void removeUnapprovedUpdate(DocumentUpdate update) {
    	unapprovedUpdates.remove(update);
    }
}