package network;

import util.BlockingQueue;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import static java.lang.Thread.yield;

/**
 * A threaded worker process to handle communications to and from a single client
 */
class ServerWorker implements Runnable{
    private Socket socket;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;
    private boolean isRunning;
    private BlockingQueue msgs;

    /**
     *
     * @param socket The socket which this worker should transmit and recieve from
     * @param msgs The blocking queue to deliver messages to
     * @throws IOException If the the socket is unable to produce input and/or output streams
     */
    ServerWorker(Socket socket, BlockingQueue msgs) throws IOException {
        this.socket = socket;
        this.msgs = msgs;
        dataOutputStream = new DataOutputStream(socket.getOutputStream());
        dataInputStream = new DataInputStream(socket.getInputStream());
    }

    @Override
    public void run() {
        //TODO determine if a client has disconnected and terminate thread
        try{

            isRunning = true;
            while (isRunning){

                //readUTF() blocks until success, so we must check before calling it to avoid waiting if a packet isnt ready
                if (dataInputStream.available() > 0){
                    String msg = dataInputStream.readUTF();

                    //TODO replace print statements with logging framework
                    System.out.println("Incoming Message from " + socket.getInetAddress() + ":" + socket.getPort() + " > " +  msg);

                    //add the recieved message to the queue for the server to broadcast later
                    msgs.add(msg);
                }

                yield();
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
    void sendUTF(String msg)throws IOException{
        dataOutputStream.writeUTF(msg);
        dataOutputStream.flush();
    }

    /**
     * check if the client has disconnected and sent an EOF
     * @return true if the client is still connected
     */
    boolean isConnected(){
        try {
            if (dataInputStream.readByte() == -1){
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }
}