package launcher;

import IO.ClientView;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;

/**
 * INCOMPLETE
 */
public class GraphicalClient {
    public static void main(String[] args){
        ClientView clientView = new ClientView("Demo 7");

        try {
            SwingUtilities.invokeAndWait(() -> clientView.create());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
