package pt.ulisboa.tecnico.tuplespaces.server;

import io.grpc.stub.StreamObserver;
//import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesCentralized.*;
//import pt.ulisboa.tecnico.tuplespaces.centralized.contract.TupleSpacesGrpc.TupleSpacesImplBase;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.*;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaGrpc.TupleSpacesReplicaImplBase;
import pt.ulisboa.tecnico.tuplespaces.server.domain.ServerState;

import java.util.Set    ;

import static io.grpc.Status.INVALID_ARGUMENT;

public class TupleServerImpl extends TupleSpacesReplicaImplBase {

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
    public void takePhase1(TakePhase1Request request, StreamObserver<TakePhase1Response> responseObserver) {
        Set<String> tuples = state.takePhase1(request.getSearchPattern(), request.getClientId());

        if (tuples == null) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Invalid pattern.").asRuntimeException());
        }

        TakePhase1Response response = TakePhase1Response.newBuilder().addAllReservedTuples(tuples).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void takePhase1Release(TakePhase1ReleaseRequest request, StreamObserver<TakePhase1ReleaseResponse> responseObserver) {
        state.takePhase1Release(request.getClientId());


        TakePhase1ReleaseResponse response = TakePhase1ReleaseResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
    @Override
    public void takePhase2(TakePhase2Request request, StreamObserver<TakePhase2Response> responseObserver) {
        int res = state.takePhase2(request.getTuple(), request.getClientId());

        if (res == -1) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("No valid tuple founded.").asRuntimeException());
        }


        TakePhase2Response response = TakePhase2Response.newBuilder().build();
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
