package launcher;

import network.ReplicaMain;

import java.util.Scanner;

/**
 * Launcher for a backend replica server. This is the entry point for the backend server portion
 * This should remain as simple as possible, and only initialize the startup sequence for the server
 * https://docs.oracle.com/javase/tutorial/networking/datagrams/broadcasting.html
 */
public class Replica {
    private static String ip = "192.168.1.102";
    private static int port = 3729;

    public static void main(String[] args){
        //TODO check valid ip format with regex "\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}"
        if (args.length > 0){
            ip = args[0];
        }

        ReplicaMain replicaMain = new ReplicaMain(ip, port);
        new Thread(replicaMain).start();

        //Await Quit command to shutdown
        Scanner scanner = new Scanner(System.in);
        String userInput;
        do {
            userInput = scanner.nextLine();
        } while (userInput.compareTo("Quit") != 0);

        replicaMain.shutdown();
        scanner.close();
    }
}