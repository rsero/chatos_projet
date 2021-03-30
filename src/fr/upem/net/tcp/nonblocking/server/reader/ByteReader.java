package fr.upem.net.tcp.nonblocking.server.reader;

import java.nio.ByteBuffer;

import fr.upem.net.tcp.nonblocking.server.data.OpCode;

public class ByteReader implements Reader<OpCode> {

    private enum State {
        DONE, WAITING, ERROR
    };

    private State state = State.WAITING;
    private final ByteBuffer internalbb = ByteBuffer.allocate(Byte.BYTES); // write-mode
    private OpCode value;

    @Override
    public ProcessStatus process(ByteBuffer bb) {
        if (state == State.DONE || state == State.ERROR) {
            System.out.println("state: " + state);
            throw new IllegalStateException();
        }
        //bb.flip();
        try {
        	System.out.println("bbremaining >> " + bb.remaining());
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
        internalbb.clear();
    }
}