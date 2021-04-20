package fr.upem.net.tcp.nonblocking.reader;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
/**
 * Represents a reader that produces an integer value
 */
public class IntReader implements Reader<Integer> {

    /**
     * Different states the reader can be in
     */
    private enum State {
        DONE, WAITING, ERROR
    }

    private State state = State.WAITING;
    private final ByteBuffer internalbb = ByteBuffer.allocate(Integer.BYTES); // write-mode
    private int value;

    /**
     * Reads the ByteBuffer bb passed
     * @param key
     * @param bb
     * @return ProcessStatus.REFILL if the value is not the size of an integer,
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
        value = internalbb.getInt();
        return ProcessStatus.DONE;
    }

    /**
     * Gets the integer value that have been processed previously
     * @return an integer value
     * @throws IllegalStateException if the state is not DONE
     */
    @Override
    public Integer get() {
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
        internalbb.clear();
    }
}
