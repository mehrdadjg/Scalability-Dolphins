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
public class ServerMain implements Runnable{
    private int clientPort;
    private int replicaPort;
    private boolean isRunning;
    private Vector<ServerWorker> serverClients = new Vector<>();
    private Vector<ServerReplicaWorker> serverReplicas = new Vector<>();
    private BlockingQueue msgs = new BlockingQueue();
    private RecoveryManager recoveryManager = new RecoveryManager(serverReplicas);

    public ServerMain(int clientPort, int replicaPort){
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
        try(ServerSocket serverClientSocket = new ServerSocket(clientPort); ServerSocket serverReplicaSocket = new ServerSocket(replicaPort)) {
            System.out.println("Server started. local address is: " + InetAddress.getLocalHost() + ":" + serverClientSocket.getLocalPort());
            serverClientSocket.setSoTimeout(10); //set a socket timeout so that accept() does not block forever and lets us exit the loop without interrupting normal execution
            serverReplicaSocket.setSoTimeout(10);
            isRunning = true;
            while (isRunning){
                try{
                    Socket socket = serverClientSocket.accept();
                    System.out.println("Accepted new client at:" + socket.getInetAddress() + ":" + socket.getPort());
                    ServerWorker serverWorker = new ServerWorker(socket, msgs, recoveryManager);
                    serverClients.addElement(serverWorker);
                    executorService.submit(serverWorker);
                } catch (SocketTimeoutException s){
                    //s.printStackTrace();              //suppress timeout exceptions when no connection requests occur
                }

                try{
                    Socket socket = serverReplicaSocket.accept();
                    System.out.println("Accepted new replica at: " + socket.getInetAddress() + ":" + socket.getPort());
                    ServerReplicaWorker serverReplicaWorker = new ServerReplicaWorker(socket, msgs, recoveryManager);
                    serverReplicas.addElement(serverReplicaWorker);
                    executorService.submit(serverReplicaWorker);
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
     * also calls shutdown on all existing serverClients.
     */
    public void shutdown(){
        isRunning = false;
        serverClients.forEach(ServerWorker::shutdown);
        serverReplicas.forEach(ServerReplicaWorker::shutdown);
    }

    /**
     * broadcasts a string to all clients
     * @param msg the message to be broadcast
     */
    private void broadcast(String msg){
        //TODO actively check for disconnects, rather than only when sending
        for (ServerReplicaWorker s : serverReplicas){
            try{
                s.sendUTF(msg);
                System.out.println("sent message to >" + s);
            } catch (IOException e) {
                //if sending has failed, socket is closed
                System.out.println("replica disconnected");
                serverReplicas.remove(s);
                //e.printStackTrace();
            }
        }
        for (ServerWorker s : serverClients){
            try {
                s.sendUTF(msg);
            } catch (IOException e) {
                //if sending has failed, client has likely disconnected
                System.out.println("client disconnected");
                s.shutdown();
                serverClients.remove(s);
                //e.printStackTrace();
            }
        }
    }
}