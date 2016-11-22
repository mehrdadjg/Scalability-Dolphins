package network;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

/**
 * INCOMPLETE
 */
class GroupManager<E extends ProxyWorker>{
    private Vector<E> workers = new Vector<E>();
    static int cleanupInterval = 1000;

    GroupManager(){
        new Timer(true).schedule(new TimerTask() {
            @Override
            public void run() {
                cleanupDeadWorkers();
            }
        }, cleanupInterval, cleanupInterval);
    }

    private void cleanupDeadWorkers(){
        Vector<E> deadWorkers= new Vector<>();

        for (E worker : workers){
            if (worker.isShutdown()){
                deadWorkers.add(worker);
            }
        }

        for(E worker : deadWorkers){
            remove(worker);
        }
    }

    void add(E worker){
        workers.add(worker);
    }

    synchronized void remove(E worker){
        workers.remove(worker);
    }

    void shutdown(){
        workers.forEach(ProxyWorker::shutdown);
    }

    /**
     * broadcasts a string to all clients
     * @param msg the message to be broadcast
     */
    void broadcast(String msg){
        Vector<E> deadWorkers = new Vector<E>();
        for (E worker : workers){
            try{
                worker.sendUTF(msg);
                System.out.println("sent message to >" + worker);
            } catch (IOException e) {
                //if sending has failed, socket is closed
                System.out.println("replica/client disconnected");
                worker.shutdown();
            }
        }
        for (E worker : deadWorkers){
            remove(worker);
        }
        deadWorkers.removeAllElements();
    }

    String workersToString() {
        String replicaList = "[";
        for (E s : workers){
            replicaList += "," + s;
        }
        return  replicaList.replaceFirst(",","") + "]";
    }

    public boolean replicasOnline() {
        return (workers.size() > 0);
    }

    public E firstElement() {
        return workers.firstElement();
    }
}
