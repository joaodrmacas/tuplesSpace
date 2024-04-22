package pt.ulisboa.tecnico.tuplespaces.server.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import pt.ulisboa.tecnico.tuplespaces.server.SeqInt;
import pt.ulisboa.tecnico.tuplespaces.server.Tuple;

import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;

public class ServerState {

  private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);
  private ArrayList<Tuple> tuples;
  private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
  private final Lock writeLock = readWriteLock.writeLock();
  private final Lock readLock = readWriteLock.readLock();

  private final Lock mapLock = new ReentrantLock();
  private final Lock readReqLock = new ReentrantLock();
  private Condition readCond = readReqLock.newCondition();
  private final Lock reqCondLock = new ReentrantLock();
  private Condition reqCond = reqCondLock.newCondition();
  private int reqCounter = 1;

  private final HashMap<String, PriorityQueue<SeqInt>> reqMap = new HashMap<>();

  private static void debug(String debugMessage) {
    if (DEBUG_FLAG)
      System.err.println(debugMessage);
  }

  private boolean verifyPattern(String pattern) {
    return pattern.substring(0, 1).equals("<") && pattern.endsWith(">");
  }

  private boolean isCurrentRequest(int reqInt){
    debug("isCurrentRequest - reqInt: " + reqInt + " reqCounter: " + this.reqCounter);
    return this.reqCounter == reqInt;
  }

  public ServerState() {
    this.tuples = new ArrayList<Tuple>();
  }

  public int put(String tuple, Integer reqInt) {
    debug("put call - tuple: " + tuple);

    if (!verifyPattern(tuple)) {
      debug("put call failed - invalid pattern.");
      return -1;
    }

    reqCondLock.lock();
    try{
      while(!isCurrentRequest(reqInt)){
        reqCond.await();
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    }


    debug("Adding tuple to tuple space..");
    writeLock.lock();
    try {
      tuples.add(new Tuple(tuple));
    } finally {
      writeLock.unlock();
    }

    SeqInt queue_lock = getOldestMatchingTake(tuple);

    if (queue_lock != null) {
      debug("There are take requests waiting..");
      queue_lock.getIntLock().lock();
      try {
        queue_lock.getIntCond().signal();
      }
      finally {
        queue_lock.getIntLock().unlock();
      }
    }
    else {
      this.reqCounter++;
    }

    readReqLock.lock();
    try {
      readCond.signalAll();
    } finally {
      readReqLock.unlock();
    }

    reqCond.signalAll();
    reqCondLock.unlock();

    return 0;
  }

  private SeqInt getOldestMatchingTake(String tuple){
    SeqInt curr_seqInnt = null;
    mapLock.lock();
    try {
      for (String key : reqMap.keySet()) {
        if (curr_seqInnt==null && tuple.matches(key)){
          curr_seqInnt = reqMap.get(key).peek();
        }
        
        else if (tuple.matches(key) && reqMap.get(key).peek().getSeqNumber() <= curr_seqInnt.getSeqNumber()){
          curr_seqInnt = reqMap.get(key).peek();
        }
      }
    } finally {
      mapLock.unlock();
    }
    return curr_seqInnt;
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
      readReqLock.lock();
      try {
        debug("Waiting for tuple with pattern: " + pattern);
        readCond.await();

      } catch (InterruptedException e) {
        e.printStackTrace();
      } finally {
        readReqLock.unlock();
      }
    }
    return tuple.getTuple();
  }

  
  public String take(String pattern, Integer reqInt) {
    Tuple tuple;
    debug("take call - pattern: " + pattern);
    
    if (!verifyPattern(pattern)) {
      debug("take call failed - invalid pattern.");
      return null;
    }

    reqCondLock.lock();
    try{      
      while(!isCurrentRequest(reqInt)){
        reqCond.await();
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    } finally {
      reqCondLock.unlock();
    }
    
    tuple = getMatchingTuple(pattern);

    if (tuple == null) {
      PriorityQueue<SeqInt> seqIntQueue=null;
      SeqInt newInt=null;

      //Adicionar o pedido à fila caso não haja o padrao no espaco de tuplos
      mapLock.lock();
      try {
        seqIntQueue = reqMap.get(pattern);
        if (seqIntQueue == null){
          seqIntQueue = new PriorityQueue<>();
          reqMap.put(pattern, seqIntQueue);
        }
        newInt = new SeqInt(reqInt);
        seqIntQueue.add(newInt);
      } finally {
        mapLock.unlock();
      }

      //Incrementar counter para o server continuar a processar pedidos
      reqCondLock.lock();
      try {
        debug("Waiting for tuple with pattern: " + pattern);
        this.reqCounter++;
      }
      finally {
        reqCondLock.unlock();
      }

      
      //Esperar pelo sinal do put e retirar do mapa
      newInt.getIntLock().lock();
      try{
        debug("waiting for pattern ["+ pattern + "] to be unlocked");
        newInt.getIntCond().await();
        mapLock.lock();
        try{
          seqIntQueue.poll();
          if (seqIntQueue.isEmpty()){
            reqMap.remove(pattern);
          }
          tuple = getMatchingTuple(pattern);
        } finally {
          mapLock.unlock();
        }
      } catch (Exception e){
        e.printStackTrace();
      } 
      finally {
        newInt.getIntLock().unlock();
      }
      
    }
    //Remover o tuple do espaco de tuplos
    writeLock.lock();
    try{
      this.tuples.remove(tuple);
    } finally {
      writeLock.unlock();
    }

    reqCondLock.lock();
    try {
      debug("Waiting for tuple with pattern: " + pattern);
      this.reqCounter++;
    }
    finally {
      reqCondLock.unlock();
    }
    
    return tuple.getTuple();
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
