package network;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;

import launcher.Client;
import transformations.OperationalTransformation;
import util.DocumentUpdate;
import util.Logger;
import util.Logger.LogType;
import util.Resources;

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
					System.out.println("input: " + input + ".");
				}
				
				if(input.startsWith("bundle")) {
					if(input.split(" ")[1].compareTo("null") != 0)
						Client.performIncomingUpdates(input);
					continue;
				} else if(input.startsWith("documents")) {
					manageListOfDocuments(input);
					continue;
				} else if(input.startsWith("done")) {
					sendDoneToSender();
					continue;
				} else if (input.startsWith("error")){
					Logger.log("proxy refused previous request with message: " + input);
					sendErrorToSender();
					continue;
				}
			} catch(IOException e) {
				Logger.log("ERROR IN CLIENT. Cannot read from the incoming stream.", LogType.Error);
				Client.respondToSender("error", null);
				reconnect();
				continue;
			}
			
			DocumentUpdate incomingUpdate = DocumentUpdate.fromString(input);
			
			if(incomingUpdate == null) {
				Client.respondToSender("error", null);
				Logger.log("Incorrect protocol detected in the incoming stream. Packet dropped.", LogType.Error);
				Logger.log("Incorrect input: " + input, LogType.Info);
//				Client.informProxy("error");
				continue;
			}
			
			OperationalTransformation.update(approvedUpdates, unapprovedUpdates, incomingUpdate);
			
			Client.performIncomingUpdate(incomingUpdate);
		}
	}

	private void sendDoneToSender() {
		Client.respondToSender("done", null);
	}

	private void sendErrorToSender() {
		Client.respondToSender("error", null);
	}

	private void manageListOfDocuments(String input) {
		if(input.indexOf("]") - input.indexOf("[") == 1) {
			Client.respondToSender("list_received", null);
		} else {
			String[] names = input.substring(11, input.length() - 1).trim().split(",");
			
			if(names.length == 0 || (names.length == 1 && names[0].trim().compareTo("") == 0)) {
				Client.respondToSender("list_received", null);
			} else {
				Client.respondToSender("list_received", names);
			}
		}
	}

	private void reconnect() {
		while(true) {
			if(Client.reconnect()) {
				break;
			} else {
				try {
					Thread.sleep(Resources.RECONNECTRETRYINTERVAL);
				} catch (InterruptedException e) { }
			}
		}
	}

	public void setInputStream(DataInputStream dataInputStream) {
		this.dataInputStream = dataInputStream;
	}
	
}
