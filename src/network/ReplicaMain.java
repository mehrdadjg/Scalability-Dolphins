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
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.regex.Pattern;

/**
 * Handles the incoming and outgoing connections
 */
public class ReplicaMain implements Runnable{
    private String proxyIp;
    private int proxyPort;
    private boolean isRunning;
    private FileHandler fileHandler = new FileHandler("file.txt");
    private ReplicaReceiver replicaReceiver;
    private int pingInterval = Resources.TIMEOUT;
    private boolean proxyConnected;

    public ReplicaMain(String ip, int port, int recoveryPort) throws IOException {
        this.proxyIp = ip;
        this.proxyPort = port;
        this.replicaReceiver = new ReplicaReceiver(this, fileHandler, recoveryPort);
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

                        switch (msg.split(" ")[0]) {
                            case "add":
                            case "delete":
                                //Write the incoming update to file
                                fileHandler.append(msg);
                                break;
                            case "query_tn":
                                //reply with the current TN
                                sendUTF("tn " + (fileHandler.read().length), proxy);
                                break;
                            case "transformations":
                                operationTransformations(Integer.parseInt(msg.split(" ")[1]), Integer.parseInt(msg.split(" ")[2]), proxy);
                                break;
                            default:
                                //Discard messages that are not recognized as part of the protocol
                                sendUTF("error:incorrect format", proxy);
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
        fileHandler.close();
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
        //startTimer();
        socketStreamContainer.dataOutputStream.writeUTF(msg);
        socketStreamContainer.dataOutputStream.flush();
    }

    /**
     * Synchronizes data between itself and all other replicas in the network
     * @param proxy The SocketStreamContainer object which has the currently open streams to the proxy
     * @throws IOException if the streams are disconnected
     */
    private void synchronize(SocketStreamContainer proxy) throws IOException {
        boolean recoveryComplete = false;

        while (!recoveryComplete && isRunning) {
            //send an update request
            sendUTF("update",proxy);

            //wait for reply
            TimeoutTimer timer = new TimeoutTimer();
            timer.startTimer(1000);
            while (!timer.isTimeoutFlag() && proxy.dataInputStream.available() == 0) {
                Thread.yield();
            }
            //retry if proxy has not yet sent a reply
            if (proxy.dataInputStream.available() == 0){
                Logger.log("Proxy timed out during recovery. reattempting", Logger.LogType.Warning);
                continue;
            }

            //parse the IP addresses in the reply
            String[] replicaListString = Pattern.compile("\\[|,|\\]").split(proxy.dataInputStream.readUTF());
            Logger.log("received addresses:  " + Arrays.toString(replicaListString), Logger.LogType.Info);

            //create connections to all of the received IP addresses
            Vector<SocketStreamContainer> replicas = new Vector<>();
            for (String s : replicaListString) {
                if ((s.compareTo(InetAddress.getLocalHost().getHostAddress()) != 0) && (s.length() > 0)) {
                    try {
                        replicas.add(new SocketStreamContainer(new Socket(s.split(":")[0], Resources.RECOVERYPORT)));
                    } catch (IOException e) {
                        //e.printStackTrace();
                    }
                }
            }
            Logger.log("Opened " + replicas.size() + " Connections for recovery", Logger.LogType.Info);

            if (replicas.isEmpty()){
                Logger.log("No connections made. Ending recovery", Logger.LogType.Info);
                recoveryComplete = true;
                break;
            }

            //query all replicas for their TNs
            for (SocketStreamContainer s : replicas) {
                try {
                    sendUTF("query_tn", s);
                } catch (IOException e) {
                    Logger.log("replica disconnected: " + s.socket.toString(), Logger.LogType.Warning);
                    //e.printStackTrace();
                }
            }

            //wait for reply
            timer = new TimeoutTimer();
            timer.startTimer(1000);
            while (!timer.isTimeoutFlag() && replicas.firstElement().dataInputStream.available() == 0) {
                Thread.yield();
            }

            int[] replicaTNs = new int[replicas.size()];
            for (SocketStreamContainer s : replicas) {
                try {
                    if (s.dataInputStream.available() > 0) {
                        replicaTNs[replicas.indexOf(s)] = Integer.parseInt(s.dataInputStream.readUTF().split(" ")[1]);      //Get their current TN
                    } else {
                        throw new IOException("Replica timeout");                                                           //If the dataInputStream is empty, then they did not reply in time and are assumed disconnected
                    }
                } catch (IOException e) {                                                                                    //If they are thought to be disconnected, then skip them
                    Logger.log("replica disconnected: " + s.socket.toString(), Logger.LogType.Warning);
                    //e.printStackTrace();
                }
            }

            //request hash from all replicas
            int TNown = fileHandler.read().length;
            for (SocketStreamContainer s : replicas) {
                sendUTF("hash " + fileHandler.getFileName() + " " + Math.min(TNown, replicaTNs[replicas.indexOf(s)]),s);               //Request the hash of transformations that should exist in both documents
            }

            //wait for reply
            timer = new TimeoutTimer();
            timer.startTimer(1000);
            while (!timer.isTimeoutFlag()) {
                Thread.yield();
            }

            //Request any new updates from each replica
            for (SocketStreamContainer s : replicas) {
                try {
                    if (s.dataInputStream.available() > 0) {
                        String hash_rs = s.dataInputStream.readUTF();

                        String fileName = hash_rs.split(" ")[1];
                        int length = Integer.parseInt(hash_rs.split(" ")[2]);
                        int hash = Integer.parseInt(hash_rs.split(" ")[3]);

                        if (replicaTNs[replicas.indexOf(s)] > TNown) {                                                                            //If their TN is greater than own
                            if (fileHandler.hash(length) != hash) {
                                TNown = 0;
                                fileHandler.purge();
                            }

                            sendUTF("transformations " + TNown + " " + replicaTNs[replicas.indexOf(s)],s);                           //Request the missing TNs
                            String reply = s.dataInputStream.readUTF();                                                         //Download the missing TNs
                            if (reply.startsWith("bundle ")) {                                                                   //Check formatting
                                operationBundle(reply);                                                                         //Apply the downloads
                                TNown = replicaTNs[replicas.indexOf(s)];
                            }
                        } else if (replicaTNs[replicas.indexOf(s)] < TNown) {                                               //Otherwise, if their TN is less than own
                            if (fileHandler.hash(length) != hash) {
                                sendUTF("replace " + Arrays.toString(Arrays.copyOfRange(fileHandler.read(), 0, TNown)), s);   //send a replace message to have them replace their entire file
                            } else {
                                sendUTF("bundle " + Arrays.toString(Arrays.copyOfRange(fileHandler.read(), replicaTNs[replicas.indexOf(s)], TNown)), s);    //Send the TNs they are missing

                            }
                        }
                    } else {
                        throw new IOException("Replica timeout");                                                           //If the dataInputStream is empty, then they did not reply in time and are assumed disconnected
                    }
                } catch (IOException e) {                                                                                    //If they are thought to be disconnected, then skip them
                    Logger.log("replica disconnected: " + s.socket.toString(), Logger.LogType.Warning);
                    //e.printStackTrace();
                }
            }


            replicas.forEach(SocketStreamContainer::close);
            recoveryComplete = true;
        }
        System.out.println("finished updating");
    }

    /**
     * Appends a list of transformations to the end of the file
     * @param msg The message, including the "bundle" header and the list of transformations
     * @throws IOException If the fileHandler cannot write to the file
     */
    void operationBundle(String msg) throws IOException{
            msg = msg.substring("bundle ".length());
            String[] msgs = Pattern.compile("\\[|,|\\]").split(msg);

            //store nonempty values from the response array
            for (int i = 1; i < msgs.length; i++){
                if (msgs[i].length() > 0){
                    fileHandler.append(msgs[i]);
                }
            }
    }

    /**
     * Replaces the contents of the file with the input list of transformations
     * @param msg The message, including the "replace" header and the list of transformations
     * @throws IOException If the fileHandler cannot write to the file
     */
    void operationReplace(String msg) throws IOException{
        fileHandler.purge();

        msg = msg.substring("replace ".length());
        String[] msgs = Pattern.compile("\\[|,|\\]").split(msg);

        //store nonempty values from the response array
        for (int i = 1; i < msgs.length; i++){
            if (msgs[i].length() > 0){
                fileHandler.append(msgs[i]);
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
    void operationTransformations(int beginIndex, int endIndex, SocketStreamContainer socketStreamContainer) throws IOException {
        sendUTF("bundle " + Arrays.toString(Arrays.copyOfRange(fileHandler.read(), beginIndex, endIndex)), socketStreamContainer);
    }

    public void shutdown() {
        isRunning = false;
        replicaReceiver.shutdown();
    }
}

