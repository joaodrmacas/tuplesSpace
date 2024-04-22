package pt.ulisboa.tecnico.tuplespaces.server;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SeqInt implements Comparable<SeqInt> {

    private final Lock intLock = new ReentrantLock();
    private Condition intCond = intLock.newCondition();
    private int reqInt;

    public SeqInt(int reqInt) {
        this.reqInt = reqInt;
    }

    public Lock getIntLock() {
        return intLock;
    }

    public Condition getIntCond() {
        return intCond;
    }

    public Integer getSeqNumber() {
        return this.reqInt;
    }

    @Override
    public int compareTo(SeqInt other) {
        return Integer.compare(this.reqInt, other.reqInt);
    }
}
