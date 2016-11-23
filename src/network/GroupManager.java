package network;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.stream.Collectors;

/**
 * INCOMPLETE
 */
class GroupManager<E extends ProxyWorker>{
    private Vector<E> workers = new Vector<>();
    private static int cleanupInterval = 1000;

    GroupManager(){
        new Timer(true).schedule(new TimerTask() {
            @Override
            public void run() {
                cleanupDeadWorkers();
            }
        }, cleanupInterval, cleanupInterval);
    }

    private void cleanupDeadWorkers(){
        Vector<E> deadWorkers= workers.stream().filter(ProxyWorker::isShutdown).collect(Collectors.toCollection(Vector::new));

        deadWorkers.forEach(this::remove);
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
    }

    String workersToString() {
        String replicaList = "[";
        for (E s : workers){
            replicaList += "," + s;
        }
        return  replicaList.replaceFirst(",","") + "]";
    }

    boolean replicasOnline() {
        return (workers.size() > 0);
    }

    E firstElement() {
        return workers.firstElement();
    }
}
