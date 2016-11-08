package launcher;

import network.ServerMain;
import java.util.Scanner;

/**
 * A launcher for a proxy server
 * The purpose is to accept connections, rebroadcast received packets to all connected clients, and promote a back up replica if the leader fails
 * This should remain as simple as possible to minimize the chances of failure
 */
public class Proxy{
    private static int clientPort = 2227;
    private static int replicaPort = 3729;

    public static void main(String[] args){
        if (args.length > 0){
            clientPort = Integer.parseInt(args[0]);
        }

        ServerMain serverMain = new ServerMain(clientPort, replicaPort);
        new Thread(serverMain).start();

        //Await Quit command to shutdown
        Scanner scanner = new Scanner(System.in);
        String userInput;
        do {
            userInput = scanner.nextLine();
        } while (userInput.compareTo("Quit") != 0);

        serverMain.shutdown();
    }
}