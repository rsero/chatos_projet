package fr.upem.net.tcp.nonblocking.reader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

import fr.upem.net.tcp.nonblocking.data.PrivateConnexionTransmission;

public class PrivateConnexionTransmissionReader implements Reader<PrivateConnexionTransmission>{

    public PrivateConnexionTransmissionReader(SelectionKey key) {
        this.key=key;
    }

    private enum State {
        DONE, WAITING, ERROR
    }

    private static int BUFFER_SIZE = 1024;
    private State state = State.WAITING;
    private ByteBuffer internalbb = ByteBuffer.allocate(BUFFER_SIZE); // write-mode
    private final SelectionKey key;
    private PrivateConnexionTransmission value;

    @Override
    public ProcessStatus process(ByteBuffer bb, SelectionKey key) {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }
        bb.flip();
        try {
            internalbb.put(bb);
        } finally {
            bb.compact();
        }
        state = State.DONE;
        internalbb.flip();
        value = new PrivateConnexionTransmission(internalbb, key);
        bb.clear();
        return ProcessStatus.DONE;
    }

    @Override
    public PrivateConnexionTransmission get() throws IOException {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return value;
    }

    @Override
    public void reset() {
        state = State.WAITING;
        internalbb.clear();
    }

}
