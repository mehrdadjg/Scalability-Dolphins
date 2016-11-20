package network;

import util.BlockingQueue;

import java.io.IOException;
import java.net.Socket;

/**
 * A Threaded worker process overloaded to handle replica connections
 */
class ProxyReplicaWorker extends ProxyWorker {
    private GroupManager groupManager;

    /**
     * @param socket The socket which this worker should transmit and recieve from
     * @param msgs   The blocking queue to deliver messages to
     * @param groupManager The group manager for connected systems
     * @throws IOException If the the socket is unable to produce input and/or output streams
     */
    ProxyReplicaWorker(Socket socket, BlockingQueue msgs, RecoveryManager recoveryManager, GroupManager groupManager) throws IOException {
        super(socket, msgs, recoveryManager);
        this.groupManager = groupManager;
    }

    /**
     * create an list of addresses of connected replicas except for the one represented by this connection and send it as requested
     * @param msg a string of the format "Update <TN>"
     * @throws IOException If sending of the list failes. Likely due to disconnection
     */
    @Override
    void operationUpdate(String msg) throws IOException{
        sendUTF(groupManager.replicasToString());
    }
}
