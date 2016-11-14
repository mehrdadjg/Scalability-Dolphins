package network;


import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

/**
 * Helps a client or replica catch up after connecting
 */
class RecoveryManager {
    private Vector<ServerReplicaWorker> serverWorkers = new Vector<>(); //A list of available replicas to consult
    String recoveryList = "[]"; //A mailbox for the list of changes needed for a recovery that is set by a ServerReplicaWorker in a different thread
    private boolean timeoutFlag = false;
    private final static int defaultTimout = 500;
    private final static String emptyList = "[]";
    Timer timer;

    RecoveryManager(Vector<ServerReplicaWorker> serverWorkers){
        this.serverWorkers = serverWorkers;
    }

    void recover(ServerWorker recoverer, int TNold){
        boolean recoveryComplete = false;

        for (ServerWorker s : serverWorkers){
            if (!s.isConnected()){
                s.shutdown();
                serverWorkers.remove(s);
            }
        }

        while (!recoveryComplete){
            //if no other servers are online, abort
            if (serverWorkers.size() < 2){
                try {
                    recoverer.sendUTF("[]");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            //request all replica TNs
            for (ServerReplicaWorker s : serverWorkers){

                if (s.equals(recoverer)){
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
            ServerReplicaWorker master = null;//serverWorkers.firstElement();


            startTimer();

            while(!timeoutFlag){Thread.yield();}

            int TNmax = -1;
            for (ServerReplicaWorker s : serverWorkers){

                if (s.equals(recoverer)){
                    //Skip the recoverer when checking for current TNs
                    continue;
                }


                if (s.knownTN > TNmax){
                    master = s;
                    TNmax = s.knownTN;
                }
            }
            //abort if nobody has a higher tn
            if (master == null){
                break;
            }

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

            //wait for a reply
            startTimer();

            while(!timeoutFlag && (recoveryList.equals(emptyList))){Thread.yield();}

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
        recoveryList = "[]";
    }

    private void startTimer(){
        timeoutFlag = false;
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                timeoutFlag = true;
                timer.cancel();
            }
        }, defaultTimout);
    }
}
