package fr.upem.net.tcp.nonblocking.reader;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

import fr.upem.net.tcp.nonblocking.data.*;

/**
 * Represents a reader that produces a Private Request data
 */
public class PrivateRequestReader implements Reader<RequestOperation> {
    /**
     * Different states the reader can be in
     */
    private enum State {
        DONE, WAITING_REQUESTER_LOGIN, WAITING_TARGET_LOGIN, ERROR
    }

    private State state = State.WAITING_REQUESTER_LOGIN;
    private Login requesterLogin;
    private Login targetLogin;
    private final LoginReader loginReader = new LoginReader();
    private final Byte opCode;
    
    public PrivateRequestReader(Byte opCode) {
		this.opCode = opCode;
	}

    /**
     * Reads the ByteBuffer bb passed
     * @param key
     * @param bb
     * @return ProcessStatus.REFILL if some content is missing, ProcessStatus.ERROR if an error
     * occurred and ProcessStatus.DONE if all the content was processed
     * @throws IllegalStateException if the state is DONE or ERROR at the beginning
     */
	@Override
    public ProcessStatus process(ByteBuffer bb, SelectionKey key) {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }
        if (state == State.WAITING_REQUESTER_LOGIN) {
            var processLogin = loginReader.process(bb, key);
            switch (processLogin) {
                case DONE:
                    requesterLogin = loginReader.get();
                    loginReader.reset();
                    state = State.WAITING_TARGET_LOGIN;
                    break;
                case REFILL:
                    return ProcessStatus.REFILL;
                case ERROR:
                    state = State.ERROR;
                    return ProcessStatus.ERROR;
            }
        }
        if (state == State.WAITING_TARGET_LOGIN) {
            var processLogin = loginReader.process(bb, key);
            switch (processLogin) {
                case DONE:
                    targetLogin = loginReader.get();
                    loginReader.reset();
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
     * @return an RequestOperation object
     * @throws IllegalStateException if the state is not DONE
     */
    @Override
    public RequestOperation get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        switch (opCode){
            case 5:
                return new PrivateRequest(requesterLogin, targetLogin);
            case 6:
                return new AcceptRequest(requesterLogin, targetLogin);
            case 7:
                return new RefuseRequest(requesterLogin, targetLogin);
            default:
                throw new IllegalStateException();
        }
    }

    @Override
    public void reset() {
        state = State.WAITING_REQUESTER_LOGIN;
    }

}