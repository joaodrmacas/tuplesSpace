package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesGrpc;
import pt.ulisboa.tecnico.NameServer.contract.NameServiceGrpc;
import pt.ulisboa.tecnico.NameServer.contract.NameServer.LookupRequest;
import pt.ulisboa.tecnico.NameServer.contract.NameServer.LookupResponse;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesCentralized.*;

import java.util.*;

public class ClientService {

    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);
    private ManagedChannel channel;
    private TupleSpacesGrpc.TupleSpacesBlockingStub stub;

    private final int NAME_SERVER_PORT = 5001;

    private static void debug(String debugMessage) {
        if (DEBUG_FLAG)
            System.err.println(debugMessage);
    }

    public ClientService(String qualifier){

        List<String> servers = lookup(NAME_SERVER_PORT, qualifier);

        while (servers == null) {
            System.out.println("There are no servers available.");
            waitForEnterKeyPress();
            servers = lookup(NAME_SERVER_PORT, qualifier);
        }

        debug("Servers available:");
        for (String server : servers) {
            debug(server);
        }

        

        final String target = servers.get(0);
        this.channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        this.stub = TupleSpacesGrpc.newBlockingStub(channel);
    }

    private void waitForEnterKeyPress() {
        System.out.println("Press Enter to retry...");
        Scanner scanner = new Scanner(System.in);
        scanner.nextLine();
    }

    public void put(String tuple) {
        debug("Call put: tuple - " + tuple);
        try {
            stub.put(PutRequest.newBuilder().setNewTuple(tuple).build());
            System.out.println("OK\n");
        } catch (StatusRuntimeException e) {
            System.out.println("Failed to reach server");
            shutdown();
            System.exit(1);
        }
    }

    public void read(String pattern) {
        debug("Call read: pattern - " + pattern);
        try {
            ReadResponse reply = stub.read(ReadRequest.newBuilder().setSearchPattern(pattern).build());
            System.out.println("OK\n" + reply.getResult() + "\n");
        } catch (StatusRuntimeException e) {
            System.out.println("Failed to reach server");
            shutdown();
            System.exit(1);        }
    }

    public void take(String pattern) {
        debug("Call take: pattern - " + pattern);
        try {
            TakeResponse reply = stub.take(TakeRequest.newBuilder().setSearchPattern(pattern).build());
            System.out.println("OK\n" + reply.getResult() + "\n");

        } catch (StatusRuntimeException e) {
            System.out.println("Failed to reach server");
            shutdown();
            System.exit(1);
        }
    }

    public void getTupleSpacesState() {
        debug("Call getTupleSpacesState");
        try {
            GetTupleSpacesStateResponse reply = stub
                    .getTupleSpacesState(GetTupleSpacesStateRequest.newBuilder().build());
            System.out.println("OK\n" + reply.getTupleList() + "\n");
        } catch (StatusRuntimeException e) {
            System.out.println("Failed to reach server");
            shutdown();
            System.exit(1);        }
    }

    public void shutdown(){
        debug("Shutting down..");
        channel.shutdown();
    }

    private List<String> lookup(int port, String qualifier) {
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
