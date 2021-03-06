package launcher;

import network.ReplicaMain;
import util.Logger;
import util.Resources;

import java.io.IOException;
import java.util.Scanner;

/**
 * Launcher for a backend replica server. This is the entry point for the backend server portion
 * This should remain as simple as possible, and only initialize the startup sequence for the server
 * https://docs.oracle.com/javase/tutorial/networking/datagrams/broadcasting.html
 */
public class Replica {
    private static String ip = "127.0.0.1";
    private static int port = Resources.REPLICAPORT;
	private static int recoveryPort = Resources.RECOVERYPORT;

    public static void main(String[] args){
		Logger.initialize(Logger.ProcessType.Replica);
		if (args.length > 0){
            ip = args[0];
			if (args.length > 1){
				port = Integer.parseInt(args[1]);
			}
			if ((args.length) > 2) {
				recoveryPort = Integer.parseInt(args[2]);
			}
        }

        ReplicaMain replicaMain;
		try {
			replicaMain = new ReplicaMain(ip, port, recoveryPort);
	        new Thread(replicaMain).start();

	        //Await Quit command to shutdown
	        Scanner scanner = new Scanner(System.in);
	        String userInput;
	        do {
	            userInput = scanner.nextLine();
	        } while (userInput.compareTo("Quit") != 0);

	        replicaMain.shutdown();
	        scanner.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

    }
}