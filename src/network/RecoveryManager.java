package network;


import util.Resources;
import util.SocketStreamContainer;
import util.TimeoutTimer;

import java.io.IOException;
import java.net.Socket;

/**
 * Helps a client or replica catch up after connecting
 */
class RecoveryManager {
    private GroupManager<ProxyReplicaWorker> groupManager; //A list of available replicas to consult
    private String recoveryList = emptyList; //A mailbox for the list of changes needed for a recovery that is set by a ProxyReplicaWorker in a different thread
    private final static int defaultTimeout = Resources.TIMEOUT;
    private final static String emptyList = "bundle []";
    private TimeoutTimer timer = new TimeoutTimer();
    private int recoveryPort = Resources.RECOVERYPORT;

    RecoveryManager(GroupManager<ProxyReplicaWorker> groupManager){
        this.groupManager = groupManager;
    }

    void recover(ProxyWorker recoverer, String doc_name, int TNold){

        boolean recoveryComplete = false;
        while (!recoveryComplete){
            //if no replicas are online, abort
            if (!groupManager.replicasOnline()){
                break;
            }

            //pick any replica
            ProxyReplicaWorker master = groupManager.firstElement();
            String masterIP = master.toString().split(":")[0];

            try (SocketStreamContainer masterConnection = new SocketStreamContainer(new Socket(masterIP, recoveryPort))){
                //Query the chosen replica
                masterConnection.dataOutputStream.writeUTF("query_tn " + doc_name);
                masterConnection.dataOutputStream.flush();

                //setup a timer
                timer.startTimer(defaultTimeout);
                while (!timer.isTimeoutFlag() && masterConnection.dataInputStream.available() == 0) {Thread.yield();}

                int TNmax;
                if (masterConnection.dataInputStream.available() > 0){
                	String tn_response = masterConnection.dataInputStream.readUTF();
                	String tn = tn_response.substring(tn_response.indexOf("[") + 1, tn_response.indexOf("]")).split(":")[1];
                	
                    TNmax = Integer.parseInt(tn);
                } else {
                    throw new IOException("replica timed out");
                }

                masterConnection.dataOutputStream.writeUTF("transformations " + doc_name + " " + TNold + " " + TNmax);
                masterConnection.dataOutputStream.flush();

                timer.startTimer(defaultTimeout);
                while (!timer.isTimeoutFlag() && masterConnection.dataInputStream.available() == 0) {Thread.yield();}

                if (masterConnection.dataInputStream.available() > 0){
                    recoveryList = masterConnection.dataInputStream.readUTF();
                } else {
                    throw new IOException("replica timed out");
                }

            } catch (IOException e) {
                groupManager.remove(master);
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
