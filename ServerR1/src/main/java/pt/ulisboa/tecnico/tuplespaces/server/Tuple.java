package pt.ulisboa.tecnico.tuplespaces.server;

public class Tuple{

    String tuple;
    int clientId;
    boolean lockedFlag;


    public Tuple(String tuple){
        this.tuple = tuple;
        this.clientId = -1;
        this.lockedFlag = false;
    }

    //make getters and setters

    public String getTuple(){
        return tuple;
    }

    public void setTuple(String tuple){
        this.tuple = tuple;
    }

    public boolean getLockedFlag(){
        return lockedFlag;
    }

    public int getClientId(){
        return clientId;
    }

    public synchronized void lockTuple(int clientId){
        System.out.println("lockTuple  " + tuple + "call - clientId: " + clientId);
        if (!lockedFlag){
            lockedFlag = true;
            this.clientId = clientId;
        }
    }

    public synchronized void unlockTuple(int clientId){
        System.out.println("unlockTuple  " + tuple + "call - clientId: " + clientId);
        if (lockedFlag && this.clientId==clientId){
            lockedFlag = false;
            this.clientId = -1;
        }
    }

}
