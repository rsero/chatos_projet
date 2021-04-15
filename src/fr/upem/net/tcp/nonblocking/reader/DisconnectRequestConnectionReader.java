package fr.upem.net.tcp.nonblocking.reader;

import fr.upem.net.tcp.nonblocking.data.DisconnectRequestConnection;
import fr.upem.net.tcp.nonblocking.data.Login;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

public class DisconnectRequestConnectionReader implements Reader<DisconnectRequestConnection> {

    private Login loginRequester;
    private final LoginReader loginReaderRequester = new LoginReader();

    private enum State {
        DONE, WAITING_LOGIN, ERROR
    }

    private State state = State.WAITING_LOGIN;

    @Override
    public ProcessStatus process(ByteBuffer bb, SelectionKey key) throws IOException {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }
        switch (state){
            case WAITING_LOGIN :
                var processLongReader = loginReaderRequester.process(bb, key);
                switch (processLongReader) {
                    case DONE:
                        loginRequester = loginReaderRequester.get();
                        loginReaderRequester.reset();
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
    public DisconnectRequestConnection get() throws IOException {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return new DisconnectRequestConnection(loginRequester);
    }

    @Override
    public void reset() {
        state = State.WAITING_LOGIN;
        loginReaderRequester.reset();
    }
}
