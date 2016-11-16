package launcher;

import network.ProxyMain;

import java.util.Scanner;

/**
 * A launcher for a proxy server
 * The purpose is to accept connections, rebroadcast received packets to all connected clients, and promote a back up replica if the leader fails
 * This should remain as simple as possible to minimize the chances of failure
 */
public class Proxy{
    private static int clientPort = 22;
    public static int replicaPort = 21;

    public static void main(String[] args){
        if (args.length > 0){
            clientPort = Integer.parseInt(args[0]);
            if (args.length > 1){
                replicaPort = Integer.parseInt(args[1]);
            }
        }

        ProxyMain proxyMain = new ProxyMain(clientPort, replicaPort);
        new Thread(proxyMain).start();

        //Await Quit command to shutdown
        Scanner scanner = new Scanner(System.in);
        String userInput;
        do {
            userInput = scanner.nextLine();
        } while (userInput.compareTo("Quit") != 0);

        proxyMain.shutdown();
        scanner.close();
    }
}