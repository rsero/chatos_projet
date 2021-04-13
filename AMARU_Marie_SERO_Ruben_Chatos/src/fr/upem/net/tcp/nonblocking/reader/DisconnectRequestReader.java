package fr.upem.net.tcp.nonblocking.reader;

import fr.upem.net.tcp.nonblocking.data.DisconnectRequest;
import fr.upem.net.tcp.nonblocking.data.Login;

import java.io.IOException;
import java.nio.ByteBuffer;

public class DisconnectRequestReader implements Reader<DisconnectRequest> {

    private Long connect_id;
    private Login loginRequester;
    private Login loginTarget;
    private final LongReader longReader = new LongReader();
    private final LoginReader loginReaderRequester = new LoginReader();
    private final LoginReader loginReaderTarget = new LoginReader();

    private enum State {
        DONE, WAITING_ID, WAITING_LOGIN_REQUESTER, WAITING_LOGIN_TARGET, ERROR
    }

    private State state = State.WAITING_ID;

    @Override
    public ProcessStatus process(ByteBuffer bb) throws IOException {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }
        switch (state){
            case WAITING_ID :
            var processLongReader = longReader.process(bb);
            switch (processLongReader) {
                case DONE:
                    connect_id = longReader.get();
                    longReader.reset();
                    state = State.WAITING_LOGIN_REQUESTER;
                    break;
                case REFILL:
                    return ProcessStatus.REFILL;
                case ERROR:
                    state = State.ERROR;
                    return ProcessStatus.ERROR;
            }
            case WAITING_LOGIN_REQUESTER:
                var processLoginReader = loginReaderRequester.process(bb);
                switch (processLoginReader) {
                    case DONE:
                        loginRequester = loginReaderRequester.get();
                        loginReaderRequester.reset();
                        state = State.WAITING_LOGIN_TARGET;
                        break;
                    case REFILL:
                        return ProcessStatus.REFILL;
                    case ERROR:
                        state = State.ERROR;
                        return ProcessStatus.ERROR;
                }
            case WAITING_LOGIN_TARGET:
                var processLoginTarget = loginReaderTarget.process(bb);
                switch (processLoginTarget){
                    case DONE :
                        loginTarget = loginReaderTarget.get();
                        loginReaderTarget.reset();
                        state = State.DONE;
                        break;
                    case REFILL :
                        return ProcessStatus.REFILL;
                    case ERROR :
                        state = State.ERROR;
                        return ProcessStatus.ERROR;
                }
        }
        return ProcessStatus.DONE;
    }

    @Override
    public DisconnectRequest get() throws IOException {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return new DisconnectRequest(connect_id, loginRequester, loginTarget);
    }

    @Override
    public void reset() {
        state = State.WAITING_ID;
        longReader.reset();
        loginReaderRequester.reset();
        loginReaderTarget.reset();
    }
}
