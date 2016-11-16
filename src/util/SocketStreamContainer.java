package util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * INCOMPLETE
 * Created by Arnold on 2016-11-15.
 */
public class SocketStreamContainer{
    Socket socket;
    public DataInputStream dataInputStream;
    public DataOutputStream dataOutputStream;

    public SocketStreamContainer(Socket socket) throws IOException {
        this.socket = socket;
        dataInputStream = new DataInputStream(socket.getInputStream());
        dataOutputStream = new DataOutputStream(socket.getOutputStream());
    }

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
