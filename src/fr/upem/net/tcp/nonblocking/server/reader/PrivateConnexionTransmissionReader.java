package fr.upem.net.tcp.nonblocking.server.reader;

import java.io.IOException;
import java.nio.ByteBuffer;

import fr.upem.net.tcp.nonblocking.server.data.PrivateConnexionTransmission;

public class PrivateConnexionTransmissionReader implements Reader<PrivateConnexionTransmission>{

	private enum State {
        DONE, WAITING, ERROR
    };

    private static int BUFFER_SIZE = 1024;
    private State state = State.WAITING;
    private final ByteBuffer internalbb = ByteBuffer.allocate(BUFFER_SIZE); // write-mode
    private PrivateConnexionTransmission value;

    @Override
    public ProcessStatus process(ByteBuffer bb) {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }
        if(bb.remaining() != BUFFER_SIZE)
        	return ProcessStatus.REFILL;
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
        internalbb.compact();
        value = new PrivateConnexionTransmission(internalbb);
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
