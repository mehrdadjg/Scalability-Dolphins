package network;

import util.BlockingQueue;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The main server class which accepts connections and creates worker threads
 */
public class ProxyMain implements Runnable{
    private int clientPort;
    private int replicaPort;
    private boolean isRunning;
    private BlockingQueue msgs = new BlockingQueue();
    private boolean systemOffline = false;
    private GroupManager<ProxyWorker> clientGroupManager = new GroupManager<>();
    private GroupManager<ProxyReplicaWorker> replicaGroupManager = new GroupManager<>();
    private RecoveryManager recoveryManager = new RecoveryManager(replicaGroupManager);

    public ProxyMain(int clientPort, int replicaPort){
        this.clientPort = clientPort;
        this.replicaPort = replicaPort;
    }

    /**
     * This is where the loop occurs which accepts connections and creates the worker threads to handle them
     */
    @Override
    public void run() {
        ExecutorService executorService = Executors.newCachedThreadPool();

        try(ServerSocket clientSocket = new ServerSocket(clientPort); ServerSocket replicaSocket = new ServerSocket(replicaPort)) {
            System.out.println("Server started. local address is: " + InetAddress.getLocalHost() + ":" + clientSocket.getLocalPort());
            clientSocket.setSoTimeout(10); //set a socket timeout so that accept() does not block forever and lets us exit the loop without interrupting normal execution
            replicaSocket.setSoTimeout(10);
            isRunning = true;
            while (isRunning){
                try{
                    Socket socket = clientSocket.accept();
                    System.out.println("Accepted new client at:" + socket.getInetAddress() + ":" + socket.getPort());
                    ProxyWorker proxyWorker = new ProxyWorker(socket, msgs, recoveryManager);
                    clientGroupManager.add(proxyWorker);
                    executorService.submit(proxyWorker);
                } catch (SocketTimeoutException s){
                    //s.printStackTrace();              //suppress timeout exceptions when no connection requests occur
                }

                try{
                    Socket socket = replicaSocket.accept();
                    System.out.println("Accepted new replica at: " + socket.getInetAddress() + ":" + socket.getPort());
                    ProxyReplicaWorker replicaWorker = new ProxyReplicaWorker(socket, msgs,recoveryManager, replicaGroupManager);
                    replicaGroupManager.add(replicaWorker);
                    executorService.submit(replicaWorker);
                } catch (SocketTimeoutException s){
                    //s.printStackTrace();              //suppress timeout exceptions when no connection requests occur
                }

                //TODO Enable once issue #24 is fixed
                //If no replicas are available, respond to all client updates with error message
                /*
                if (replicas.isEmpty()){
                    systemOffline();
                } else {
                    systemOnline();
                }
                */

                //read all available messages
                while (msgs.available()){
                    broadcast(msgs.retrieve());
                }
            }
        } catch (java.net.BindException e){
            System.out.println("One or more of the requested ports are already taken");
            System.out.println("Aborting");
        } catch (IOException e) {
            e.printStackTrace();
        }

        executorService.shutdown();
    }

    /**
     * sets the shutdown flag, allowing the run() method to eventually exit it's accept loop and clean up.
     * also calls shutdown on all existing clients.
     */
    public void shutdown(){
        isRunning = false;
        replicaGroupManager.shutdown();
        clientGroupManager.shutdown();
    }

    /**
     * broadcasts a string to all clients
     * @param msg the message to be broadcast
     */
    private void broadcast(String msg){
        replicaGroupManager.broadcast(msg);
        clientGroupManager.broadcast(msg);
    }

    /**
     * Sets a flag to disable workers from accepting any new updates
     */
    /*private void systemOffline() {
        if (!systemOffline){
            for (ProxyWorker p : clients){
                p.setOffline(true);
            }
            systemOffline = true;
        }
    }*/

    /**
     * Disables a flag and allows workers to continue normal operation
     */
    /*private void systemOnline() {
        if (systemOffline){
            for (ProxyWorker p : clients){
                p.setOffline(false);
            }
            systemOffline = false;
        }
    }*/

}