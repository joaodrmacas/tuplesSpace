package pt.ulisboa.tecnico.tuplespaces.client.observers;

import pt.ulisboa.tecnico.tuplespaces.client.ResponseCollector;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.TakePhase1Response;

public class TakePhase1Observer extends ResponseObserver<TakePhase1Response>{

    private boolean rejected;

    public TakePhase1Observer (ResponseCollector<TakePhase1Response> c) {
        super(c);
        this.rejected = true;
    }

    public boolean isRejected(){
        return this.rejected;
    }

    public void setRejected(Boolean TorF){
        this.rejected = TorF;
    }

    @Override
    public void onNext(TakePhase1Response takeResponse) {
        collector.addResponse(takeResponse);
        if (!takeResponse.getReservedTuplesList().isEmpty()) {
            this.rejected = false;
        }
    }
}
