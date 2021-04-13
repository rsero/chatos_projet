package fr.upem.net.tcp.nonblocking.reader;

import java.nio.ByteBuffer;

import fr.upem.net.tcp.nonblocking.data.AcceptRequest;
import fr.upem.net.tcp.nonblocking.data.PrivateRequest;

public class AcceptRequestReader implements Reader<AcceptRequest>{

    private enum State {
        DONE, WAITING_PRIVATE_REQUEST, WAITING_ID, ERROR
    }

    private State state = State.WAITING_PRIVATE_REQUEST;
    private PrivateRequest privateRequest;
    private Long connect_id;
    private final PrivateRequestReader privateRequestReader = new PrivateRequestReader((byte) 5);
    private final LongReader longReader = new LongReader();

	@Override
    public ProcessStatus process(ByteBuffer bb) {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }
        if (state == State.WAITING_PRIVATE_REQUEST) {
            var processPrivateRequest = privateRequestReader.process(bb);
            switch (processPrivateRequest) {
                case DONE:
                	privateRequest = (PrivateRequest) privateRequestReader.get();
                    privateRequestReader.reset();
                    state = State.WAITING_ID;
                    break;
                case REFILL:
                    return ProcessStatus.REFILL;
                case ERROR:
                    state = State.ERROR;
                    return ProcessStatus.ERROR;
            }
        }
        if (state == State.WAITING_ID) {
            var processLongReader = longReader.process(bb);
            switch (processLongReader) {
                case DONE:
                    connect_id = longReader.get();
                    longReader.reset();
                    state = State.DONE;
                    break;
                case REFILL:
                    return ProcessStatus.REFILL;
                case ERROR:
                    state = State.ERROR;
                    return ProcessStatus.ERROR;
            }
        }
        return ProcessStatus.DONE;
    }

    @Override
    public AcceptRequest get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
    	return new AcceptRequest(privateRequest.getLoginRequester(), privateRequest.getLoginTarget(), connect_id);       	
    }

    @Override
    public void reset() {
        state = State.WAITING_PRIVATE_REQUEST;
        privateRequestReader.reset();
        longReader.reset();
    }

}