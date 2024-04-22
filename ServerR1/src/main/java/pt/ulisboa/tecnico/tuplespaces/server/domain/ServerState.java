package pt.ulisboa.tecnico.tuplespaces.server.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import pt.ulisboa.tecnico.tuplespaces.server.Tuple;

import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;

public class ServerState {

  private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);
  private ArrayList<Tuple> tuples;
  private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
  private final Lock writeLock = readWriteLock.writeLock();
  private final Lock readLock = readWriteLock.readLock();

  private final Lock condLock = new ReentrantLock();
  private Condition cond = condLock.newCondition();

  private static void debug(String debugMessage) {
    if (DEBUG_FLAG)
      System.err.println(debugMessage);
  }

  private boolean verifyPattern(String pattern) {
    return pattern.substring(0, 1).equals("<") && pattern.endsWith(">");
  }

  public ServerState() {
    this.tuples = new ArrayList<Tuple>();
  }

  public int put(String tuple) {
    debug("put call - tuple: " + tuple);

    if (!verifyPattern(tuple)) {
      debug("put call failed - invalid pattern.");
      return -1;
    }

    writeLock.lock();
    condLock.lock();
    try {
      tuples.add(new Tuple(tuple));
      debug("Wake waiting threads");
      cond.signalAll();
    } finally {
      condLock.unlock();
      writeLock.unlock();
    }

    return 0;
  }

  private Tuple getMatchingTuple(String pattern) {

    readLock.lock();
    try {
      for (Tuple tuple : this.tuples) {
        if (tuple.getTuple().matches(pattern)) {
          return tuple;
        }
      }
    } finally {
      readLock.unlock();
    }
    return null;
  }

  public String read(String pattern) {
    Tuple tuple;
    debug("read call - pattern: " + pattern);

    if (!verifyPattern(pattern)) {
      debug("read call failed - invalid pattern.");
      return null;
    }

    while ((tuple = getMatchingTuple(pattern)) == null) {
      condLock.lock();
      try {
        debug("Waiting for tuple with pattern: " + pattern);
        cond.await();
      } catch (InterruptedException e) {
        e.printStackTrace();
      } finally {
        condLock.unlock();
      }
    }
    return tuple.getTuple();
  }

  public Set<String> takePhase1(String pattern, int clientId) {
    debug("takePhase1 call - pattern: " + pattern + "clientId: " + clientId);

    if (!verifyPattern(pattern)) {
      debug("take call failed - invalid pattern.");
      return null;
    }

    Set<String> matchingTuples = new HashSet<String>();
    boolean flag = false;
    while(!flag){
        readLock.lock();
        try {
          for (Tuple tuple : this.tuples) {
            if (tuple.getTuple().matches(pattern)) {
              flag = true;
              if(!tuple.getLockedFlag() && matchingTuples.add(tuple.getTuple())){
                debug("Locking tuple " + tuple.getTuple() + "with clientId: " + clientId);
                tuple.lockTuple(clientId);
              }
              else if (tuple.getLockedFlag() && tuple.getClientId()!=clientId){
                debug("Tuple " + tuple.getTuple() + " already locked by diferent client with clientId: " + tuple.getClientId());
                takePhase1Release(clientId);
                break;
              }
            }
          }
        } finally {
            readLock.unlock();
          }

        if(!flag){
          condLock.lock();
          try {
            debug("Waiting for tuple with pattern: " + pattern);
            cond.await();
          } catch (InterruptedException e) {
            e.printStackTrace();
          } finally {
            condLock.unlock();
          }
        }
      }

    debug("matchingTuples: " + matchingTuples.toString());
    return matchingTuples;
  }


  public void takePhase1Release(int clientId) {
    debug("takePhase1Release call - clientId: " + clientId);
    for (Tuple tuple : this.tuples) {
      if (tuple.getLockedFlag() && tuple.getClientId() == clientId) {
        debug("Unlocking tuple " + tuple.getTuple() + "with clientId: " + clientId);
        tuple.unlockTuple(clientId);
      }
    }
  }


  public int takePhase2(String tuple, int clientId){
    Tuple to_remove = null;
    debug("takePhase2 call - clientId: " + clientId);
    writeLock.lock();
    try{
      for (Tuple tup: this.tuples){
        if(tup.getClientId()==clientId){
          tup.unlockTuple(clientId);
          if (tup.getTuple().equals(tuple)){
            to_remove = tup;
          }
        }
      }
      if(to_remove != null){
        this.tuples.remove(to_remove);
      }
      else {
        return -1;
      }
    } finally {
      writeLock.unlock();
    }

    return 0;
  }

  public List<String> getTupleSpacesState() {
    debug("getTupleSpacesState call");
    readLock.lock();
    List<String> tup;
    try {
      tup = new ArrayList<String>();
      for (Tuple tuple : this.tuples) {
        tup.add(tuple.getTuple());
      }
    } finally {
      readLock.unlock();
    }
    return tup;
  }
}
