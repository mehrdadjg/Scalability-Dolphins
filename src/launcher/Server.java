package launcher;

import network.ServerMain;
import util.DocumentUpdate;

import java.util.LinkedList;

/**
 * Launcher for a backend replica server
 * This should remain as simple as possible, and only initialize the startup sequence for the server
 */
public class Server{
    static int port = 2227;
    LinkedList<DocumentUpdate> changes;

    public static void main(String[] args){
        if (args.length > 0){
            port = Integer.parseInt(args[0]);
        }

        ServerMain serverMain = new ServerMain(port);
        Thread serverMainThread = new Thread(serverMain);
        serverMainThread.start();


        //waits 30 seconds then shuts down the server
        //TODO implement a better method than waiting a fixed length of time
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        serverMain.shutdown();
    }
}