package fr.upem.net.tcp.nonblocking.reader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.charset.StandardCharsets;

import fr.upem.net.tcp.nonblocking.data.PrivateConnectionTransmission;
/**
 * Represents a reader that produces a Private Connection Transmission data
 */
public class PrivateConnectionTransmissionReader implements Reader<PrivateConnectionTransmission>{

    public PrivateConnectionTransmissionReader(SelectionKey key) {
        this.key=key;
    }
    /**
     * Different states the reader can be in
     */
    private enum State {
        DONE, WAITING, ERROR
    }

    private static int BUFFER_SIZE = 1024;
    private State state = State.WAITING;
    private ByteBuffer internalbb = ByteBuffer.allocate(BUFFER_SIZE); // write-mode
    private final SelectionKey key;
    private PrivateConnectionTransmission value;

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
        if(bb.position() == 0){
            return ProcessStatus.REFILL;
        }
        bb.flip();
        try {
            if (bb.remaining() <= internalbb.remaining()) {
                internalbb.put(bb);
            } else {
                var oldLimit = bb.limit();
                bb.limit(internalbb.remaining());
                internalbb.put(bb);
                bb.limit(oldLimit);
            }
        } finally {
            bb.compact();
        }
        state = State.DONE;
        internalbb.flip();
        System.out.println(">>>>>>>>processin"+StandardCharsets.UTF_8.decode(internalbb));
        value = new PrivateConnectionTransmission(internalbb.flip(), key);
        bb.clear();
        return ProcessStatus.DONE;
    }

    /**
     * Gets the Private Connection Transmission that have been processed previously
     * @return a PrivateConnectionTransmission object
     * @throws IllegalStateException if the state is not DONE
     */
    @Override
    public PrivateConnectionTransmission get() throws IOException {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return value;
    }

    /**
     * Resets the reader
     */
    @Override
    public void reset() {
        state = State.WAITING;
        internalbb = ByteBuffer.allocate(BUFFER_SIZE);
    }

}
