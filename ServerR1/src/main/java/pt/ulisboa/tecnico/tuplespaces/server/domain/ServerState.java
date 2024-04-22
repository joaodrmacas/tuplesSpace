package pt.ulisboa.tecnico.tuplespaces.server.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;

public class ServerState {

  private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);
  private ArrayList<String> tuples;
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
    this.tuples = new ArrayList<String>();
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
      tuples.add(tuple);
      debug("Wake waiting threads");
      cond.signalAll();
    } finally {
      condLock.unlock();
      writeLock.unlock();
    }

    return 0;
  }

  private String getMatchingTuple(String pattern) {

    readLock.lock();
    try {
      for (String tuple : this.tuples) {
        if (tuple.matches(pattern)) {
          return tuple;
        }
      }
    } finally {
      readLock.unlock();
    }
    return null;
  }

  public String read(String pattern) {
    String tuple;
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
    return tuple;
  }

  public String take(String pattern) {
    String tuple;
    debug("take call - pattern: " + pattern);

    if (!verifyPattern(pattern)) {
      debug("take call failed - invalid pattern.");
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
    writeLock.lock();
    try {
      debug("Removing tuple " + tuple + " from tupleSpace");
      tuples.remove(tuple);
    } finally {
      writeLock.unlock();
    }
    return tuple;
  }

  public List<String> getTupleSpacesState() {
    debug("getTupleSpacesState call");
    readLock.lock();
    List<String> tup;
    try {
      tup = new ArrayList<String>(tuples);
    } finally {
      readLock.unlock();
    }
    return tup;
  }
}
