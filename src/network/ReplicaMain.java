package network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;

import handlers.FileHandler;

/**
 * Handles the incoming and outgoing connections
 */
public class ReplicaMain implements Runnable{
    private String proxyIp;
    private int proxyPort;
    private boolean isRunning;
    private FileHandler fileHandler;

    public ReplicaMain(String ip, int port) throws IOException {
        this.proxyIp = ip;
        this.proxyPort = port;
        fileHandler = new FileHandler("file.txt");
    }

    /**
     * opens a socket to the proxy, receives messages in a loop
     */
    @Override
    public void run() {
        try(Socket socket = new Socket(proxyIp, proxyPort); DataInputStream dataInputStream = new DataInputStream(socket.getInputStream()); DataOutputStream dataOutputStream = new DataOutputStream((socket.getOutputStream()))) {
            System.out.println("Connected to proxy");
            isRunning = true;
            while (isRunning) {
                //readUTF() blocks until success, so we must check before calling it to avoid waiting if a packet isnt ready
                if (dataInputStream.available() > 0) {
                    String msg = dataInputStream.readUTF();

                    switch (msg.split(" ")[0]){
                        case "add"  : case "delete" :
                            //Write the incoming update to file
                            fileHandler.append(msg);
                            break;
                        case "query_tn" :
                            //reply with the current TN
                            dataOutputStream.writeUTF("tn " + (fileHandler.read().length - 1));
                            break;
                        case "transformations" :
                            //prepare yourself
                            dataOutputStream.writeUTF(Arrays.toString(Arrays.copyOfRange(fileHandler.read(), Integer.parseInt(msg.split(" ")[1]), Integer.parseInt(msg.split(" ")[2]))));
                            break;
                        default:
                            //Discard messages that are not recognized as part of the protocol
                            //TODO reply with <Error> according to protocol
                            System.out.println("Unknown message type recieved. Discarding > " + msg);
                            break;
                    }



                    //TODO replace print statements with logging framework
                    System.out.println("Incoming Message from proxy > " + msg);
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

    public void shutdown() {
        isRunning = false;
    }
}
