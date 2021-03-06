package launcher;

import util.DocumentUpdate;
import util.DocumentUpdate.PositionType;
import util.Logger.LogType;
import util.Logger.ProcessType;
import util.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Random;
import java.util.Scanner;
import java.util.regex.Pattern;

import network.ClientReceiver;
import network.ClientSender;
import util.Resources;

/**
 * Launcher for a client application.
 */
public class Client{
    private static ArrayList<DocumentUpdate> approvedUpdates = new ArrayList<>();
    private static ArrayList<DocumentUpdate> unapprovedUpdates = new ArrayList<>();
    
    private static Socket 			socket				= null;
    
    private static DataInputStream	dataInputStream		= null;
    private static DataOutputStream	dataOutputStream	= null;
    
    private static String 			host				= "127.0.0.1";
    private static int    			port 				= Resources.CLIENTPORT;
    
    private static String			message 			= "";
    
    private static int				TN					= 0;
    
    private static Thread			receiverThread		= null;
    private static Thread			senderThread		= null;
    
    public	static final boolean	debugging			= Resources.DEBUG;
    
    public	static String			id					= Client.getSelfMAC() + new Random().nextInt();
    public	static String			current_doc			= "null";
    
    private static ClientReceiver	receiver			= null;
    private static ClientSender		sender				= null;
    
    public static void main(String[] args){
    	Logger.initialize(ProcessType.Client);
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
    		System.out.println("ERROR IN CLIENT. The input could not be processed.");
			e.printStackTrace();
    	}
    	
    	System.out.println("Connecting to " + host + ":" + String.valueOf(port) + "...");
    	
    	try {
    		socket = new Socket(host, port);
    		System.out.println("Connected.");
    	} catch (IOException e) {
    		System.out.println("ERROR IN CLIENT. Cannot open a socket to the proxy server.");
    		if(debugging) {
    			e.printStackTrace();
    		} else {
    			return;
    		}
			
    	}
    	
    	try {
    		dataInputStream = new DataInputStream(socket.getInputStream());
    		dataOutputStream = new DataOutputStream(socket.getOutputStream());
    		
    		initialize(true);
    	} catch(IOException e) {
    		System.out.println("ERROR IN CLIENT. Cannot establish the required connections to the proxy.");
    		if(debugging) {
    			e.printStackTrace();
    		} else {
    			return;
    		}
    	}
    	
    	receiver	= new ClientReceiver(dataInputStream, approvedUpdates, unapprovedUpdates);
    	sender		= new ClientSender(dataOutputStream);
    	
    	receiverThread	= new Thread(receiver);
    	senderThread	= new Thread(sender);
    	
    	receiverThread.start();
    	senderThread.start();
    }
    
    public static void close() {
    	id = Client.getSelfMAC() + new Random().nextInt();
    	TN = 0;
    	approvedUpdates = new ArrayList<>();
    	unapprovedUpdates = new ArrayList<>();
    	current_doc = "null";
    	message = "";
    }
    
    public static boolean reconnect() {
    	try {
    		Logger.log("Attempting to reconnect to the proxy...", LogType.Info);
	    	socket = new Socket(host, port);
			
	    	Logger.log("Initializing streams...", LogType.Info);
			dataInputStream = new DataInputStream(socket.getInputStream());
			dataOutputStream = new DataOutputStream(socket.getOutputStream());
			
			Logger.log("Requesting lost updates...", LogType.Info);
			initialize(true);
			
			Logger.log("Finalizing the process...", LogType.Info);
			receiver.setInputStream(dataInputStream);
			sender.setOutputSender(dataOutputStream);
			Logger.log("Reconnected...", LogType.Info);
			return true;
    	} catch(IOException e) {
    		Logger.log("Reconnect failed.", LogType.Error);
    		return false;
    	}
    }
    
    public static void initialize(boolean readIncoming) throws IOException {
    	dataOutputStream.writeUTF("update " + current_doc + " " + TN);
    	
    	if(readIncoming) {
		    //recieve and format the response
			String reply = dataInputStream.readUTF();
		    reply =  reply.replaceFirst("bundle ", "");
			String[] msgs = Pattern.compile("\\[|,|\\]").split(reply);
			int count = msgs.length;
			
			System.out.println(Arrays.toString(msgs));
			
			for(int i = 0; i < count; i++) {
				String msg = msgs[i];
				if(msg.trim().matches("")) {
					continue;
				}
				DocumentUpdate newUpdate = DocumentUpdate.fromString(msg);
				if(newUpdate != null) {
					if(newUpdate.getDocumentName().compareTo(Client.current_doc) != 0)
						return;
					performIncomingUpdate(newUpdate);
				}
			}
    	}
	}

	public static void performIncomingUpdate(DocumentUpdate incomingUpdate) {
		if(incomingUpdate.getDocumentName().compareTo(Client.current_doc) != 0)
			return;
    	if(incomingUpdate.getID().compareTo(Client.id) != 0) {
    		TN++;
    		performOutgoingUpdate(incomingUpdate);
    	} else {
    		Client.removeUnapprovedUpdate(incomingUpdate);
    	}
    }
	
	public static void performIncomingUpdates(String updates) {
		String[] msgs = Pattern.compile("\\[|,|\\]").split(updates);
		if(Client.debugging)
			System.out.println(Arrays.toString(msgs));
		int count = msgs.length;
		for(int i = 0; i < count; i++) {
			String msg = msgs[i];
			if(msg.trim().matches("")) {
				continue;
			}
			DocumentUpdate newUpdate = DocumentUpdate.fromString(msg);
			if(newUpdate != null) {
				if(newUpdate.getDocumentName().compareTo(Client.current_doc) != 0)
					return;
				performIncomingUpdate(newUpdate);
			}
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
    		if(outgoingUpdate.getString().compareTo(String.valueOf(DocumentUpdate.BACKSPACE)) == 0) {
    			if(Client.message.length() > 0) {
    				Client.message = Client.message.substring(0, Client.message.length() - 1);
    			}
    		} else {
    			Client.message = Client.message + outgoingUpdate.getString();
    		}
    	} else if(position == 0) {
    		if(outgoingUpdate.getString().compareTo(String.valueOf(DocumentUpdate.BACKSPACE)) != 0) {
    			Client.message = outgoingUpdate.getString() + Client.message;
    		}
    	} else {	// The update is happening at an index in the middle
    		if(outgoingUpdate.getString().compareTo(String.valueOf(DocumentUpdate.BACKSPACE)) == 0) {
    			Client.message = Client.message.substring(0, position) +
    					Client.message.substring(position + 1);
    		} else {
    			Client.message = Client.message.substring(0, position) + outgoingUpdate.getString() +
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
    
    /**
     * Sends a string to the proxy. This message is not crucial to the functionality
     * of the process.
     * @param msg The message to be sent to the proxy.
     */
	public static void informProxy(String msg) {
		try {
			dataOutputStream.writeUTF(msg);
			Logger.log("Message sent to proxy: " + msg, LogType.Info);
		} catch (IOException e) {
			Logger.log("Failed sending a message to the proxy.", LogType.Warning);
			Logger.log("The failed message: " + msg, LogType.Info);
		}
	}

	public static void respondToSender(String responseMsg, Object response) {
		sender.respondWith(responseMsg, response);
	}
}