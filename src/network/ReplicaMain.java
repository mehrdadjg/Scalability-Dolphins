package network;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

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
        try(Socket socket = new Socket(proxyIp, proxyPort); DataInputStream dataInputStream = new DataInputStream(socket.getInputStream())) {
            System.out.println("Connected to proxy");
            isRunning = true;
            while (isRunning) {
                //readUTF() blocks until success, so we must check before calling it to avoid waiting if a packet isnt ready
                if (dataInputStream.available() > 0) {
                    String msg = dataInputStream.readUTF();
                    //Write the incoming update to file
                    fileHandler.append(msg);

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
