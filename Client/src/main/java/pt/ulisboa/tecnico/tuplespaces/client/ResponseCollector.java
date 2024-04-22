package pt.ulisboa.tecnico.tuplespaces.client;

import java.util.ArrayList;


public class ResponseCollector<R> {
    ArrayList<R> collectedResponses;

    public ResponseCollector() {
        collectedResponses = new ArrayList<R>();
    }

    synchronized public void addResponse(R r) {
        collectedResponses.add(r);
        notifyAll();
    }

    synchronized public void clearResponses() {
        collectedResponses.clear();
        notifyAll();
    }

    synchronized public ArrayList<R> getResponses() {
        return collectedResponses;
    }

    synchronized public void waitUntilNReceived(int n) throws InterruptedException {
        while (collectedResponses.size() < n) 
            wait();
    }
}