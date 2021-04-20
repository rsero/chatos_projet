package fr.upem.net.tcp.nonblocking.reader;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

import fr.upem.net.tcp.nonblocking.data.AcceptRequest;
import fr.upem.net.tcp.nonblocking.data.PrivateRequest;
/**
 * Represents a reader that produces a Accept Request data
 */
public class AcceptRequestReader implements Reader<AcceptRequest>{
    /**
     * Different states the reader can be in
     */
    private enum State {
        DONE, WAITING_PRIVATE_REQUEST, WAITING_ID, ERROR
    }

    private State state = State.WAITING_PRIVATE_REQUEST;
    private PrivateRequest privateRequest;
    private Long connect_id;
    private final PrivateRequestReader privateRequestReader = new PrivateRequestReader((byte) 5);
    private final LongReader longReader = new LongReader();

    /**
     * Reads the ByteBuffer bb passed
     * @param key
     * @param bb
     * @return ProcessStatus.REFILL if some content is missing, ProcessStatus.ERROR if an error
     * occurred and ProcessStatus.DONE if all the content was processed
     */
	@Override
    public ProcessStatus process(ByteBuffer bb, SelectionKey key) {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }
        if (state == State.WAITING_PRIVATE_REQUEST) {
            var processPrivateRequest = privateRequestReader.process(bb, key);
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
            var processLongReader = longReader.process(bb, key);
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

    /**
     * Gets the Data that have been processed previously
     * @return an AcceptRequest object
     * @throws IllegalStateException if the state is not DONE
     */
    @Override
    public AcceptRequest get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
    	return new AcceptRequest(privateRequest.getLoginRequester(), privateRequest.getLoginTarget(), connect_id);       	
    }

    /**
     * Resets the reader
     */
    @Override
    public void reset() {
        state = State.WAITING_PRIVATE_REQUEST;
        privateRequestReader.reset();
        longReader.reset();
    }

}