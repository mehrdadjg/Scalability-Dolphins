package network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * A threaded worker process to handle communications to and from a single client
 */
class ServerWorker implements Runnable{
    private Socket socket;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;
    private boolean isRunning;

    /**
     *
     * @param socket The socket which this worker should transmit and recieve from
     * @throws IOException If the the socket is unable to produce input and/or output streams
     */
    ServerWorker(Socket socket) throws IOException {
        this.socket = socket;
        dataOutputStream = new DataOutputStream(socket.getOutputStream());
        dataInputStream = new DataInputStream(socket.getInputStream());
    }

    @Override
    public void run() {
        try{

            isRunning = true;
            while (isRunning){
                if (dataInputStream.available() > 0){
                    //TODO deliver the message instead of discarding
                    dataInputStream.readUTF();
                }
            }

            //release the resources before the thread terminates
            dataInputStream.close();
            dataOutputStream.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * clears the isRunning flag which allows the thread to terminate it's loop and clean up
     */
    void shutdown(){
        isRunning = false;
    }

    /**
     * Attempts to send a string to the connected client
     * @param msg The string to be transmitted. It does not need to be null terminated.
     * @throws IOException if the dataOutputStream fails to send
     */
    public void sendUTF(String msg)throws IOException{
        dataOutputStream.writeUTF(msg);
        dataOutputStream.flush();
    }
}