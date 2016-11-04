package network;

import java.io.IOException;
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
            serverSocket.setSoTimeout(100); //set a socket timeout so that accept() does not block forever and lets us exit the loop without interrupting normal execution
            isRunning = true;
            while (isRunning){
                try{
                    Socket socket = serverSocket.accept();
                    ServerWorker serverWorker = new ServerWorker(socket);
                    serverWorkers.addElement(serverWorker);
                    executorService.submit(serverWorker);
                } catch (SocketTimeoutException s){
                    //s.printStackTrace();              //suppress timeout exceptions when no connection requests occur
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
}
