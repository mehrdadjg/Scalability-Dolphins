package network;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Scanner;

import launcher.Client;
import util.DocumentUpdate;

public class ClientSender implements Runnable {
	
	private DataOutputStream			dataOutputStream;
    
    private boolean						isRunning;
    
    private EditorStatus				status		= EditorStatus.CommandLine;
    
    private boolean						firstRun	= true;
    
    private enum EditorStatus {
    	CommandLine,
    	Editing,
    	Error,
    	AskingToEndEditing,
    }
    
	public ClientSender(DataOutputStream dataOutputStream) {
		this.dataOutputStream = dataOutputStream;
	}

	@Override
	public void run() {
		isRunning = true;
		Scanner scanner = new Scanner(System.in);
		String line		= null;
		
		while(isRunning) {
			switch(status) {
			case CommandLine:
				if(firstRun) {
					System.out.println("The follwing is a list of commands that you can use:");
					System.out.println("list    : Returns a list of all the editable documents for you to choose from.");
					System.out.println("create x: Prompts to create a document with name x and opens it for edit.");
					System.out.println("open x  : Opens the document with name x for edit, if it exists. None of these commands work when you are editing a document. You can close a file by pressing enter twice in a row.");
					System.out.println("help    : shows this message again.");
					
					firstRun = false;
				} else {
					// Nothing goes here
				}
				
				System.out.print(">>> ");
				line = scanner.nextLine();
				
				if(line.toLowerCase().startsWith("list")) {
					try {
						dataOutputStream.writeUTF("list");
						dataOutputStream.flush();
						
						waitForAnswer();
					} catch (IOException e) {
						System.out.println("ERROR IN CLIENT. Cannot write to the outgoing stream.");
						if(Client.debugging) {
							e.printStackTrace();
						} else {
							scanner.close();
							return;
						}
					}
					
					if(responseMsg.compareTo("list_received") == 0) {
						String[] list = (String[]) response;
						
						// TODO
					} else {
						status = EditorStatus.Error;
						System.out.println("The request was responded with an error.");
					}
					this.response		= null;
					this.responseMsg	= null;
				} else if(line.toLowerCase().startsWith("create ")) {
					
				} else if(line.toLowerCase().startsWith("open ")) {
					
				} else if(line.toLowerCase().startsWith("help")) {
					
				} else {
					System.out.println("Unrecognizable command.");
				}
				
				break;
			case Editing:
				line = scanner.nextLine();
				
				if (line == null || line.compareTo("") == 0) {
					status = EditorStatus.AskingToEndEditing;
					break;
				}
					
				DocumentUpdate outgoingUpdate = 
						new DocumentUpdate(line, Client.getMessage().length(), Client.getAndIncreaseTransformationNumber());
					
				Client.performOutgoingUpdate(outgoingUpdate);
					
				String outgoingUpdateString = outgoingUpdate.toString();
				System.out.println("outgoing: " + outgoingUpdateString);
					
				Client.addUnapprovedUpdate(outgoingUpdate);
					
				try {
					dataOutputStream.writeUTF(outgoingUpdateString);
					dataOutputStream.flush();
				} catch (IOException e) {
					System.out.println("ERROR IN CLIENT. Cannot write to the outgoing stream.");
					if(Client.debugging) {
						e.printStackTrace();
					} else {
						scanner.close();
						return;
					}
				}
				break;
				
			case Error:
				
				break;
				
			case AskingToEndEditing:
				System.out.println("Press enter another time to quit editing. Press any other key to keep editing.");
				
				line = scanner.nextLine();
				
				if(line.compareTo("") == 0) {
					status = EditorStatus.CommandLine;
					System.out.println("Editing ended.");
				} else {
					status = EditorStatus.Editing;
					System.out.println("Keep editing.");
				}
				break;
				
			default:
					break;
					
			}
			
		}
		
		scanner.close();
	}
	
	private String responseMsg	= null;
	private Object response		= null;
	private boolean waiting		= true;
	
	private void waitForAnswer() {
		waiting = true;
		
		while(waiting) {}
	}
	
	public void respondWith(String responseMsg, Object response) {
		this.responseMsg = responseMsg;
		this.response = response;
		waiting = false;
	}
	
	

	public void setOutputSender(DataOutputStream dataOutputStream) {
		this.dataOutputStream = dataOutputStream;
	}
	
}
