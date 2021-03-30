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

		if(internalbb.remaining()<1){
			return ProcessStatus.REFILL;
		}

		state = State.DONE;
        internalbb.flip();
        value = new OpCode(internalbb.get());
        return ProcessStatus.DONE;
    }

    @Override
    public OpCode get() {
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
