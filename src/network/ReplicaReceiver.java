package network;

import handlers.FileHandler;
import util.Resources;
import util.SocketStreamContainer;
import util.TimeoutTimer;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A server socket handler to allow the replica to listen on a different channel for incoming update requests and queries
 */
class ReplicaReceiver implements Runnable{
    private int port;
    private boolean isRunning;
    private FileHandler fileHandler;
    private ReplicaMain replicaMain;

    /**
     * Initializes a replica receiver with a given fileHandler and port number to use for the ServerSocket
     * @param fileHandler The handler for the file that this receiver will be providing
     * @param port The port number to listen to for incoming connection requests
     */
    ReplicaReceiver(ReplicaMain replicaMain, FileHandler fileHandler, int port){
        this.replicaMain = replicaMain;
        this.fileHandler = fileHandler;
        this.port = port;
    }

    @Override
    public void run() {
        ExecutorService executorService = Executors.newCachedThreadPool();
        try(ServerSocket serverSocket = new ServerSocket(port)){
            serverSocket.setSoTimeout(10); //Set a timeout so that we dont block forever on ServerSocket.accept()

            isRunning = true;
            while (isRunning){
                try{
                    SocketStreamContainer socketStreamContainer = new SocketStreamContainer(serverSocket.accept());
                    executorService.submit(new ReplicaReceiverWorker(socketStreamContainer));
                    System.out.println("Accepted new recoverer");

                } catch (SocketTimeoutException s){
                    //s.printStackTrace();
                }
            }

        } catch (IOException e) {
            System.out.println("Failed to open serverSocket: " + port);
            e.printStackTrace();
        }
        executorService.shutdown();
    }

    /**
     * clears the isRunning flag which allows the thread to terminate it's loop and clean up
     */
    void shutdown(){
        isRunning = false;
    }

    private class ReplicaReceiverWorker implements Runnable{
        private SocketStreamContainer recoverer;
        private TimeoutTimer timer = new TimeoutTimer();
        private int timeout = Resources.TIMEOUT * 2;

        ReplicaReceiverWorker(SocketStreamContainer recoverer){
            this.recoverer = recoverer;
        }

        @Override
        public void run() {
            timer.reset();  //reset the timer in case it is already running
            do{
                //make sure that there are bytes to be read before attempting to read to avoid being blocked
                try {
                    if (recoverer.dataInputStream.available() > 0){
                        String msg = recoverer.dataInputStream.readUTF();

                        switch (msg.split(" ")[0]) {
                            case "query_tn" :
                                //reply with the current TN
                                recoverer.dataOutputStream.writeUTF("tn " + (fileHandler.read().length));
                                recoverer.dataOutputStream.flush();
                                break;
                            case "transformations" :
                                //prepare yourself
                                recoverer.dataOutputStream.writeUTF("bundle " + Arrays.toString(Arrays.copyOfRange(fileHandler.read(), Integer.parseInt(msg.split(" ")[1]), Integer.parseInt(msg.split(" ")[2]))));
                                recoverer.dataOutputStream.flush();
                                break;
                            case "bundle" :
                                replicaMain.operationBundle(msg);
                            case "replace" :
                                replicaMain.operationReplace(msg);
                            case "hash":
                                operationHash(msg.split(" ")[1], Integer.parseInt(msg.split(" ")[2]), recoverer);
                                break;
                            case "list":
                                String list = getDocumentList();
                                recoverer.dataOutputStream.writeUTF("documents [" + list + "]");
                                recoverer.dataOutputStream.flush();
                                break;
                            default:
                                //Discard messages that are not recognized as part of the protocol
                                recoverer.dataOutputStream.writeUTF("error:incorrect format");
                                recoverer.dataOutputStream.flush();
                                break;
                        }
                        timer.startTimer(timeout); //start the timer again every time a message is received
                    }
                } catch (IOException e) {
                    //e.printStackTrace();
                }
            } while (!timer.isTimeoutFlag());   //if no messages are received, then time out and close the connection

        }


        private String getDocumentList() {
            try {
                File location = new File(".");
                String output = "";

                File[] files = location.listFiles();

                for(int i = 0; i < files.length; i++) {
                    if(files[i].isFile()) {
                        if(files[i].getName().endsWith(".txt")) {
                            output += (files[i].getName().substring(0, files[i].getName().length() - 4)) + ",";
                        }
                    }
                }

                return output.substring(0, output.length() - 1);
            } catch(Exception e) {
                return "";
            }
        }

		private void operationHash(String fileName, int length, SocketStreamContainer socketStreamContainer) throws IOException{
            String reply = "signature ";                        //message header
            reply += fileHandler.getFileName() + " ";           //filename
            reply += length + " ";                              //number of transformations in the hash
            reply += fileHandler.hash(length);                  //hash of contents to the specified length
            recoverer.dataOutputStream.writeUTF(reply);
            recoverer.dataOutputStream.flush();
        }
    }
}

