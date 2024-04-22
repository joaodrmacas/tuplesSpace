package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import java.util.List;
import java.util.Random;
import java.util.Scanner;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.NameServer.contract.NameServer.LookupRequest;
import pt.ulisboa.tecnico.NameServer.contract.NameServer.LookupResponse;
import pt.ulisboa.tecnico.NameServer.contract.NameServiceGrpc;
import pt.ulisboa.tecnico.tuplespaces.client.ResponseCollector;
import pt.ulisboa.tecnico.tuplespaces.client.observers.ResponseObserver;
import pt.ulisboa.tecnico.tuplespaces.client.util.OrderedDelayer;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaGrpc;
import pt.ulisboa.tecnico.sequencer.contract.SequencerGrpc;
import pt.ulisboa.tecnico.sequencer.contract.SequencerOuterClass.GetSeqNumberRequest;
import pt.ulisboa.tecnico.sequencer.contract.SequencerOuterClass.GetSeqNumberResponse;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaTotalOrder.GetTupleSpacesStateRequest;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaTotalOrder.GetTupleSpacesStateResponse;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaTotalOrder.PutRequest;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaTotalOrder.PutResponse;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaTotalOrder.ReadRequest;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaTotalOrder.ReadResponse;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaTotalOrder.TakeRequest;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaTotalOrder.TakeResponse;


public class ClientService {

    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);
    private ManagedChannel[] channels;
    private final int NAME_SERVER_PORT = 5001;
    private final int SEQUENCER_SERVER_PORT = 8080;

    private final String target = "localhost:" + SEQUENCER_SERVER_PORT; 

    ManagedChannel sequencer_channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
    SequencerGrpc.SequencerBlockingStub sequencer_stub = SequencerGrpc.newBlockingStub(sequencer_channel);    private TupleSpacesReplicaGrpc.TupleSpacesReplicaStub[] stubs;

    private Integer numServers;
    private int clientId;


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
            GetSeqNumberResponse sequencer_response = sequencer_stub.getSeqNumber(GetSeqNumberRequest.newBuilder().build());
            for (Integer id: delayer){
                debug("Sending request to server " + id);
                stubs[id].put(PutRequest.newBuilder().setNewTuple(tuple).setSeqNumber(sequencer_response.getSeqNumber()).build(), new ResponseObserver<PutResponse>(c));
            }
            debug("Waiting for responses from servers");
            c.waitUntilNReceived(this.numServers);
            debug("Received all responses");
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
        debug("Call take: pattern - " + pattern);
        try {
            ResponseCollector<TakeResponse> c = new ResponseCollector<TakeResponse>();
            GetSeqNumberResponse sequencer_response = sequencer_stub.getSeqNumber(GetSeqNumberRequest.newBuilder().build());
            for (Integer id: delayer){
                stubs[id].take(TakeRequest.newBuilder().setSearchPattern(pattern).setSeqNumber(sequencer_response.getSeqNumber()).build(), new ResponseObserver<TakeResponse>(c));
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
}
