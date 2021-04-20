package fr.upem.net.tcp.nonblocking.reader;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

import fr.upem.net.tcp.nonblocking.data.OpCode;
/**
 * Represents a reader that produces an OpCode data
 */
public class ByteReader implements Reader<OpCode> {
    /**
     * Different states the reader can be in
     */
    private enum State {
        DONE, WAITING, ERROR
    }

    private State state = State.WAITING;
    private OpCode value;

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
            throw new IllegalStateException(state.toString());
        }
        bb.flip();
        try {
            if (bb.remaining() >= 1) {
                value = new OpCode(bb.get(), key);
                state = State.DONE;
                return ProcessStatus.DONE;
            }
            return ProcessStatus.REFILL;
        } finally {
            bb.compact();
        }
    }
    /**
     * Gets the Data that have been processed previously
     * @return an OpCode object
     * @throws IllegalStateException if the state is not DONE
     */
    @Override
    public OpCode get() {
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
    }
}