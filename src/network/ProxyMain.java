package network;

import util.BlockingQueue;

import java.io.*;
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
public class ProxyMain implements Runnable{
    private int clientPort;
    private int replicaPort;
    private boolean isRunning;
    private Vector<ProxyWorker> clients = new Vector<>();
    private Vector<ProxyReplicaWorker> replicas = new Vector<>();
    private BlockingQueue msgs = new BlockingQueue();
    private RecoveryManager recoveryManager = new RecoveryManager(replicas);

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

        //TODO Abort if desired port is already taken
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
                    clients.addElement(proxyWorker);
                    executorService.submit(proxyWorker);
                } catch (SocketTimeoutException s){
                    //s.printStackTrace();              //suppress timeout exceptions when no connection requests occur
                }

                try{
                    Socket socket = replicaSocket.accept();
                    System.out.println("Accepted new replica at: " + socket.getInetAddress() + ":" + socket.getPort());
                    ProxyReplicaWorker replicaWorker = new ProxyReplicaWorker(socket, msgs,recoveryManager, replicas);
                    replicas.addElement(replicaWorker);
                    executorService.submit(replicaWorker);
                } catch (SocketTimeoutException s){
                    //s.printStackTrace();              //suppress timeout exceptions when no connection requests occur
                }

                //read all available messages
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
     * also calls shutdown on all existing clients.
     */
    public void shutdown(){
        isRunning = false;
        clients.forEach(ProxyWorker::shutdown);
        replicas.forEach(ProxyReplicaWorker::shutdown);
    }

    /**
     * broadcasts a string to all clients
     * @param msg the message to be broadcast
     */
    private void broadcast(String msg){
        //TODO actively check for disconnects, rather than only when sending
        for (ProxyReplicaWorker p : replicas){
            try{
                p.sendUTF(msg);
                System.out.println("sent message to >" + p);
            } catch (IOException e) {
                //if sending has failed, socket is closed
                System.out.println("replica disconnected");
                p.shutdown();
                replicas.remove(p);
                //e.printStackTrace();
            }
        }
        for (ProxyWorker p : clients){
            try {
                p.sendUTF(msg);
                System.out.println("sent message to >" + p);
            } catch (IOException e) {
                //if sending has failed, client has likely disconnected
                System.out.println("client disconnected");
                p.shutdown();
                clients.remove(p);
                //e.printStackTrace();
            }
        }
    }
}