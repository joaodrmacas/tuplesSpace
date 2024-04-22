package pt.ulisboa.tecnico.tuplespaces.client.observers;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.client.ResponseCollector;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.TakePhase1Response;

public class ResponseObserver<R> implements StreamObserver<R> {

    protected ResponseCollector<R> collector;



    public ResponseObserver (ResponseCollector<R> c) {
        collector = c;
    }
    public ResponseObserver (ResponseCollector<R> c, int serverId) {
        collector = c;
    }


    @Override
    public void onNext(R r) {
        collector.addResponse(r);
    }

    @Override
    public void onError(Throwable throwable) {
        System.err.println("Received error: " + throwable);
    }

    @Override
    public void onCompleted() {
        // System.out.println("Request completed");
    }
}
