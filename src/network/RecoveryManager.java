package network;


import java.io.IOException;
import java.util.Vector;

/**
 * INCOMPLETE
 */
class RecoveryManager {
    private Vector<ServerReplicaWorker> serverWorkers = new Vector<>();

    RecoveryManager(Vector<ServerReplicaWorker> serverWorkers){
        this.serverWorkers = serverWorkers;
    }

    void recover(ServerWorker recoverer, int TNold){
        //recieve all replica TNs
        for (ServerReplicaWorker s : serverWorkers){

            if (s.equals(recoverer)){
                //Skip the recoverer when checking for current TNs
                continue;
            }

            try {
                s.sendUTF("query_tn");
                s.TNupdated = false;
            } catch (IOException e) {
                e.printStackTrace();
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



           /* String[] queryResponse = s.read().split(" ");

            if (queryResponse[0].compareTo("tn") == 0){
                int TNresponse = Integer.parseInt(queryResponse[1]);
                if (TNresponse > TNmax){
                    master = s;
                    TNmax = TNresponse;
                }
            }
            */
            if (s.knownTN > TNmax){
                master = s;
                TNmax = s.knownTN;
            }
        }

        //retrieve all missed changes
        try {
            master.sendUTF("transformations " + TNold + " " + TNmax);
        } catch (IOException e) {
            //TODO remove failed server from serverWorkers and restart recovery
            e.printStackTrace();
        }

        //forward changes to recoverer
        try {
            recoverer.sendUTF(master.read());
        } catch (IOException e) {
            e.printStackTrace();
        }

        recoverer.resumeAfterRecovery();
    }
}
