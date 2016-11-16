package network;


import util.TimeoutTimer;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

/**
 * Helps a client or replica catch up after connecting
 */
class RecoveryManager {
    private Vector<ProxyReplicaWorker> serverWorkers = new Vector<>(); //A list of available replicas to consult
    String recoveryList = emptyList; //A mailbox for the list of changes needed for a recovery that is set by a ProxyReplicaWorker in a different thread
    private final static int defaultTimout = 500;
    private final static String emptyList = "[]";
    TimeoutTimer timer;

    RecoveryManager(Vector<ProxyReplicaWorker> serverWorkers){
        this.serverWorkers = serverWorkers;
    }

    void recover(ProxyWorker recoverer, int TNold){
        boolean recoveryComplete = false;

        for (ProxyWorker s : serverWorkers){
            if (!s.isConnected()){
                s.shutdown();
                serverWorkers.remove(s);
            }
        }

        while (!recoveryComplete){
            //if no other servers are online, abort

            if (serverWorkers.size() > 0 && recoverer != serverWorkers.firstElement()) {

                //request all replica TNs
                for (ProxyReplicaWorker s : serverWorkers) {

                    if (s.equals(recoverer)) {
                        //Skip the recoverer when checking for current TNs
                        continue;
                    }
                    s.knownTN = -1;

                    try {
                        s.sendUTF("query_tn");
                    } catch (IOException e) {
                        // replica has failed, remove it from the list of replicas and shut it down
                        serverWorkers.remove(s);
                        s.shutdown();
                        //e.printStackTrace();
                    }
                }

                //pick server with highest TN
                ProxyReplicaWorker master = null;//serverWorkers.firstElement();


                timer.startTimer(defaultTimout);
                while (!timer.isTimeoutFlag()) {Thread.yield();}

                int TNmax = -1;
                for (ProxyReplicaWorker s : serverWorkers) {

                    if (s.equals(recoverer)) {
                        //Skip the recoverer when checking for current TNs
                        continue;
                    }


                    if (s.knownTN > TNmax) {
                        master = s;
                        TNmax = s.knownTN;
                    }
                }


                if (master != null && TNmax > TNold) {
                    //retrieve all missed changes
                    try {
                        master.sendUTF("transformations " + TNold + " " + TNmax);
                    } catch (IOException e) {
                        //remove failed server and restart recovery if the master has failed
                        serverWorkers.remove(master);
                        master.shutdown();
                        continue;
                        //e.printStackTrace();
                    }
                }

                //wait for a reply
                timer.startTimer(defaultTimout);
                while (!timer.isTimeoutFlag() && (recoveryList.equals(emptyList))) {
                    Thread.yield();
                }
            }

            //forward changes to recoverer
            try {
                recoverer.sendUTF(recoveryList);
            } catch (IOException e) {
                //recoverer has failed again
                recoverer.shutdown();
                break;
                //e.printStackTrace();
            }

            recoveryComplete = true;

        }

        if (recoveryComplete){
            recoverer.resumeAfterRecovery();
        }

        //reset and prepare for the next request
        recoveryList = emptyList;

    }
}
