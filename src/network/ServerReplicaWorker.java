package network;

import util.BlockingQueue;

import java.io.IOException;
import java.net.Socket;
import java.util.Vector;

/**
 * A Threaded worker process overloaded to handle replica connections
 */
class ServerReplicaWorker extends ServerWorker{
    private Vector<ServerReplicaWorker> serverReplicas;

    /**
     * @param socket The socket which this worker should transmit and recieve from
     * @param msgs   The blocking queue to deliver messages to
     * @param serverReplicas a reference to the list of currently connected replicas
     * @throws IOException If the the socket is unable to produce input and/or output streams
     */
    ServerReplicaWorker(Socket socket, BlockingQueue msgs, Vector<ServerReplicaWorker> serverReplicas) throws IOException {
        super(socket, msgs);
        this.serverReplicas = serverReplicas;
    }

    /**
     * create an list of addresses of connected replicas except for the one represented by this connection and send it as requested
     * @param msg a string of the format "Update <TN>"
     * @throws IOException If sending of the list failes. Likely due to disconnection
     */
    @Override
    void operationUpdate(String msg) throws IOException{
        String replicaList = "[";
        for (ServerReplicaWorker s : serverReplicas){
            if (this != s){
                replicaList += "," + s;
            }
        }
        replicaList = replicaList.replaceFirst(",","") + "]";
        sendUTF(replicaList);
    }
}
