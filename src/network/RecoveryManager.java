package network;


import util.Resources;
import util.SocketStreamContainer;
import util.TimeoutTimer;

import java.io.IOException;
import java.net.Socket;
import java.util.regex.Pattern;

/**
 * Helps a client or replica catch up after connecting
 */
class RecoveryManager {
    private GroupManager groupManager; //A list of available replicas to consult
    private String recoveryList = emptyList; //A mailbox for the list of changes needed for a recovery that is set by a ProxyReplicaWorker in a different thread
    private final static int defaultTimeout = 500;
    private final static String emptyList = "[]";
    private TimeoutTimer timer = new TimeoutTimer();
    private int recoveryPort = Resources.RECOVERYPORT;

    RecoveryManager(GroupManager groupManager){
        this.groupManager = groupManager;
    }

    void recover(ProxyWorker recoverer, int TNold){

        boolean recoveryComplete = false;
        while (!recoveryComplete){
            //if no replicas are online, abort
            if (groupManager.replicasOnline()){
                break;
            }

            String[] replicaAddresses = Pattern.compile("\\[|,|\\]").split(groupManager.replicasToString());

            //pick any replica
            ProxyReplicaWorker master = groupManager.firstReplica();
            String masterIP = master.toString().split(":")[0];

            try (SocketStreamContainer masterConnection = new SocketStreamContainer(new Socket(masterIP, recoveryPort))){
                //Query the chosen replica
                masterConnection.dataOutputStream.writeUTF("query_tn");
                masterConnection.dataOutputStream.flush();

                //setup a timer
                timer.startTimer(defaultTimeout);
                while (!timer.isTimeoutFlag() && masterConnection.dataInputStream.available() == 0) {Thread.yield();}

                int TNmax = 0;
                if (masterConnection.dataInputStream.available() > 0){
                    TNmax = Integer.parseInt(masterConnection.dataInputStream.readUTF().split(" ")[1]);
                } else {
                    throw new IOException("replica timed out");
                }

                masterConnection.dataOutputStream.writeUTF("transformations " + TNold + " " + TNmax);
                masterConnection.dataOutputStream.flush();

                timer.startTimer(defaultTimeout);
                while (!timer.isTimeoutFlag() && masterConnection.dataInputStream.available() == 0) {Thread.yield();}

                if (masterConnection.dataInputStream.available() > 0){
                    recoveryList = masterConnection.dataInputStream.readUTF();
                } else {
                    throw new IOException("replica timed out");
                }

            } catch (IOException e) {
                groupManager.removeReplica(master);
                master.shutdown();
                continue;
                //e.printStackTrace();
            }

            recoveryComplete = true;
        }

        try {
            recoverer.sendUTF(recoveryList);
        } catch (IOException e) {
            recoverer.shutdown();
            //e.printStackTrace();
        }

        //reset and prepare for the next request
        recoveryList = emptyList;
        timer.reset();
    }
}
