package network;


import handlers.FileHandler;
import util.Logger;
import util.Resources;
import util.SocketStreamContainer;
import util.TimeoutTimer;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Handles the incoming and outgoing connections
 */
public class ReplicaMain implements Runnable{
    private String proxyIp;
    private int proxyPort;
    private boolean isRunning;
//    private FileHandler fileHandler = new FileHandler("file.txt");
    private ReplicaReceiver replicaReceiver;
    private int pingInterval = Resources.TIMEOUT;
    private boolean proxyConnected;

    public ReplicaMain(String ip, int port, int recoveryPort) throws IOException {
        this.proxyIp = ip;
        this.proxyPort = port;
        this.replicaReceiver = new ReplicaReceiver(this, recoveryPort);
    }

    /**
     * opens a socket to the proxy, receives messages in a loop
     */
    @Override
    public void run() {
        //Launch the receiver thread which will service incoming update requests
        new Thread(replicaReceiver).start();                //Start the receiver thread that will handle recovery requests
        isRunning = true;
        while (isRunning) {

            try (SocketStreamContainer proxy = new SocketStreamContainer(new Socket(proxyIp, proxyPort));){

                proxyConnected = true;
                startTimer(proxy);                               //Start the timer to periodically ping the proxy
                System.out.println("Connected to proxy");

                //retrieve file contents
                synchronize(proxy);


                while (isRunning) {
                    //readUTF() blocks until success, so we must check before calling it to avoid waiting if a packet isnt ready
                    if (proxy.dataInputStream.available() > 0) {
                        String msg = proxy.dataInputStream.readUTF();

                        Logger.log("Incoming Message from proxy > " + msg, Logger.LogType.Info);
                        
                        FileHandler fileHandler = null;
                        switch (msg.split(" ")[0]) {
                            case "add":
                            case "delete":
                                //Write the incoming update to file
                            	fileHandler = new FileHandler(msg.split(" ")[1] + ".txt");
                                fileHandler.append(msg);
                                fileHandler.close();
                                fileHandler = null;
                                break;
                            case "query_tn":
                                //reply with the current TN
                            	if(msg.split(" ").length == 2) {
                            		fileHandler = new FileHandler(msg.split(" ")[1] + ".txt");
                            		sendUTF("tn [" + msg.split(" ")[1] + ":" + (fileHandler.read().length) + "]", proxy);
                            		fileHandler.close();
                            		fileHandler = null;
                            	} else {
                            		File root = new File(".");
                            		File[] docs = root.listFiles();
                            		String tns = "";
                            		for(int i = 0; i < docs.length; i++) {
                            			if(docs[i].isFile() && docs[i].getName().endsWith(".txt")) {
                            				String name = docs[i].getName().substring(0, docs[i].getName().length() - 4);
                            				fileHandler = new FileHandler(docs[i].getName());
                            				tns += (name + ":" + fileHandler.read().length + ",");
                            				fileHandler.close();
                            				fileHandler = null;
                            			}
                            		}
                            		
                            		if(tns.endsWith(",")) {
                            			sendUTF("tn [" + tns.substring(0, tns.length() - 1) + "]", proxy);
                            		} else {
                            			sendUTF("tn [" + tns + "]", proxy);
                            		}
                            	}
                                break;
                            case "transformations":
                                operationTransformations(msg.split(" ")[1], Integer.parseInt(msg.split(" ")[2]), Integer.parseInt(msg.split(" ")[3]), proxy);
                                break;
                            case "open":
                            	new FileHandler(msg.split(" ")[1] + ".txt");
                            	break;
                            default:
                                //Discard messages that are not recognized as part of the protocol
                                //System.out.println("error:incorrect format");
                                break;
                        }
                    }
                    if (!proxyConnected){
                        throw new IOException("proxy disconnected");
                    }
                }
            } catch (UnknownHostException e) {
                //Possible ip is not online, or ip is not valid
                e.printStackTrace();
            } catch (IOException e) {
                //Proxy is offline.
                System.out.println("Disconnected from proxy. attempting reconnect");
                //e.printStackTrace();
            }
        }
    }

    /**
     * Schedules a timer which periodically pings the host proxy
     */
    private void startTimer(SocketStreamContainer proxy){
        Timer timeoutTimer = new Timer(true);
        timeoutTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    sendUTF("ping", proxy);
                } catch (IOException e) {
                    timeoutTimer.cancel();
                    proxyConnected = false;
                    //e.printStackTrace();
                }
            }
        }, pingInterval, pingInterval);
    }

    /**
     * Attempts to send a string over a connection
      * @param msg The string to attempt sending
     * @param socketStreamContainer The connection to send the string over
     * @throws IOException If the connection is not open
     */
    private void sendUTF(String msg, SocketStreamContainer socketStreamContainer) throws IOException{
        socketStreamContainer.dataOutputStream.writeUTF(msg);
        socketStreamContainer.dataOutputStream.flush();
    }

    /**
     * Sends an update request to the proxy parses the reply into an array of addresses of available replicas
     * @param proxy The proxy to request the update from
     * @return An array of addresses of replicas
     * @throws IOException If the proxy disconnects
     */
    private String[] requestAvailableReplicas(SocketStreamContainer proxy)throws IOException{
        String[] replicaListString = new String[0];
        do {
            //send an update request to get the list of available replicas from the proxy
            sendUTF("update", proxy);

            //wait for reply
            TimeoutTimer timer = new TimeoutTimer();
            timer.startTimer(1000);
            while (!timer.isTimeoutFlag() && proxy.dataInputStream.available() == 0) {
                Thread.yield();
            }
            //retry if proxy has not yet sent a reply
            if (proxy.dataInputStream.available() == 0) {
                Logger.log("Proxy timed out during recovery. reattempting", Logger.LogType.Warning);
                continue;
            }

            //parse the IP addresses in the reply
            replicaListString = Pattern.compile("\\[|,|\\]").split(proxy.dataInputStream.readUTF());
        } while (replicaListString.length == 0);
        return replicaListString;
    }

    /**
     * create and return connections all IPs in an array except own
     * @param replicaListString A String array containing IPs
     * @return A list of connections that can be used to communicate to the IPs
     */
    private Vector<SocketStreamContainer> stringToConnections(String[] replicaListString){

        Vector<SocketStreamContainer> replicas = new Vector<>();
        for (String s : replicaListString) {
            try {
                if ((s.compareTo(InetAddress.getLocalHost().getHostAddress()) != 0) && (s.length() > 0)) {              //make sure own IP is not in the list so we dont connect to ourselves
                    try {
                        replicas.add(new SocketStreamContainer(new Socket(s.split(":")[0], Resources.RECOVERYPORT)));   //create the new connection and add it to the list
                    } catch (IOException e) {
                        //e.printStackTrace();
                    }
                }
            } catch (UnknownHostException e) {
                //e.printStackTrace();
            }
        }
        return replicas;
    }

    /**
     * broadcast a message to all connections in a list
     * @param msg the message to broadcast
     * @param replicas the connections to broadcast the message to
     */
    private void broadcast(String msg, Vector<SocketStreamContainer> replicas){
        for (SocketStreamContainer s : replicas) {
            try {
                sendUTF(msg, s);
            } catch (IOException e) {
                Logger.log("replica disconnected: " + s.socket.toString(), Logger.LogType.Warning);
                //e.printStackTrace();
            }
        }
    }

    private String[] readFromMultipleConnections(Vector<SocketStreamContainer> replicas){
        String[] replies = new String[replicas.size()];
        for (SocketStreamContainer s : replicas) {
            try {
                if (s.dataInputStream.available() > 0) {
                    replies[replicas.indexOf(s)] = s.dataInputStream.readUTF();
                } else {
                    throw new IOException("Replica timeout");                                                           //If the dataInputStream is empty, then they did not reply in time and are assumed disconnected
                }
            } catch (IOException e) {                                                                                   //If they are thought to be disconnected, then skip them
                Logger.log("replica disconnected: " + s.socket.toString(), Logger.LogType.Warning);
                //e.printStackTrace();
            }
        }
        return replies;
    }

    private void hashBroadcast(Vector<SocketStreamContainer> replicas){
        File root = new File(".");
        File[] docs = root.listFiles();
        String hashes = "hash [";
        for(int i = 0; i < docs.length; i++) {
            if(docs[i].isFile() && docs[i].getName().endsWith(".txt")) {
                String name = docs[i].getName().substring(0, docs[i].getName().length() - 4);
                int hashTN = 0;
                int hashcode = 0;
                try(FileHandler fileHandler = new FileHandler(docs[i].getName())){
                    hashTN = fileHandler.read().length;
                    hashcode = Arrays.hashCode(fileHandler.read());
                } catch (IOException e) {
                    //e.printStackTrace();
                }
                hashes += (name + ":" + hashTN + ":" + hashcode + ",");
            }
        }

        if(hashes.endsWith(",")) {
            hashes = (hashes.substring(0, hashes.length() - 1) + "]");
        } else {
            hashes = (hashes + "]");
        }

        broadcast(hashes, replicas);
    }

    /**
     * Synchronizes data between itself and all other replicas in the network
     * @param proxy The SocketStreamContainer object which has the currently open streams to the proxy
     * @throws IOException if the streams are disconnected
     */
    private void synchronize(SocketStreamContainer proxy) throws IOException {

        //get the list of available replicas from the proxy
        String[] replicaListString = requestAvailableReplicas(proxy);
        Logger.log("received addresses:  " + Arrays.toString(replicaListString), Logger.LogType.Info);

        //turn the list of replicas into connections to those replicas
        Vector<SocketStreamContainer> replicas = stringToConnections(replicaListString);
        Logger.log("Opened " + replicas.size() + " Connections for recovery", Logger.LogType.Info);

        //If no other replicas were found, then this is the only current replica and recovery is not necessary
        if (replicas.isEmpty()){
            Logger.log("No connections made. Ending recovery", Logger.LogType.Info);
            return;
        }

        //query all replicas for their TNs
        broadcast("query_tn", replicas);

        //wait
        TimeoutTimer timer = new TimeoutTimer();
        timer.startTimer(1000);
        while (!timer.isTimeoutFlag()){
            Thread.yield();
        }

        //read the replies from the replicas
        String[] replies = readFromMultipleConnections(replicas);

        //parse the replies into integers
        String[][] replicaFiles = new String[replicas.size()][];
        int[][] replicaTNs = new int[replicas.size()][];
        for (int i = 0; i < replies.length; i++) {
            String[] currentReplicaTN = Pattern.compile("\\[|,|\\]").split(replies[i].replaceFirst("tn ", ""));
            replicaFiles[i] = new String[currentReplicaTN.length];
            replicaTNs[i] = new int[currentReplicaTN.length];
            for (int j = 1; j < replicaTNs[i].length; j++){
                replicaFiles[i][j] = currentReplicaTN[j].split(":")[0];
                replicaTNs[i][j] = Integer.parseInt(currentReplicaTN[j].split(":")[1]);
            }
        }

        //query all replicas for their file hashes
        hashBroadcast(replicas);

        //wait
        timer = new TimeoutTimer();
        timer.startTimer(1000);
        while (!timer.isTimeoutFlag()){
            Thread.yield();
        }

        //receive the hashes from the replicas
        String[] hashes = readFromMultipleConnections(replicas);


        //for each replica...
        for(int i = 0; i < replicas.size(); i++){
            String currentReply = hashes[i].replaceFirst("signature ", "");
            SocketStreamContainer currentReplica = replicas.elementAt(i);
            //for each file at each replica...
            String[] split = Pattern.compile("\\[|,|\\]").split(currentReply);
            for (int j = 0; j < split.length; j++) {
                String currentFile = split[j];
                if (currentFile.length() == 0) {
                    continue;
                }
                String fileName = currentFile.split(":")[0];
                int hashLength = Integer.parseInt(currentFile.split(":")[1]);
                int hash = Integer.parseInt(currentFile.split(":")[2]);

                try (FileHandler fileHandler = new FileHandler(fileName + ".txt")) {
                    int ownTN = fileHandler.read().length;
                    int theirTN = hashLength;

                    List replicaFilesList = Arrays.asList(replicaFiles[i]);

                    if (replicaFilesList.contains(fileName)){

                        System.err.println("DEBUG " + theirTN + ":" + ownTN);
                        theirTN = replicaTNs[i][replicaFilesList.indexOf(fileName)];

                        System.err.println("DEBUG " + theirTN + ":" + ownTN);
                    }


                    //check if the files are compatible
                    boolean isCompatible = (hash == Arrays.hashCode(Arrays.copyOfRange(fileHandler.read(), 0, hashLength)));
                    //if their file is incompatible
                    if (!isCompatible) {
                        //and their file is smaller
                        if (theirTN < ownTN) {
                            //replace their file
                            sendUTF("replace " + fileName + ":" + Arrays.toString(Arrays.copyOfRange(fileHandler.read(), 0, ownTN)), currentReplica);
                        }
                        //and their file is larger or same size
                        else {
                            //delete own file
                            fileHandler.purge();
                            ownTN = 0;

                            sendUTF("transformations " + fileName.replaceFirst(".txt", "") + " " + ownTN + " " + theirTN, currentReplica);                         //Request the file
                            String reply = currentReplica.dataInputStream.readUTF();                                    //Download the file
                            if (reply.startsWith("bundle ")) {                                                          //Check formatting
                                operationBundle(reply);                                                                 //Apply the downloads
                            }
                        }
                        //If the files are compatible
                    } else {
                        //and their file is smaller
                        if (theirTN < ownTN) {
                            //send the difference
                            sendUTF("bundle " + fileName.replaceFirst(".txt", "") + " " + Arrays.toString(Arrays.copyOfRange(fileHandler.read(), theirTN, ownTN)), currentReplica);
                            //and their file is bigger
                        } else if (ownTN < theirTN) {
                            //request the difference
                            sendUTF("transformations " + fileName.replaceFirst(".txt", "") + " " + ownTN + " " + theirTN, currentReplica);                         //Request the file
                            String reply = currentReplica.dataInputStream.readUTF();                                    //Download the file
                            if (reply.startsWith("bundle ")) {                                                          //Check formatting
                                operationBundle(reply);                                                                 //Apply the downloads
                            }
                        }
                    }

                } catch (IOException e) {
                    //e.printStackTrace();
                }
            }
        }
        replicas.forEach(SocketStreamContainer::close);
        System.out.println("finished updating");
    }

    /**
     * Appends a list of transformations to the end of the file
     * @param msg The message, including the "bundle" header, the doc_name and the list of transformations
     * @throws IOException If the fileHandler cannot write to the file
     */
    void operationBundle(String msg) throws IOException{

            String[] msgs = Pattern.compile("\\[|,|\\]").split(msg.substring("bundle ".length()));

            //store nonempty values from the response array
            FileHandler fileHandler = new FileHandler(msg.split(" ")[1] + ".txt");
            for (int i = 1; i < msgs.length; i++){
                if (msgs[i].length() > 0){
                    fileHandler.append(msgs[i]);
                }
            }
            fileHandler.close();
    }

    /**
     * Replaces the contents of the file with the input list of transformations
     * @param msg The message, including the "replace" header and the list of transformations
     * @throws IOException If the fileHandler cannot write to the file
     */
    void operationReplace(String msg) throws IOException{

        msg = msg.substring("replace ".length());
        String fileName = msg.substring(0, msg.indexOf(":"));
        String[] msgs = Pattern.compile("\\[|,|\\]").split(msg);

        try(FileHandler fileHandler = new FileHandler(fileName)){
            //empty the file
            fileHandler.purge();

            //store nonempty values from the response array
            for (int i = 1; i < msgs.length; i++){
                if (msgs[i].length() > 0){
                    fileHandler.append(msgs[i]);
                }
            }
        }
    }

    /**
     * Sends the formatted list of transformations through the proxy
     * @param beginIndex The index of the first transformation needed
     * @param endIndex The index of the last transformation needed
     * @param socketStreamContainer The connection that the list should be sent on
     * @throws IOException If the connection is not open
     */
    void operationTransformations(String doc_name, int beginIndex, int endIndex, SocketStreamContainer socketStreamContainer) throws IOException {
    	System.out.println("ReplicaMain.java");
    	FileHandler fileHandler = new FileHandler(doc_name + ".txt");
        sendUTF("bundle " + doc_name + " " + Arrays.toString(Arrays.copyOfRange(fileHandler.read(), beginIndex, endIndex)), socketStreamContainer);
    }

    public void shutdown() {
        isRunning = false;
        replicaReceiver.shutdown();
    }
}

