package util;

import java.util.Vector;
import network.ProxyWorker;

/**
 * a threadsafe queue for messages from clients on different threads to the main server class
 */
public class BlockingQueue{
    private Vector<MessageInfo> msgs = new Vector<>();

    synchronized public void add(String update, ProxyWorker sender){
        msgs.addElement(new MessageInfo(update, sender));
    }

    synchronized public MessageInfo retrieve(){
        return msgs.remove(0);
    }

    public boolean available(){
        return (msgs.size() > 0);
    }
}
