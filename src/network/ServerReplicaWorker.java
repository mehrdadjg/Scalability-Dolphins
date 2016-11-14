package network;

import util.BlockingQueue;

import java.io.IOException;
import java.net.Socket;
import java.util.Vector;

/**
 * A Threaded worker process overloaded to handle replica connections
 */
class ServerReplicaWorker extends ServerWorker{

    /**
     * @param socket The socket which this worker should transmit and recieve from
     * @param msgs   The blocking queue to deliver messages to
     * @throws IOException If the the socket is unable to produce input and/or output streams
     */
    ServerReplicaWorker(Socket socket, BlockingQueue msgs, RecoveryManager recoveryManager) throws IOException {
        super(socket, msgs, recoveryManager);
    }
}
