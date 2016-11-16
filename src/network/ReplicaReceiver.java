package network;

import handlers.FileHandler;
import util.TimeoutTimer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Arrays;

/**
 * INCOMPLETE
 * Created by Arnold on 2016-11-15.
 */
public class ReplicaReceiver implements Runnable{
    private int port;
    private boolean isRunning;
    private int timeout = 10000;
    private FileHandler fileHandler;
    TimeoutTimer timer = new TimeoutTimer();

    public ReplicaReceiver(FileHandler fileHandler, int port){
        this.fileHandler = fileHandler;
        this.port = port;
    }

    @Override
    public void run() {
        try(ServerSocket serverSocket = new ServerSocket(port)){
            serverSocket.setSoTimeout(10);
            isRunning = true;

            while (isRunning){
                try(Socket socket = serverSocket.accept();){
                    DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                    DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());

                    do{
                        if (dataInputStream.available() > 0){
                            String msg = dataInputStream.readUTF();

                            switch (msg.split(" ")[0]) {
                                case "query_tn" :
                                    //reply with the current TN
                                    dataOutputStream.writeUTF("tn " + (fileHandler.read().length));
                                    break;
                                case "transformations" :
                                    //prepare yourself
                                    dataOutputStream.writeUTF(Arrays.toString(Arrays.copyOfRange(fileHandler.read(), Integer.parseInt(msg.split(" ")[1]), Integer.parseInt(msg.split(" ")[2]))));
                                    break;
                                default:
                                    //Discard messages that are not recognized as part of the protocol
                                    dataOutputStream.writeUTF("error:incorrect format");
                                    break;
                            }
                            timer.cancel();
                            timer.startTimer(timeout);
                        }
                    } while (!timer.isTimeoutFlag());

                    System.out.println("recoverer timed out");

                } catch (SocketTimeoutException s){
                    //s.printStackTrace();
                }
            }

        } catch (IOException e) {
            System.out.println("Failed to open serverSocket: " + port);
            e.printStackTrace();
        }
    }

    public void shutdown(){
        isRunning = false;
    }
}
