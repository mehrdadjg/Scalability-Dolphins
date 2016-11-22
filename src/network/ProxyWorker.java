package network;

import util.BlockingQueue;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import static java.lang.Thread.yield;

/**
 * A threaded worker process to handle communications to and from a single client
 */
class ProxyWorker implements Runnable{
    Socket socket;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;
    private boolean isRunning = true;
    private BlockingQueue msgs;
    private RecoveryManager recoveryManager;
    private volatile boolean offline = false;

    /**
     *
     * @param socket The socket which this worker should transmit and receive from
     * @param msgs The blocking queue to deliver messages to
     * @throws IOException If the the socket is unable to produce input and/or output streams
     */
    ProxyWorker(Socket socket, BlockingQueue msgs, RecoveryManager recoveryManager) throws IOException {
        this.socket = socket;
        this.msgs = msgs;
        this.recoveryManager = recoveryManager;
        dataOutputStream = new DataOutputStream(socket.getOutputStream());
        dataInputStream = new DataInputStream(socket.getInputStream());
    }

    @Override
    public void run() {
        try{
            while (isRunning){
                //readUTF() blocks until success, so we must check before calling it to avoid waiting if a packet isnt ready
                if (dataInputStream.available() > 0){
                    receiveMessage(dataInputStream.readUTF());
                }
                yield();
            }

            //release the resources before the thread terminates
            dataInputStream.close();
            dataOutputStream.close();
            socket.close();
        } catch (IOException e) {
            shutdown();
            //e.printStackTrace();
        }
    }

    void receiveMessage(String msg) throws IOException{
        //TODO replace print statements with logging framework
        if (!msg.startsWith("ping")) {
            System.out.println("Incoming Message from " + socket.getInetAddress() + ":" + socket.getPort() + " > " + msg);
        }

        if (offline){
            //System offline
            sendUTF("error: system offline");
        } else {
            switch (msg.split(" ")[0]){
                case "add"  : case "delete" :
                    //add the received message to the queue for the server to broadcast later
                    operationDeliver(msg);
                    break;
                case "update" :
                    operationUpdate(msg);
                    break;
                default:
                    //Discard messages that are not recognized as part of the protocol
                    sendUTF("error:incorrect format");
                    break;
            }
        }
    }

    /**
     * Retrieves updates with a TN of <TN> or higher from any available replica and sends them to the client
     * @param msg a string of the format "Update <TN>"
     * @throws IOException if sending process fails. Likely due to the client disconnecting.
     */
    void operationUpdate(String msg) throws IOException {
        recoveryManager.recover(this, Integer.parseInt(msg.split(" ")[1]));
    }

    private void operationDeliver(String msg){
        msgs.add(msg);
    }

    /**
     * clears the isRunning flag which allows the thread to terminate it's loop and clean up
     */
    void shutdown(){
        isRunning = false;
        System.out.println("disconnected worker >" + socket);
    }

    /**
     * Attempts to send a string to the connected client
     * @param msg The string to be transmitted. It does not need to be null terminated.
     * @throws IOException if the dataOutputStream fails to send
     */
    void sendUTF(String msg)throws IOException{
        System.out.println("Sending to >" + toString());
        System.out.println("\t Message > " + msg);
        dataOutputStream.writeUTF(msg);
        dataOutputStream.flush();
        System.out.println("\t sent");
    }

    @Override
    public String toString() {
        String ip = socket.getRemoteSocketAddress().toString().replaceFirst("/","").split(":")[0];
        String loopback = InetAddress.getLoopbackAddress().toString().split("/")[1];
        try {
            if (ip.compareTo(loopback) == 0){
                return InetAddress.getLocalHost().getHostAddress();
            }
        } catch (UnknownHostException e) {
        }
        return ip;
    }

    void setOffline(boolean val){
        offline = val;
    }

    public boolean isShutdown() {
        return !isRunning;
    }
}