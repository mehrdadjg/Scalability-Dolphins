package network;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;

import launcher.Client;
import transformations.OperationalTransformation;
import util.DocumentUpdate;

public class ClientReceiver implements Runnable {
	
	private ArrayList<DocumentUpdate>	approvedUpdates;
	private ArrayList<DocumentUpdate>	unapprovedUpdates;
	
	private boolean						isRunning;
	
	private DataInputStream				dataInputStream;
	
	public ClientReceiver(DataInputStream dataInputStream, ArrayList<DocumentUpdate> approvedUpdates, ArrayList<DocumentUpdate> unapprovedUpdates) {
		this.dataInputStream	= dataInputStream;
		this.approvedUpdates	= approvedUpdates;
		this.unapprovedUpdates	= unapprovedUpdates;
	}

	@Override
	public void run() {
		isRunning = true;
		
		String input = null;
		while(isRunning) {
			try {
				input = dataInputStream.readUTF();
				if(Client.debugging) {
					System.out.print("input: " + input);
				}
				if(input.startsWith("[")) {
					Client.performIncomingUpdates(input);
					continue;
				}
			} catch(IOException e) {
				System.err.println("ERROR IN CLIENT. Cannot read from the incoming stream.");
				if(Client.debugging) {
					e.printStackTrace();
				} else {
					return;
				}
			}
			
			DocumentUpdate incomingUpdate = DocumentUpdate.fromString(input);
			
			OperationalTransformation.update(approvedUpdates, unapprovedUpdates, incomingUpdate);
			
			Client.performIncomingUpdate(incomingUpdate);
		}
	}
	
}
