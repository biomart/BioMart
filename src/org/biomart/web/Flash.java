package org.biomart.web;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


public class Flash implements Serializable {
    private List<String> queue = new ArrayList<String>();
    public synchronized String getMessage() {
        if (queue.isEmpty()) return "";
        return queue.remove(0);
    }
    public synchronized void addMessage(String msg) {
        queue.add(msg);
    }
    public synchronized int getSize() {
        return queue.size();
    }
    public synchronized List<String> getMessages() {
        return queue;
    }
}