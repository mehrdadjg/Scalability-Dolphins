package network;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

import launcher.Client;
import transformations.OperationalTransformation;
import util.DocumentUpdate;

public class ClientReceiver implements Runnable {
	
	private Socket						socket;
	private ArrayList<DocumentUpdate>	approvedUpdates;
	private ArrayList<DocumentUpdate>	unapprovedUpdates;
	
	private boolean						isRunning;
	
	private DataInputStream				dataInputStream;
	
	public ClientReceiver(Socket socket, ArrayList<DocumentUpdate> approvedUpdates, ArrayList<DocumentUpdate> unapprovedUpdates) {
		this.socket				= socket;
		this.approvedUpdates	= approvedUpdates;
		this.unapprovedUpdates	= unapprovedUpdates;
		
		try {
			dataInputStream = new DataInputStream(this.socket.getInputStream());
		} catch(IOException e) {
			System.err.println("ERROR IN CLIENT. Cannot establish an incoming stream.");
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		isRunning = true;
		
		String input = null;
		while(isRunning) {
			try {
				input = dataInputStream.readUTF();
				System.out.println("input: " + input);
			} catch(IOException e) {
				System.err.println("ERROR IN CLIENT. Cannot read from the incoming stream.");
				e.printStackTrace();
			}
			
			DocumentUpdate incomingUpdate = DocumentUpdate.fromString(input);
			
			OperationalTransformation.update(approvedUpdates, unapprovedUpdates, incomingUpdate);
			
			Client.performIncomingUpdate(incomingUpdate);
		}
	}
	
}
