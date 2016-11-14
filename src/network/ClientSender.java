package network;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Scanner;

import launcher.Client;
import util.DocumentUpdate;

public class ClientSender implements Runnable {
	
	private DataOutputStream			dataOutputStream;
    
    private boolean						isRunning;
    
	public ClientSender(DataOutputStream dataOutputStream) {
		this.dataOutputStream = dataOutputStream;
	}

	@Override
	public void run() {
		isRunning = true;
		Scanner scanner = new Scanner(System.in);
		
		while(isRunning) {
			char c = scanner.next(".").charAt(0);
			
			DocumentUpdate outgoingUpdate = 
					new DocumentUpdate(c, Client.getMessage().length(), Client.getAndIncreaseTransformationNumber());
			
			Client.performOutgoingUpdate(outgoingUpdate);
			
			String outgoingUpdateString = outgoingUpdate.toString();
			
			Client.addUnapprovedUpdate(outgoingUpdate);
			
			try {
				dataOutputStream.writeUTF(outgoingUpdateString);
			} catch (IOException e) {
				System.err.println("ERROR IN CLIENT. Cannot write to the outgoing stream.");
				if(Client.debugging) {
					e.printStackTrace();
				} else {
					scanner.close();
					return;
				}
			}
		}
		
		scanner.close();
	}
	
}
