package network;

import handlers.FileHandler;
import launcher.Proxy;

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
    private final static int port = Proxy.replicaPort;
    private boolean isRunning;
    private int timeout = 1000;
    private FileHandler fileHandler;

    public ReplicaReceiver(FileHandler fileHandler){
        this.fileHandler = fileHandler;
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

                } catch (SocketTimeoutException s){
                    //s.printStackTrace();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void shutdown(){
        isRunning = false;
    }
}
