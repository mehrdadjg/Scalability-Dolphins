package launcher;

import network.ServerMain;
import util.DocumentUpdate;

import java.util.LinkedList;
import java.util.Scanner;

/**
 * Launcher for a backend replica server
 * This should remain as simple as possible, and only initialize the startup sequence for the server
 */
public class Server{
    private static int port = 2227;
    LinkedList<DocumentUpdate> changes;

    public static void main(String[] args){
        if (args.length > 0){
            port = Integer.parseInt(args[0]);
        }

        ServerMain serverMain = new ServerMain(port);
        Thread serverMainThread = new Thread(serverMain);
        serverMainThread.start();

        //Await Quit command to shutdown
        Scanner scanner = new Scanner(System.in);
        String userInput;
        do {
            userInput = scanner.nextLine();
        } while (userInput.compareTo("Quit") != 0);

        serverMain.shutdown();
    }
}