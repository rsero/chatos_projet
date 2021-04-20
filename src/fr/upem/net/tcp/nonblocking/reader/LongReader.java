package fr.upem.net.tcp.nonblocking.reader;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
/**
 * Represents a reader that produces a long value
 */
public class LongReader implements Reader<Long> {

    private enum State {
        DONE, WAITING, ERROR
    }

    private State state = State.WAITING;
    private final ByteBuffer internalbb = ByteBuffer.allocate(Long.BYTES); // write-mode
    private long value;

    /**
     * Reads the ByteBuffer bb passed
     * @param key
     * @param bb
     * @return ProcessStatus.REFILL if the value is not the size of a long,
     * and ProcessStatus.DONE if all the content was processed
     * @throws IllegalStateException if the state is DONE or ERROR at the beginning
     */
    @Override
    public ProcessStatus process(ByteBuffer bb, SelectionKey key) {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
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
        if (internalbb.hasRemaining()) {
            return ProcessStatus.REFILL;
        }
        state = State.DONE;
        internalbb.flip();
        value = internalbb.getLong();
        return ProcessStatus.DONE;
    }

    @Override
    public Long get() {
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

