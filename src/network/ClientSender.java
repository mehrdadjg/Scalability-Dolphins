package network;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

import launcher.Client;
import util.DocumentUpdate;

public class ClientSender implements Runnable {
	
	private Socket						socket;
    private DataOutputStream			dataOutputStream;
    
    private boolean						isRunning;
    
	public ClientSender(Socket socket) {
		this.socket = socket;
		
		try {
			dataOutputStream = new DataOutputStream(this.socket.getOutputStream());
		} catch(IOException e) {
			System.err.println("ERROR IN CLIENT. Cannot establish an outgoing stream.");
			e.printStackTrace();
		}
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
			
			try {
				dataOutputStream.writeUTF(outgoingUpdateString);
			} catch (IOException e) {
				System.err.println("ERROR IN CLIENT. Cannot write to the outgoing stream.");
				e.printStackTrace();
			}
		}
		
		scanner.close();
	}
	
}
