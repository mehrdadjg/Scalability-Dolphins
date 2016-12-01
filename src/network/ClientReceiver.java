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
					System.out.print("input: " + input);
				}
				if(input.startsWith("bundle")) {
					Client.performIncomingUpdates(input);
					continue;
				}
			} catch(IOException e) {
				Logger.log("ERROR IN CLIENT. Cannot read from the incoming stream.", LogType.Error);
				reconnect();
				continue;
			}
			
			DocumentUpdate incomingUpdate = DocumentUpdate.fromString(input);
			
			if(incomingUpdate == null) {
				Logger.log("Incorrect protocol detected in the incoming stream. Packet dropped.", LogType.Error);
				Logger.log("Incorrect input: " + input, LogType.Info);
//				Client.informProxy("error");
				continue;
			}
			
			OperationalTransformation.update(approvedUpdates, unapprovedUpdates, incomingUpdate);
			
			Client.performIncomingUpdate(incomingUpdate);
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
