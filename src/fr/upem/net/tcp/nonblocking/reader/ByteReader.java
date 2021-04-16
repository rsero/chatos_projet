package fr.upem.net.tcp.nonblocking.reader;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

import fr.upem.net.tcp.nonblocking.data.OpCode;

public class ByteReader implements Reader<OpCode> {

    private enum State {
        DONE, WAITING, ERROR
    }

    private State state = State.WAITING;
    private OpCode value;

    @Override
    public ProcessStatus process(ByteBuffer bb, SelectionKey key) {
        //System.out.println("process bytereader");
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException(state.toString());
        }
        bb.flip();
        try {
            if (bb.remaining() >= 1) {
                value = new OpCode(bb.get());
                state = State.DONE;
                return ProcessStatus.DONE;
            }
            return ProcessStatus.REFILL;
        } finally {
            bb.compact();
        }
    }

    @Override
    public OpCode get() {
        if (state != State.DONE) {
            System.out.println(state);
            throw new IllegalStateException();
        }
        return value;
    }

    @Override
    public void reset() {
        state = State.WAITING;
    }
}