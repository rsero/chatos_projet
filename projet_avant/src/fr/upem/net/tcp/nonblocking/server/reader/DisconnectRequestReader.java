package fr.upem.net.tcp.nonblocking.server.reader;

import fr.upem.net.tcp.nonblocking.server.data.DisconnectRequest;

import java.io.IOException;
import java.nio.ByteBuffer;

public class DisconnectRequestReader implements Reader<DisconnectRequest> {

    private Long connect_id;
    private final LongReader longReader = new LongReader();

    private enum State {
        DONE, WAITING_ID, ERROR
    }

    private State state = State.WAITING_ID;

    @Override
    public ProcessStatus process(ByteBuffer bb) throws IOException {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
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
    public DisconnectRequest get() throws IOException {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return new DisconnectRequest(connect_id);
    }

    @Override
    public void reset() {
        state = State.WAITING_ID;
        longReader.reset();
    }
}
