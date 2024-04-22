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
        ArrayList<R> responses = new ArrayList<R>(collectedResponses);
        return responses;
    }

    synchronized public void waitUntilNReceived(int n) throws InterruptedException {
        while (collectedResponses.size() < n){
            wait();
        }
        for (int i=0; i<n; i++){
            if (collectedResponses.get(i) == null){
                throw new RuntimeException("ResponseCollector: null response received");
            }
        }
    }
}