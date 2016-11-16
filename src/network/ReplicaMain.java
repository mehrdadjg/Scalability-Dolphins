package network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.regex.Pattern;

import handlers.FileHandler;
import util.SocketStreamContainer;
import util.TimeoutTimer;

/**
 * Handles the incoming and outgoing connections
 */
public class ReplicaMain implements Runnable{
    private String proxyIp;
    private int proxyPort;
    private boolean isRunning;
    public boolean recoveryMode = false;
    private FileHandler fileHandler = new FileHandler("file.txt");
    private ReplicaReceiver replicaReceiver = new ReplicaReceiver(fileHandler, replicaPort);
    static int replicaPort = 880;

    public ReplicaMain(String ip, int port) throws IOException {
        this.proxyIp = ip;
        this.proxyPort = port;
    }

    /**
     * opens a socket to the proxy, receives messages in a loop
     */
    @Override
    public void run() {

        try(SocketStreamContainer socketStreamContainer = new SocketStreamContainer(new Socket(proxyIp, proxyPort))) {
            System.out.println("Connected to proxy");

            //retrieve file contents
            if (recoveryMode){
                requestUpdates(socketStreamContainer);
            }

            //Launch the receiver thread which will service incoming update requests
            new Thread(replicaReceiver).start();

            isRunning = true;
            while (isRunning) {
                //readUTF() blocks until success, so we must check before calling it to avoid waiting if a packet isnt ready
                if (socketStreamContainer.dataInputStream.available() > 0) {
                    String msg = socketStreamContainer.dataInputStream.readUTF();

                    //TODO replace print statements with logging framework
                    System.out.println("Incoming Message from proxy > " + msg);

                    switch (msg.split(" ")[0]){
                        case "add"  : case "delete" :
                            //Write the incoming update to file
                            fileHandler.append(msg);
                            break;
                        case "query_tn" :
                            //reply with the current TN
                            socketStreamContainer.dataOutputStream.writeUTF("tn " + (fileHandler.read().length));
                            socketStreamContainer.dataOutputStream.flush();
                            break;
                        case "transformations" :
                            //prepare yourself
                            socketStreamContainer.dataOutputStream.writeUTF(Arrays.toString(Arrays.copyOfRange(fileHandler.read(), Integer.parseInt(msg.split(" ")[1]), Integer.parseInt(msg.split(" ")[2]))));
                            socketStreamContainer.dataOutputStream.flush();
                            break;
                        default:
                            //Discard messages that are not recognized as part of the protocol
                            socketStreamContainer.dataOutputStream.writeUTF("error:incorrect format");
                            socketStreamContainer.dataOutputStream.flush();
                            break;
                    }
                }
            }
        } catch (UnknownHostException e) {
            //Possible ip is not online, or ip is not valid
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        fileHandler.close();
    }


    /**
     * Requests and downloads missed messages and saves them to file
     * @param proxy The SocketStreamContainer object which has the currently open streams to the proxy
     * @throws IOException if the streams are disconnected
     */
    private void requestUpdates(SocketStreamContainer proxy) throws IOException {
        //TODO compare received messages to existing ones to avoid accidental duplication

        //send an update request
        proxy.dataOutputStream.writeUTF("update");
        proxy.dataOutputStream.flush();

        //parse the IP addresses in the reply
        String[] replicaListString = Pattern.compile("\\[|,|\\]").split(proxy.dataInputStream.readUTF());

        //create connections to all of the IP addresses
        Vector<SocketStreamContainer> replicas = new Vector<>();
        for (String s : replicaListString){
            try {
                replicas.add(new SocketStreamContainer(new Socket(s.split(":")[0],replicaPort)));
            } catch (IOException e){
                //e.printStackTrace();
            }
        }

        //query all replicas for their TNs
        for (SocketStreamContainer s : replicas){
            try{
                s.dataOutputStream.writeUTF("query_tn");
                s.dataOutputStream.flush();
            } catch (IOException e){
                s.close();
                replicas.remove(s);
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
                s.close();
                replicas.remove(s);
            }
        }

        //request transformations from higher replica
        if (master != null){
            master.dataOutputStream.writeUTF("transformations " + TNold + " "+ TNmax);
            master.dataOutputStream.flush();
            //recieve and format the response
            String[] msgs = Pattern.compile("\\[|,|\\]").split(master.dataInputStream.readUTF());

            //store nonempty values from the response array
            for (int i = 1; i < msgs.length; i++){
                if (msgs[i].length() > 0){
                    fileHandler.append(msgs[i]);
                }
            }
        }

        System.out.println("finished updating");
    }

    public void shutdown() {
        isRunning = false;
        replicaReceiver.shutdown();
    }
}

