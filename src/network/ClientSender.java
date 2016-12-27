package network;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Random;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import IO.ClientView;
import launcher.Client;
import util.DocumentUpdate;

public class ClientSender implements Runnable {
	
	private DataOutputStream			dataOutputStream;
    
    private volatile boolean			isRunning;
    
    private EditorStatus				status		= EditorStatus.CommandLine;
    
    private boolean						firstRun	= true;

	private Pattern pattern = Pattern.compile("(\\S+) (\\d+)");
    
    private enum EditorStatus {
    	CommandLine,
    	Editing,
    	Error,
    	AskingToEndEditing,
		Fuzz,
    }
    
	public ClientSender(DataOutputStream dataOutputStream) {
		this.dataOutputStream = dataOutputStream;
	}

	public void open(String doc_name){
		try {
			dataOutputStream.writeUTF("open " + doc_name);
			dataOutputStream.flush();

			waitForAnswer();

			if(this.responseMsg.compareTo("done") == 0) {
				Client.current_doc = doc_name;
				status = EditorStatus.Editing;
				try {
					Client.initialize(false);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				System.out.println("... Could not create the document.");
			}
			this.response		= null;
			this.responseMsg	= null;

		} catch (IOException e) {
			System.out.println("ERROR IN CLIENT. Cannot write to the outgoing stream.");
			if(Client.debugging) {
				e.printStackTrace();
			} else {
				return;
			}
		}

	}

	public void getList(){

		try {
			dataOutputStream.writeUTF("list");
			dataOutputStream.flush();

			waitForAnswer();

			if(responseMsg.compareTo("list_received") == 0) {
				String[] list = new String[0];
				if (response != null){
					list = (String[]) response;
				}
				Client.clientView.setList(list);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		this.response		= null;
		this.responseMsg	= null;
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
					System.out.println("list  : Returns a list of all the editable documents for you to choose from.");
					System.out.println("open x: Opens a document for editing. If it doesn't exist it will create the document.");
					System.out.println("fuzz x: experiments");
					System.out.println("help  : shows this message again.");
					
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
						if(Client.debugging)
							System.out.println("ERROR IN CLIENT. Cannot write to the outgoing stream.");
						if(Client.debugging) {
							e.printStackTrace();
						} else {
							scanner.close();
							return;
						}
					}
					
					if(responseMsg.compareTo("list_received") == 0) {
						String[] list = new String[0];
						if (response != null){
							list = (String[]) response;
						}
						
						if(list.length > 0) {
							System.out.println("... The documents are as listed below:");
							
							int index = 1;
							for(int i = 0; i < list.length; i++) {
								if(list[i].trim().compareTo("") != 0)
									System.out.println("... " + (index++) + ". " + list[i]);
							}
						} else {
							System.out.println("There are currently no documents. create one with open <name>");
						}

						Client.clientView.setList(list);
					} else {
						status = EditorStatus.CommandLine;
						System.out.println("... The request was responded with an error.");
					}
					this.response		= null;
					this.responseMsg	= null;
				} else if(line.toLowerCase().startsWith("open ")) {
					String doc_name = line.substring(5);
					if(doc_name.trim().compareTo("") == 0) {
						System.out.println("... Unacceptable document name.");
						break;
					} else {
						if(doc_name.contains("<") || doc_name.contains(">") || doc_name.contains(":") || doc_name.contains("\"") ||
						   doc_name.contains("/") || doc_name.contains("\\") || doc_name.contains("|") || doc_name.contains("?") ||
						   doc_name.contains("*")) {
							System.out.println("... Unacceptable document name.");
							break;
						} else if(doc_name.compareTo("null") == 0) {
							System.out.println("... Cannot use a reserved name.");
							break;
						}
					}
					
					try {
						dataOutputStream.writeUTF("open " + doc_name);
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
					
					if(this.responseMsg.compareTo("done") == 0) {
						System.out.println("... " + doc_name + " was created and is open to be written on.");
						Client.current_doc = doc_name;
						status = EditorStatus.Editing;
						try {
							Client.initialize(false);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					} else if (this.responseMsg.compareTo("error") == 0){
						continue;
					}
					else {
						System.out.println("... Could not create the document.");
					}
					this.response		= null;
					this.responseMsg	= null;
				} else if (line.toLowerCase().startsWith("fuzz")){

					String doc_name = line.substring(5);
					if(doc_name.trim().compareTo("") == 0) {
						System.out.println("... Unacceptable document name.");
						break;
					} else {
						if(doc_name.contains("<") || doc_name.contains(">") || doc_name.contains(":") || doc_name.contains("\"") ||
								doc_name.contains("/") || doc_name.contains("\\") || doc_name.contains("|") || doc_name.contains("?") ||
								doc_name.contains("*")) {
							System.out.println("... Unacceptable document name.");
							break;
						} else if(doc_name.compareTo("null") == 0) {
							System.out.println("... Cannot use a reserved name.");
							break;
						}
					}

					try {
						dataOutputStream.writeUTF("open " + doc_name);
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

					if(this.responseMsg.compareTo("done") == 0) {
						System.out.println("... " + doc_name + " was created and is open to be written on.");
						Client.current_doc = doc_name;
						status = EditorStatus.Fuzz;
						try {
							Client.initialize(false);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					} else if (this.responseMsg.compareTo("error") == 0){
						continue;
					}
					else {
						System.out.println("... Could not create the document.");
					}
					this.response		= null;
					this.responseMsg	= null;
				}
				else if(line.toLowerCase().startsWith("help")) {
					firstRun = true;
				} else {
					System.out.println("... Unrecognizable command.");
				}
				
				break;
			case Editing:
				line = scanner.nextLine();

				if (line == null || line.compareTo("") == 0) {
					status = EditorStatus.AskingToEndEditing;
					break;
				}

				DocumentUpdate outgoingUpdate;
				Matcher matcher = pattern.matcher(line);
				if (matcher.matches()){
					System.out.println("detected arbitrary input command!");

					String message = matcher.group(1);
					int position = Integer.parseInt(matcher.group(2));

					outgoingUpdate =
							new DocumentUpdate(message, position, Client.getAndIncreaseTransformationNumber());
				} else {
					outgoingUpdate =
							new DocumentUpdate(line, Client.getMessage().length(), Client.getAndIncreaseTransformationNumber());
				}

				Client.performOutgoingUpdate(outgoingUpdate);
					
				String outgoingUpdateString = outgoingUpdate.toString();
				if(Client.debugging)
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
					Client.close();
					status = EditorStatus.CommandLine;
					System.out.println("Editing ended.");
				} else {
					status = EditorStatus.Editing;
					System.out.println("Keep editing.");
				}
				break;

			case Fuzz:
				//System.out.println("butts");
				try {
					while (System.in.available()==0){
						char nextChar = (char) (new Random().nextInt(255));
						

						outgoingUpdate =
								new DocumentUpdate(nextChar, Client.getMessage().length(), Client.getAndIncreaseTransformationNumber());
						Client.performOutgoingUpdate(outgoingUpdate);

						outgoingUpdateString = outgoingUpdate.toString();
						if(Client.debugging)
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
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				Client.close();
				status = EditorStatus.CommandLine;
				System.out.println("Fuzzing ended.");

				break;
			default:
					break;
					
			}
			
		}
		
		scanner.close();
	}
	
	private String responseMsg	= null;
	private Object response		= null;
	private volatile boolean waiting		= true;
	
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
