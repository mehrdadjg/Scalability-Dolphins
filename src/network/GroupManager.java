package network;

import java.io.IOException;
import java.util.Vector;

/**
 * INCOMPLETE
 */
class GroupManager<E extends ProxyWorker>{
    private Vector<E> workers = new Vector<E>();

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
                System.out.println("replica disconnected");
                worker.shutdown();
                remove(worker);
                //e.printStackTrace();
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

    public boolean replicasOnline() {
        return (workers.size() > 0);
    }

    public E firstElement() {
        return workers.firstElement();
    }
}
