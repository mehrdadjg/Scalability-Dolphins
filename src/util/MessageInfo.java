package util;

import network.ProxyWorker;

public class MessageInfo {
    public ProxyWorker sender;
    public String update;

    MessageInfo(String documentUpdate, ProxyWorker sender){
        this.update = documentUpdate;
        this.sender = sender;
    }
}
