package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.NameServer.contract.NameServer.LookupRequest;
import pt.ulisboa.tecnico.NameServer.contract.NameServer.LookupResponse;
import pt.ulisboa.tecnico.NameServer.contract.NameServiceGrpc;
import pt.ulisboa.tecnico.tuplespaces.client.ResponseCollector;
import pt.ulisboa.tecnico.tuplespaces.client.observers.ResponseObserver;
import pt.ulisboa.tecnico.tuplespaces.client.observers.TakePhase1Observer;
import pt.ulisboa.tecnico.tuplespaces.client.util.OrderedDelayer;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaGrpc;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.GetTupleSpacesStateRequest;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.GetTupleSpacesStateResponse;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.PutRequest;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.PutResponse;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.ReadRequest;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.ReadResponse;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.TakePhase1ReleaseRequest;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.TakePhase1ReleaseResponse;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.TakePhase1Request;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.TakePhase1Response;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.TakePhase2Request;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.TakePhase2Response;

public class ClientService {

    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);
    private ManagedChannel[] channels;
    private TupleSpacesReplicaGrpc.TupleSpacesReplicaStub[] stubs;

    private Integer numServers;
    private int clientId;

    private final int NAME_SERVER_PORT = 5001;

    OrderedDelayer delayer;

    private static void debug(String debugMessage) {
        if (DEBUG_FLAG)
            System.err.println(debugMessage);
        }

    public ClientService(){

        Random random = new Random(System.currentTimeMillis());
        this.clientId = random.nextInt(1000000);

        List<String> servers = lookup(NAME_SERVER_PORT,"");
        this.numServers = servers.size();
		this.stubs = new TupleSpacesReplicaGrpc.TupleSpacesReplicaStub[this.numServers];
        this.channels = new ManagedChannel[this.numServers];

        delayer = new OrderedDelayer(this.numServers);


        while (servers == null) {
            System.out.println("There are no servers available.");
            waitForEnterKeyPress();
            servers = lookup(NAME_SERVER_PORT,"");
        }

        debug("Servers available:");
        for (String server : servers) {
            debug(server);
        }


        int i = 0;
        debug("Creating stubs for all available servers.");
		for (String server: servers){
			this.channels[i] = ManagedChannelBuilder.forTarget(server).usePlaintext().build();
            debug("creating server");
			this.stubs[i] = TupleSpacesReplicaGrpc.newStub(this.channels[i]);
            i++;
        }

    }

    /* This method allows the command processor to set the request delay assigned to a given server */
    public void setDelay(int id, int delay) {
        delayer.setDelay(id, delay);
    }

    private int getIndexFromQualifier(String qualifier){
        if (qualifier==null || qualifier.length()!=1) return -1;
        switch (qualifier) {
            case "A":
                return 0;
            case "B":
                return 1;
            case "C":
                return 2;
            default:
                return -1;
        }
    }

    private void waitForEnterKeyPress() {
        System.out.println("Press Enter to retry...");
        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();
    }

    public void put(String tuple) {
        debug("Call put: tuple - " + tuple);
        try {
            ResponseCollector<PutResponse> c = new ResponseCollector<PutResponse>();
            for (Integer id: delayer){
                stubs[id].put(PutRequest.newBuilder().setNewTuple(tuple).build(), new ResponseObserver<PutResponse>(c));
            }
            c.waitUntilNReceived(this.numServers);
            System.out.println("OK\n");
        } catch (StatusRuntimeException e) {
            System.out.println("Failed to reach server.");
            shutdown();
            System.exit(1);
        } catch (InterruptedException e) {
            System.out.println("Failed to receive answers to put request.");
            shutdown();
            System.exit(1);
        }
    }

    public void read(String pattern) {
        debug("Call read: pattern - " + pattern);
        try {
            ResponseCollector<ReadResponse> c = new ResponseCollector<ReadResponse>();
            for(Integer id: delayer){
                stubs[id].read(ReadRequest.newBuilder().setSearchPattern(pattern).build(), new ResponseObserver<ReadResponse>(c));
            }
            c.waitUntilNReceived(1);
            System.out.println("OK\n" + c.getResponses().get(0).getResult() + "\n");
        } catch (StatusRuntimeException e) {
            System.out.println("Failed to reach server.");
            shutdown();
            System.exit(1);
        } catch (InterruptedException e) {
            System.out.println("Failed to receive answers to read request.");
            shutdown();
            System.exit(1);
        }
    }

    public void take(String pattern) {
        String tuple = takePhase1(pattern);
        takePhase2(tuple);
    }

    private String takePhase1(String pattern){
        int accepted;
        String tuple = "";
        ArrayList<TakePhase1Observer> observers = new ArrayList<TakePhase1Observer>();

        ResponseCollector<TakePhase1Response> c = new ResponseCollector<TakePhase1Response>();
        for (int i=0; i<this.numServers; i++){
            observers.add(new TakePhase1Observer(c));
        }
        
        boolean firstIt = true;
        while (tuple.equals("")){
            debug("Call take: pattern - " + pattern);
            accepted = 0;
            try {
                int i = 0;
                c.clearResponses();
                if (firstIt){
                    firstIt = false;
                    for (Integer id: delayer){
                        TakePhase1Request req = TakePhase1Request.newBuilder().setSearchPattern(pattern).setClientId(this.clientId).build();
                        stubs[id].takePhase1(req, observers.get(i));
                    }
                }
                else {
                    for (int id=0; id<this.numServers; id++){
                        Random random = new Random(System.currentTimeMillis());
                        try {
                            int wait = random.nextInt(5000);
                            debug("waiting " + wait + "ms");
                            Thread.sleep(wait);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        TakePhase1Request req = TakePhase1Request.newBuilder().setSearchPattern(pattern).setClientId(this.clientId).build();
                        stubs[id].takePhase1(req, observers.get(i));
                    }
                }
                c.waitUntilNReceived(this.numServers);
            } catch (StatusRuntimeException e) {
                System.out.println("Failed to reach server.");
                shutdown();
                System.exit(1);
            } catch (InterruptedException e) {
                System.out.println("Failed to receive answers to take request.");
                shutdown();
                System.exit(1);
            }

            List<Set<String>> listOfSets = new ArrayList<>();
            for (TakePhase1Response r : c.getResponses()) {
                Set<String> tuples = new HashSet<>(r.getReservedTuplesList());
                if ( tuples.size()!=0 ) {
                    accepted++;
                }
                listOfSets.add(tuples);
            }


            //check if intersection is null
            //if it is, check majority/minority
            //if not, immediatly return one of the resulting tuples



            tuple = intersect(listOfSets);

            if (tuple.equals("") && accepted==this.numServers){
                //this means that the servers dont all have the same info, free all locks
                sendReleaseRequest();
                debug("No tuple found and all messages accepted.");
            }

            if (!tuple.equals("")){
                debug("Tuple found with " + accepted + "accepted messages.");
                System.out.println("OK\n" + tuple + "\n");
                return tuple;
            }

            //Checks minority
            if (accepted!=0 && (this.numServers / 2) + 1 > accepted) {
                sendReleaseRequest();
                for (TakePhase1Observer observer: observers){
                    observer.setRejected(true);
                }
            }
            else {
                debug("Majority");
            }
            debug("Accepted: " + accepted);

        }
        return tuple;
    }

    private void takePhase2(String tuple){
        debug("Call takePhase2: tuple - " + tuple);
        try {
            ResponseCollector<TakePhase2Response> c = new ResponseCollector<TakePhase2Response>();
            for(TupleSpacesReplicaGrpc.TupleSpacesReplicaStub stub: this.stubs){
                stub.takePhase2(TakePhase2Request.newBuilder().setTuple(tuple).setClientId(this.clientId).build(), new ResponseObserver<TakePhase2Response>(c));
            }
            c.waitUntilNReceived(this.numServers);
        } catch (StatusRuntimeException e) {
            System.out.println("Failed to reach server.");
            shutdown();
            System.exit(1);
        } catch (InterruptedException e) {
            System.out.println("Failed to receive answers to takePhase2 request.");
            shutdown();
            System.exit(1);
        }
    }

    public void getTupleSpacesState(String qualifier) {
        debug("Call getTupleSpacesState");

        int index = getIndexFromQualifier(qualifier);

        try {
            ResponseCollector<GetTupleSpacesStateResponse> c = new ResponseCollector<GetTupleSpacesStateResponse>();
            stubs[index].getTupleSpacesState(GetTupleSpacesStateRequest.newBuilder().build(), new ResponseObserver<GetTupleSpacesStateResponse>(c));
            c.waitUntilNReceived(1);
            System.out.println("OK\n" + (c.getResponses().get(0)).getTupleList() + "\n");
        } catch (StatusRuntimeException e) {
            System.out.println("Failed to reach server.");
            shutdown();
            System.exit(1);
        } catch (InterruptedException e) {
            System.out.println("Failed to reach server.");
            shutdown();
            System.exit(1);
        }
    }

    public void shutdown(){
        debug("Shutting down..");
        for(ManagedChannel channel: this.channels){
            channel.shutdown();
        }
    }

    private List<String> lookup(int port,String qualifier) {
        LookupResponse reply = null;
        final String target = "localhost" + ":" + port;
        debug("Looking for servers on DNS\nTarget: " + target);
        ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        NameServiceGrpc.NameServiceBlockingStub stub = NameServiceGrpc.newBlockingStub(channel);

        try {
            reply = stub.lookup(
                    LookupRequest.newBuilder().setServiceName("TupleSpace").setServerQualifier(qualifier).build());
            if (reply.getServersList().isEmpty()) {
                return null;
            }
        } catch (StatusRuntimeException e) {
            System.out.println("Caught exception with description: " + e.getStatus().getDescription());
            System.out.println("Could not communicate with name server");
            return null;
        }

        channel.shutdown();
        return reply.getServersList();
    }

    private String intersect(List<Set<String>> listOfSets){
        //Fazer intersecao dos 3 sets
        debug("Intersecting sets");

        for (Set<String> set: listOfSets){
            debug("Set: " + set);
        }

        Set<String> intersection = new HashSet<String>(listOfSets.get(0)); // use the copy constructor
        for (int i=1; i<this.numServers; i++){
            intersection.retainAll(listOfSets.get(i));
        }

        if (intersection.isEmpty()) {
            System.err.println("No tuple found");
            return "";
        }

        //get me the first member of the intersection
        String tuple = intersection.iterator().next();
        return tuple;
    }

    private void sendReleaseRequest(){
        ResponseCollector<TakePhase1ReleaseResponse> col = new ResponseCollector<TakePhase1ReleaseResponse>();
        debug("Minority");
        try {
            for(TupleSpacesReplicaGrpc.TupleSpacesReplicaStub stub: this.stubs){
                TakePhase1ReleaseRequest req = TakePhase1ReleaseRequest.newBuilder().setClientId(this.clientId).build();
                stub.takePhase1Release(req, new ResponseObserver<TakePhase1ReleaseResponse>(col));
            }
            col.waitUntilNReceived(this.numServers);
        } catch (StatusRuntimeException e) {
            System.out.println("Failed to reach server.");
            shutdown();
            System.exit(1);
        } catch (InterruptedException e) {
            System.out.println("Failed to receive answers to take request.");
            shutdown();
            System.exit(1);
        }
    }
}
