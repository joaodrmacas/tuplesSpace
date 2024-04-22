package pt.ulisboa.tecnico.tuplespaces.server;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesCentralized.*;
import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesGrpc.TupleSpacesImplBase;
import pt.ulisboa.tecnico.tuplespaces.server.domain.ServerState;

import static io.grpc.Status.INVALID_ARGUMENT;

public class TupleServerImpl extends TupleSpacesImplBase {

    private ServerState state = new ServerState();

    @Override
    public void put(PutRequest request, StreamObserver<PutResponse> responseObserver) {
        int ret = state.put(request.getNewTuple());

        if (ret == -1) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Invalid tuple.").asRuntimeException());
        }

        PutResponse response = PutResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void read(ReadRequest request, StreamObserver<ReadResponse> responseObserver) {
        String tuple = state.read(request.getSearchPattern());

        if (tuple == null) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Invalid pattern.").asRuntimeException());
        }

        ReadResponse response = ReadResponse.newBuilder().setResult(tuple).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void take(TakeRequest request, StreamObserver<TakeResponse> responseObserver) {
        String tuple = state.take(request.getSearchPattern());

        if (tuple == null) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Invalid pattern.").asRuntimeException());
        }

        TakeResponse response = TakeResponse.newBuilder().setResult(tuple).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getTupleSpacesState(GetTupleSpacesStateRequest request,
            StreamObserver<GetTupleSpacesStateResponse> responseObserver) {
        GetTupleSpacesStateResponse response = GetTupleSpacesStateResponse.newBuilder()
                .addAllTuple(state.getTupleSpacesState()).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

}
