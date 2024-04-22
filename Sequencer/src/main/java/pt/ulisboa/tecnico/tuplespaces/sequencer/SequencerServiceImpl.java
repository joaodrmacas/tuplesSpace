package pt.ulisboa.tecnico.tuplespaces.sequencer;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.sequencer.contract.SequencerGrpc.SequencerImplBase;
import pt.ulisboa.tecnico.sequencer.contract.SequencerOuterClass.GetSeqNumberRequest;
import pt.ulisboa.tecnico.sequencer.contract.SequencerOuterClass.GetSeqNumberResponse;

import java.util.List;

public class SequencerServiceImpl extends SequencerImplBase {
	private static boolean DEBUG_FLAG = false;  //needs to be manually set for now
	int seqNumber;

	public SequencerServiceImpl() {
		seqNumber = 0;
	}

	@Override
	synchronized public void getSeqNumber(GetSeqNumberRequest request, StreamObserver<GetSeqNumberResponse> responseObserver) {
		seqNumber ++;
		debug("Sequence number sent: " + seqNumber);
		GetSeqNumberResponse response = GetSeqNumberResponse.newBuilder()
				.setSeqNumber(seqNumber).build();
		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}

    private static void debug(String debugMessage) {
		if (DEBUG_FLAG)
			System.err.println(debugMessage);
	}

}
