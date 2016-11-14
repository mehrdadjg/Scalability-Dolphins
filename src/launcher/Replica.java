package launcher;

import network.ReplicaMain;

import java.io.IOException;
import java.util.Scanner;

/**
 * Launcher for a backend replica server. This is the entry point for the backend server portion
 * This should remain as simple as possible, and only initialize the startup sequence for the server
 */
public class Replica {
    private static String ip = "192.168.1.102";
    private static int port = 3729;

    public static void main(String[] args){
        //TODO check valid ip format with regex "\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}"
        if (args.length > 0){
            ip = args[0];
        }

        ReplicaMain replicaMain;
		try {
			replicaMain = new ReplicaMain(ip, port);
            replicaMain.recoveryMode = true;
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