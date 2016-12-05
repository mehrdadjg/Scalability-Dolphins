package network;

import util.BlockingQueue;
import util.Resources;

import java.io.IOException;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A Threaded worker process overloaded to handle replica connections
 */
class ProxyReplicaWorker extends ProxyWorker {
    private GroupManager groupManager;
    private Timer timeoutTimer = new Timer(true);
    private int timeout = Resources.TIMEOUT * 2;

    /**
     * @param socket The socket which this worker should transmit and recieve from
     * @param msgs   The blocking queue to deliver messages to
     * @param groupManager The group manager for connected systems
     * @throws IOException If the the socket is unable to produce input and/or output streams
     */
    ProxyReplicaWorker(Socket socket, BlockingQueue msgs, RecoveryManager recoveryManager, GroupManager groupManager) throws IOException {
        super(socket, msgs, recoveryManager);
        this.groupManager = groupManager;
        startTimer();
    }

    private void startTimer(){
        timeoutTimer.cancel();
        timeoutTimer = new Timer(true);
        timeoutTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                timeoutTimer.cancel();
                shutdown();
            }
        }, timeout);
    }

    @Override
    void receiveMessage(String msg) throws IOException {
        startTimer();

        if (!msg.startsWith("ping")) {
            System.out.println("Incoming Message from " + io.socket.getInetAddress() + ":" + io.socket.getPort() + " > " + msg);
        }

        switch (msg.split(" ")[0]) {
            case "update":
                operationUpdate(msg);
                break;
            case "ping" :
                break;
            default:
                //Discard messages that are not recognized as part of the protocol
                sendUTF("error:incorrect format");
                break;
        }
    }

    /**
     * create an list of addresses of connected replicas except for the one represented by this connection and send it as requested
     * @param msg a string of the format "Update <TN>"
     * @throws IOException If sending of the list failes. Likely due to disconnection
     */
    @Override
    void operationUpdate(String msg) throws IOException{
        sendUTF(groupManager.workersToString());
    }
}
