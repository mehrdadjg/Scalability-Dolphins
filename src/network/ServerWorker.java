package network;

import util.BlockingQueue;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Vector;

import static java.lang.Thread.yield;

/**
 * A threaded worker process to handle communications to and from a single client
 */
class ServerWorker implements Runnable{
    private Socket socket;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;
    private boolean isRunning;
    private BlockingQueue msgs;
    boolean isRecovering = false;
    int knownTN;
    boolean TNupdated = false;
    private Vector<ServerReplicaWorker> serverReplicas;

    /**
     *
     * @param socket The socket which this worker should transmit and receive from
     * @param msgs The blocking queue to deliver messages to
     * @param serverReplicas a reference to the list of currently connected replicas
     * @throws IOException If the the socket is unable to produce input and/or output streams
     */
    ServerWorker(Socket socket, BlockingQueue msgs, Vector<ServerReplicaWorker> serverReplicas) throws IOException {
        this.socket = socket;
        this.msgs = msgs;
        this.serverReplicas = serverReplicas;
        dataOutputStream = new DataOutputStream(socket.getOutputStream());
        dataInputStream = new DataInputStream(socket.getInputStream());
    }

    @Override
    public void run() {
        //TODO determine if a client has disconnected and terminate thread
        try{

            isRunning = true;
            while (isRunning){

                //readUTF() blocks until success, so we must check before calling it to avoid waiting if a packet isnt ready
                if (dataInputStream.available() > 0){
                    String msg = dataInputStream.readUTF();

                    //TODO replace print statements with logging framework
                    System.out.println("Incoming Message from " + socket.getInetAddress() + ":" + socket.getPort() + " > " +  msg);

                    switch (msg.split(" ")[0]){
                        case "add"  : case "delete" :
                            //add the received message to the queue for the server to broadcast later
                            deliver(msg);
                            break;
                        case "update" :
                            //TODO send list of connected replicas
                            String replicaList = "[";
                            for (ServerReplicaWorker s : serverReplicas){
                                if (this != s){
                                    replicaList += "," + s;
                                }
                            }
                            replicaList = replicaList.replaceFirst(",","") + "]";
                            sendUTF(replicaList);
                            break;
                        case "tn" :
                            knownTN = Integer.parseInt(msg.split(" ")[1]);
                            TNupdated = true;
                            break;
                        default:
                            //Discard messages that are not recognized as part of the protocol
                            //TODO reply with <Error> according to protocol
                            sendUTF("error:incorrect format");
                            break;

                    }
                }
                yield();
            }


            //release the resources before the thread terminates
            dataInputStream.close();
            dataOutputStream.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void deliver(String msg){
        msgs.add(msg);
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
        dataOutputStream.writeUTF(msg);
        dataOutputStream.flush();
        System.out.println("\t sent");
    }

    /**
     * check if the client has disconnected and sent an EOF
     * @return true if the client is still connected
     */
    boolean isConnected(){
        try {
            if (dataInputStream.available() == 1){
                return false;
            }
        } catch (IOException e) {
            //e.printStackTrace();
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return (socket.getRemoteSocketAddress().toString().replaceFirst("/",""));
    }

    void resumeAfterRecovery(){
        isRecovering = false;
    }
}