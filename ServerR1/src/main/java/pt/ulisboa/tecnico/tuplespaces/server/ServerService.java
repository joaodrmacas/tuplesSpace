package pt.ulisboa.tecnico.tuplespaces.server;

import io.grpc.BindableService;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.NameServer.contract.NameServiceGrpc;
import pt.ulisboa.tecnico.NameServer.contract.NameServer.DeleteRequest;
import pt.ulisboa.tecnico.NameServer.contract.NameServer.RegisterRequest;

import sun.misc.Signal;
import sun.misc.SignalHandler;

public class ServerService {

    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);
    private Server server;
    final ManagedChannel DNSchannel;
    final NameServiceGrpc.NameServiceBlockingStub stub;

    private static void debug(String debugMessage) {
		if (DEBUG_FLAG)
			System.err.println(debugMessage);
	}
    

    public ServerService(){
        String target = "localhost:5001";
        debug("Connecting to DNS server on target: " + target);
        this.DNSchannel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        this.stub = NameServiceGrpc.newBlockingStub(this.DNSchannel);
    }

    public void execute(String port, String qualifier) {
        final int p = Integer.parseInt(port);
        

        debug("Creating server on port: " + p);
        final BindableService impl = new TupleServerImpl();
        server = ServerBuilder.forPort(p).addService(impl).build();

        debug("Server created succesfully");

        try{
            server.start();
        } catch (Exception e) {
            System.out.println("Error starting server: " + e);
            return;
        }

        System.out.println("Server started, listening on " + p);
        
        if (!registerOnDNS(p, qualifier)) System.exit(1);

        Signal.handle(new Signal("INT"), new SignalHandler() {
            public void handle(Signal sig) {
                System.out.println("Ctrl+C pressed.\nUnregistering from DNS before exiting...");
                deleteFromDNS(p);
                System.exit(0);
            }
        });

        try {
            server.awaitTermination();
        } catch (Exception e) {
            System.out.println("Error running server: " + e);
        }
    }

    private boolean registerOnDNS(int port, String qualifier){
        debug("Registering on DNS");
        String address = "localhost:" + port;
        System.out.println(address);
        try{
            stub.register(RegisterRequest.newBuilder().setServiceName("TupleSpace").setServerAddress(address).setServerQualifier(qualifier).build());
        } catch (StatusRuntimeException e){
            System.out.println("Caught exception description: " + e.getStatus().getDescription());
            System.out.println("Could not communicate with name server. Make sure the DNS is active");
            return false;
        }
        return true;
    }

    private void deleteFromDNS(int port){
        String address = "localhost:" + port;
        debug("Unregistering from DNS");
        try{
            stub.delete(DeleteRequest.newBuilder().setServiceName("TupleSpace").setServerAddress(address).build());
        } catch (StatusRuntimeException e){
            System.out.println("Caught exception description: " + e.getStatus().getDescription());
        }
        DNSchannel.shutdown();
    }
    
}
