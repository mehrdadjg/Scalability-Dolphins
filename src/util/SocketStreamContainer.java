package util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * An Autocloseable container for a socket object and it's associated DataInputStream and DataOutputStream
 */
public class SocketStreamContainer implements AutoCloseable {
    Socket socket;
    public DataInputStream dataInputStream;
    public DataOutputStream dataOutputStream;

    /**
     * Initialize this object with an existing socket
     * @param socket The socket to send and receive data on
     * @throws IOException If the socket is disconnected during initialization
     */
    public SocketStreamContainer(Socket socket) throws IOException {
        this.socket = socket;
        dataInputStream = new DataInputStream(socket.getInputStream());
        dataOutputStream = new DataOutputStream(socket.getOutputStream());
    }

    @Override
    public void close(){
        try {
            dataOutputStream.close();
        } catch (IOException e) {
            //e.printStackTrace();
        }

        try {
            dataInputStream.close();
        } catch (IOException e) {
            //e.printStackTrace();
        }

        try {
            socket.close();
        } catch (IOException e) {
            //e.printStackTrace();
        }
    }
}
