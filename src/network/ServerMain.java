package network;

import util.BlockingQueue;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The main server class which accepts connections and creates worker threads
 */
public class ServerMain implements Runnable{
    private int port;
    private boolean isRunning;
    private Vector<ServerWorker> serverWorkers = new Vector<>();
    private BlockingQueue msgs = new BlockingQueue();

    public ServerMain(int port){
        this.port = port;
    }

    /**
     * This is where the loop occurs which accepts connections and creates the worker threads to handle them
     */
    @Override
    public void run() {
        ExecutorService executorService = Executors.newCachedThreadPool();

        try(ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println(InetAddress.getLocalHost() + ":" + serverSocket.getLocalPort());
            serverSocket.setSoTimeout(100); //set a socket timeout so that accept() does not block forever and lets us exit the loop without interrupting normal execution
            isRunning = true;
            while (isRunning){
                try{
                    Socket socket = serverSocket.accept();
                    System.out.println("Accepted new client!");
                    ServerWorker serverWorker = new ServerWorker(socket, msgs);
                    serverWorkers.addElement(serverWorker);
                    executorService.submit(serverWorker);
                } catch (SocketTimeoutException s){
                    //s.printStackTrace();              //suppress timeout exceptions when no connection requests occur
                }
                while (msgs.available()){
                    broadcast(msgs.retrieve());
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        executorService.shutdown();
    }

    /**
     * sets the shutdown flag, allowing the run() method to eventually exit it's accept loop and clean up.
     * also calls shutdown on all existing serverWorkers.
     */
    public void shutdown(){
        isRunning = false;
        serverWorkers.forEach(ServerWorker::shutdown);
    }

    /**
     * broadcasts a string to all clients
     * @param msg the message to be broadcast
     */
    private void broadcast(String msg){
        for (ServerWorker s : serverWorkers){
            //TODO determine if sending has failed due to client disconnect
            try {
                s.sendUTF(msg);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}