package network;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Handles the incoming and outgoing connections
 */
public class ReplicaMain implements Runnable{
    private String proxyIp;
    private int proxyPort;
    private boolean isRunning;

    public ReplicaMain(String ip, int port) {
        this.proxyIp = ip;
        this.proxyPort = port;
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
                    //TODO deliver message instead of discarding

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

    }

    public void shutdown() {
        isRunning = false;
    }
}
