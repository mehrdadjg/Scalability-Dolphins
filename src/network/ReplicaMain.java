package network;


import handlers.FileHandler;
import util.Resources;
import util.SocketStreamContainer;
import util.TimeoutTimer;

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
    private int pingInterval = 1500;
    private Timer timeoutTimer = new Timer(true);
    private SocketStreamContainer socketStreamContainer;
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
        new Thread(replicaReceiver).start();
        isRunning = true;
        while (isRunning) {

            try{
                socketStreamContainer = new SocketStreamContainer(new Socket(proxyIp, proxyPort));
                proxyConnected = true;
                startTimer();
                System.out.println("Connected to proxy");

                //retrieve file contents
                requestUpdates(socketStreamContainer);


                while (isRunning) {
                    //readUTF() blocks until success, so we must check before calling it to avoid waiting if a packet isnt ready
                    if (socketStreamContainer.dataInputStream.available() > 0) {
                        String msg = socketStreamContainer.dataInputStream.readUTF();

                        System.out.println("Incoming Message from proxy > " + msg);

                        switch (msg.split(" ")[0]) {
                            case "add":
                            case "delete":
                                //Write the incoming update to file
                                fileHandler.append(msg);
                                break;
                            case "query_tn":
                                //reply with the current TN
                                sendUTF("tn " + (fileHandler.read().length), socketStreamContainer);
                                break;
                            case "transformations":
                                //prepare yourself
                                sendUTF(Arrays.toString(Arrays.copyOfRange(fileHandler.read(), Integer.parseInt(msg.split(" ")[1]), Integer.parseInt(msg.split(" ")[2]))), socketStreamContainer);
                                break;
                            default:
                                //Discard messages that are not recognized as part of the protocol
                                sendUTF("error:incorrect format", socketStreamContainer);
                                break;
                        }
                    }
                    if (!proxyConnected){
                        throw new IOException("proxy disconnected");
                    }
                }
                break;
            } catch (UnknownHostException e) {
                //Possible ip is not online, or ip is not valid
                e.printStackTrace();
            } catch (IOException e) {
                //Proxy is offline.
                System.out.println("Disconnected from proxy. attempting reconnect");
                //e.printStackTrace();
            } finally {
                socketStreamContainer.close();
            }
        }
        fileHandler.close();
    }


    private void startTimer(){
        timeoutTimer.cancel();
        timeoutTimer = new Timer(true);
        timeoutTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                timeoutTimer.cancel();
                try {
                    sendUTF("ping", socketStreamContainer);
                } catch (IOException e) {
                    proxyConnected = false;
                    //e.printStackTrace();
                }
            }
        }, pingInterval, pingInterval);
    }


    private void sendUTF(String msg, SocketStreamContainer socketStreamContainer) throws IOException{
        startTimer();
        socketStreamContainer.dataOutputStream.writeUTF(msg);
        socketStreamContainer.dataOutputStream.flush();
    }

    /**
     * Requests and downloads missed messages and saves them to file
     * @param proxy The SocketStreamContainer object which has the currently open streams to the proxy
     * @throws IOException if the streams are disconnected
     */
    private void requestUpdates(SocketStreamContainer proxy) throws IOException {

        //send an update request
        proxy.dataOutputStream.writeUTF("update");
        proxy.dataOutputStream.flush();

        //parse the IP addresses in the reply
        String[] replicaListString = Pattern.compile("\\[|,|\\]").split(proxy.dataInputStream.readUTF());

        //create connections to all of the IP addresses
        Vector<SocketStreamContainer> replicas = new Vector<>();
        for (String s : replicaListString){
            if ((s.compareTo(InetAddress.getLocalHost().getHostAddress()) != 0) && (s.length() > 0)){
                try {
                    replicas.add(new SocketStreamContainer(new Socket(s.split(":")[0],Resources.RECOVERYPORT)));
                } catch (IOException e){
                    //e.printStackTrace();
                }
            }
        }

        //query all replicas for their TNs
        for (SocketStreamContainer s : replicas){
            try{
                s.dataOutputStream.writeUTF("query_tn");
                s.dataOutputStream.flush();
            } catch (IOException e){
                //e.printStackTrace();
            }
        }

        //wait for reply
        TimeoutTimer timer = new TimeoutTimer();
        timer.startTimer(500);
        while (!timer.isTimeoutFlag()){Thread.yield();}

        //Find the maximum
        int TNold = fileHandler.read().length;
        int TNmax = -1;
        SocketStreamContainer master = null;
        for (SocketStreamContainer s : replicas){
            try{
                if (s.dataInputStream.available() > 0) {
                    int TNcurrent = Integer.parseInt(s.dataInputStream.readUTF().split(" ")[1]);
                    if (TNcurrent > TNmax && TNcurrent > TNold) {
                        master = s;
                        TNmax = TNcurrent;
                    }
                } else {
                    throw new IOException("Replica timeout");
                }
            } catch (IOException e){
                //e.printStackTrace();
            }
        }

        //request transformations from higher replica
        if (master != null){
            master.dataOutputStream.writeUTF("transformations " + TNold + " "+ TNmax);
            master.dataOutputStream.flush();
            //recieve and format the response
            String reply = master.dataInputStream.readUTF();
            if (reply.startsWith("bundle ")){
                operationBundle(reply);
            } else {
                master.dataOutputStream.writeUTF("error:incorrect format");
                master.dataOutputStream.flush();
            }
        }

        replicas.forEach(SocketStreamContainer::close);

        System.out.println("finished updating");
    }

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

    public void shutdown() {
        isRunning = false;
        replicaReceiver.shutdown();
    }
}

