package network;

import java.io.IOException;
import java.util.Vector;

/**
 * INCOMPLETE
 */
class GroupManager{
    private Vector<ProxyWorker> clients = new Vector<>();
    private Vector<ProxyReplicaWorker> replicas = new Vector<>();

    void addClient(ProxyWorker proxyWorker){
        clients.add(proxyWorker);
    }

    void addReplica(ProxyReplicaWorker proxyReplicaWorker){
        replicas.add(proxyReplicaWorker);
    }

    private synchronized void removeClient(ProxyWorker proxyWorker){
        clients.remove(proxyWorker);
    }

    synchronized void removeReplica(ProxyReplicaWorker proxyReplicaWorker){
        replicas.remove(proxyReplicaWorker);
    }

    void shutdown() {
        clients.forEach(ProxyWorker::shutdown);
        replicas.forEach(ProxyReplicaWorker::shutdown);
    }

    /**
     * broadcasts a string to all clients
     * @param msg the message to be broadcast
     */
    void broadcast(String msg){
        for (ProxyReplicaWorker p : replicas){
            try{
                p.sendUTF(msg);
                System.out.println("sent message to >" + p);
            } catch (IOException e) {
                //if sending has failed, socket is closed
                System.out.println("replica disconnected");
                p.shutdown();
                removeReplica(p);
                //e.printStackTrace();
            }
        }
        for (ProxyWorker p : clients){
            try {
                p.sendUTF(msg);
                System.out.println("sent message to >" + p);
            } catch (IOException e) {
                //if sending has failed, client has likely disconnected
                System.out.println("client disconnected");
                p.shutdown();
                removeClient(p);
                //e.printStackTrace();
            }
        }
    }

    String replicasToString() {
        String replicaList = "[";
        for (ProxyReplicaWorker s : replicas){
            replicaList += "," + s;
        }
        return  replicaList.replaceFirst(",","") + "]";
    }

    public boolean replicasOnline() {
        return (replicas.size() > 0);
    }

    public ProxyReplicaWorker firstReplica() {
        return replicas.firstElement();
    }
}
