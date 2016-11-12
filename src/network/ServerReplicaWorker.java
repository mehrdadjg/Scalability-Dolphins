package network;

import util.BlockingQueue;

import java.io.IOException;
import java.net.Socket;
import java.util.Vector;

/**
 * INCOMPLETE
 */
class ServerReplicaWorker extends ServerWorker{
    Vector<String> msgs = new Vector<>();

    /**
     * @param socket The socket which this worker should transmit and recieve from
     * @param msgs   The blocking queue to deliver messages to
     * @throws IOException If the the socket is unable to produce input and/or output streams
     */
    ServerReplicaWorker(Socket socket, BlockingQueue msgs, RecoveryManager recoveryManager) throws IOException {
        super(socket, msgs, recoveryManager);
    }

    @Override
    protected void deliver(String msg){
        msgs.add(msg);
    }

    public String read(){
        if (msgs.size() > 0) {
            return (msgs.remove(0));
        }
        else {
            return "";
        }
    }
}














