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
			String line = scanner.nextLine();
			
			for(int k = 0; k < line.length(); k++) {
				char c = line.charAt(k);
				
				DocumentUpdate outgoingUpdate = 
						new DocumentUpdate(c, Client.getMessage().length(), Client.getAndIncreaseTransformationNumber());
				
				Client.performOutgoingUpdate(outgoingUpdate);
				
				String outgoingUpdateString = outgoingUpdate.toString();
				System.out.println("outgoing: " + outgoingUpdateString);
				
				Client.addUnapprovedUpdate(outgoingUpdate);
				
				try {
					dataOutputStream.writeUTF(outgoingUpdateString);
					dataOutputStream.flush();
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
				
		}
		
		scanner.close();
	}
	
}
