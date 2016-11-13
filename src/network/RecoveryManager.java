package network;


import java.io.IOException;
import java.util.Vector;

/**
 * Helps a client or replica catch up after connecting
 */
class RecoveryManager {
    private Vector<ServerReplicaWorker> serverWorkers = new Vector<>(); //A list of available replicas to consult
    String recoveryList = ""; //A mailbox for the list of changes needed for a recovery that is set by a ServerReplicaWorker in a different thread

    RecoveryManager(Vector<ServerReplicaWorker> serverWorkers){
        this.serverWorkers = serverWorkers;
    }

    void recover(ServerWorker recoverer, int TNold){
        //request all replica TNs
        for (ServerReplicaWorker s : serverWorkers){

            if (s.equals(recoverer)){
                //Skip the recoverer when checking for current TNs
                continue;
            }

            try {
                s.sendUTF("query_tn");
                s.TNupdated = false;
            } catch (IOException e) {
                // replica has failed, remove it from the list of replicas and shut it down
                serverWorkers.remove(s);
                s.shutdown();
                //e.printStackTrace();
            }
        }

        //pick server with highest TN
        ServerReplicaWorker master = serverWorkers.firstElement();
        int TNmax = -1;
        for (ServerReplicaWorker s : serverWorkers){

            if (s.equals(recoverer)){
            //Skip the recoverer when checking for current TNs
            continue;
        }
            while(!s.TNupdated){Thread.yield();}

            if (s.knownTN > TNmax){
                master = s;
                TNmax = s.knownTN;
            }
        }

        //retrieve all missed changes
        try {
            master.sendUTF("transformations " + TNold + " " + TNmax);
        } catch (IOException e) {
            //TODO remove failed server from serverWorkers and restart recovery if the master has failed
            e.printStackTrace();
        }

        //wait for a reply
        while (recoveryList.length() == 0){Thread.yield();}

        //forward changes to recoverer
        try {
            recoverer.sendUTF(recoveryList);
        } catch (IOException e) {
            e.printStackTrace();
        }

        recoverer.resumeAfterRecovery();
    }
}
