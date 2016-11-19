package launcher;

import network.ProxyMain;
import util.Resources;

import java.io.IOException;
import java.util.Scanner;

import static java.lang.Thread.yield;
/**
 * A launcher for a proxy server
 * The purpose is to accept connections, rebroadcast received packets to all connected clients, and promote a back up replica if the leader fails
 * This should remain as simple as possible to minimize the chances of failure
 */
public class Proxy{
    private static int clientPort = Resources.CLIENTPORT;
    private static int replicaPort = Resources.REPLICAPORT;

    public static void main(String[] args){
        if (args.length > 0){
            clientPort = Integer.parseInt(args[0]);
            if (args.length > 1){
                replicaPort = Integer.parseInt(args[1]);
            }
        }

        ProxyMain proxyMain = new ProxyMain(clientPort, replicaPort);
        Thread proxyThread = new Thread(proxyMain);
        proxyThread.start();


        //Await Quit command to shutdown
        Scanner scanner = new Scanner(System.in);
        String userInput = "";
        do {
            //check input before reading to avoid blocking
            try {
                if (System.in.available() > 0){
                    userInput = scanner.nextLine();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            //determine if the main proxy thread has terminated on it's own and finish the application
            if (proxyThread.getState().compareTo(Thread.State.TERMINATED) == 0){
                break;
            }
            yield();
        } while (userInput.compareTo("Quit") != 0);

        proxyMain.shutdown();
        scanner.close();
    }
}