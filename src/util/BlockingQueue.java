package util;

import java.util.Vector;

/**
 * a threadsafe queue for messages from clients on different threads to the main server class
 */
public class BlockingQueue{
    private Vector<String> msgs = new Vector<>();

    synchronized public void add(String s){
        msgs.addElement(s);
    }

    synchronized public String retrieve(){
        return msgs.remove(0);
    }

    public boolean available(){
        return (msgs.size() > 0);
    }
}