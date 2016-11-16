package network;

import handlers.FileHandler;
import util.SocketStreamContainer;
import util.TimeoutTimer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Arrays;

/**
 * A server socket handler to allow the replica to listen on a different channel for incoming update requests and queries
 */
public class ReplicaReceiver implements Runnable{
    private int port;
    private boolean isRunning;
    private int timeout = 10000;
    private FileHandler fileHandler;
    TimeoutTimer timer = new TimeoutTimer();

    /**
     * Initializes a replica receiver with a given fileHandler and port number to use for the ServerSocket
     * @param fileHandler The handler for the file that this receiver will be providing
     * @param port The port number to listen to for incoming connection requests
     */
    public ReplicaReceiver(FileHandler fileHandler, int port){
        this.fileHandler = fileHandler;
        this.port = port;
    }

    @Override
    public void run() {
        try(ServerSocket serverSocket = new ServerSocket(port)){
            serverSocket.setSoTimeout(10); //Set a timeout so that we dont block forever on ServerSocket.accept()

            isRunning = true;
            while (isRunning){
                //TODO create replica worker to allow multiple recoveries at the same time
                try(SocketStreamContainer socketStreamContainer = new SocketStreamContainer(serverSocket.accept());){

                    timer.reset();  //reset the timer in case it is already running
                    do{
                        //make sure that there are bytes to be read before attempting to read to avoid being blocked
                        if (socketStreamContainer.dataInputStream.available() > 0){
                            String msg = socketStreamContainer.dataInputStream.readUTF();

                            switch (msg.split(" ")[0]) {
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
                            timer.startTimer(timeout); //start the timer again every time a message is received
                        }
                    } while (!timer.isTimeoutFlag());   //if no messages are received, then time out and close the connection

                } catch (SocketTimeoutException s){
                    //s.printStackTrace();
                }
            }

        } catch (IOException e) {
            System.out.println("Failed to open serverSocket: " + port);
            e.printStackTrace();
        }
    }

    /**
     * clears the isRunning flag which allows the thread to terminate it's loop and clean up
     */
    public void shutdown(){
        //TODO override timer when shutdown is requested to avoid waiting for timeouts
        isRunning = false;
    }
}
