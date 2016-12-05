package network;

import util.BlockingQueue;
import util.Resources;
import util.SocketStreamContainer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import static java.lang.Thread.yield;

/**
 * A threaded worker process to handle communications to and from a single client
 */
public class ProxyWorker implements Runnable{
    protected SocketStreamContainer io;
    private boolean isRunning = true;
    private BlockingQueue msgs;
    private RecoveryManager recoveryManager;

    /**
     *
     * @param socket The socket which this worker should transmit and receive from
     * @param msgs The blocking queue to deliver messages to
     * @throws IOException If the the socket is unable to produce input and/or output streams
     */
    ProxyWorker(Socket socket, BlockingQueue msgs, RecoveryManager recoveryManager) throws IOException {
        this.io = new SocketStreamContainer(socket);
        this.msgs = msgs;
        this.recoveryManager = recoveryManager;
    }

    @Override
    public void run() {
        try{
            while (isRunning){
                //readUTF() blocks until success, so we must check before calling it to avoid waiting if a packet isnt ready
                if (io.dataInputStream.available() > 0){
                    receiveMessage(io.dataInputStream.readUTF());
                }
                yield();
            }

            //release the resources before the thread terminates
            io.close();
        } catch (IOException e) {
            shutdown();
            //e.printStackTrace();
        }
    }

    void receiveMessage(String msg) throws IOException{
        if (!msg.startsWith("ping")) {
            System.out.println("Incoming Message from " + io.socket.getInetAddress() + ":" + io.socket.getPort() + " > " + msg);
        }

        switch (msg.split(" ")[0]){
            case "add"  : case "delete" :
                //add the received message to the queue for the server to broadcast later
                operationDeliver(msg);
                break;
            case "update" :
                operationUpdate(msg);
                break;
            case "list":
            	operationList(msg);
            	break;
            case "open":
            	operationCreate(msg);
            	break;
            case "error" : case "error:" :
                break;
            default:
                //Discard messages that are not recognized as part of the protocol
                sendUTF("error: incorrect format");
                break;
        }

    }

    /**
     * Retrieves updates with a TN of <TN> or higher from any available replica and sends them to the client
     * @param msg a string of the format "Update <ID> <TN>"
     * @throws IOException if sending process fails. Likely due to the client disconnecting.
     */
    void operationUpdate(String msg) throws IOException {
    	if(msg.split(" ")[1].compareTo("null") != 0) {
    		recoveryManager.recover(this, msg.split(" ")[1], Integer.parseInt(msg.split(" ")[2]));
    	} else {
    		sendUTF("bundle null []");
    	}
    }

    private void operationDeliver(String msg){
        msgs.add(msg, this);
    }

    private void operationList(String msg) throws IOException {
        GroupManager<ProxyReplicaWorker> groupManager = ProxyMain.replicaGroupManager;                                  //Get the group manager from proxyMain
        String reply = "error: system offline";

        while (reply.compareTo("error: system offline") == 0 && groupManager.replicasOnline()) {
            ProxyReplicaWorker primary = groupManager.firstElement();                                                   //check the first available replica
            String masterIP = primary.toString().split(":")[0];                                                         //get IP, we will be accessing through the recovery port
            try(SocketStreamContainer master = new SocketStreamContainer(new Socket(masterIP, Resources.RECOVERYPORT))){//create connection to replica over the recovery port
                master.dataOutputStream.writeUTF(msg);                                                                  //send the list request
                master.dataOutputStream.flush();
                reply = master.dataInputStream.readUTF();                                                               //store the reply
            } catch (IOException e) {
                groupManager.remove(primary);                                                                           //remove failed replica from groupManager
                primary.shutdown();
            }
        }                                                //keep trying replicas until we get a reply, or we run out of replicas
        sendUTF(reply);                                                                                                 //pass the reply to the client
    }
    
    private void operationCreate(String msg) throws IOException {
        GroupManager<ProxyReplicaWorker> groupManager = ProxyMain.replicaGroupManager;
        
        groupManager.broadcast(msg);
        sendUTF("done");
    }

    /**
     * clears the isRunning flag which allows the thread to terminate it's loop and clean up
     */
    void shutdown(){
        isRunning = false;
    }

    /**
     * Attempts to send a string to the connected client
     * @param msg The string to be transmitted. It does not need to be null terminated.
     * @throws IOException if the dataOutputStream fails to send
     */
    void sendUTF(String msg)throws IOException{
        System.out.println("Sending to >" + toString());
        System.out.println("\t Message > " + msg);
        io.dataOutputStream.writeUTF(msg);
        io.dataOutputStream.flush();
        System.out.println("\t sent");
    }

    @Override
    public String toString() {
        String ip = io.socket.getRemoteSocketAddress().toString().replaceFirst("/","").split(":")[0];
        String loopback = InetAddress.getLoopbackAddress().toString().split("/")[1];
        try {
            if (ip.compareTo(loopback) == 0){
                return InetAddress.getLocalHost().getHostAddress();
            }
        } catch (UnknownHostException e) {
            //Unlikely to occur unless own IP is unknown to the OS
            //e.printStackTrace();
        }
        return ip;
    }

    boolean isShutdown() {
        return !isRunning;
    }
}