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
    private ByteBuffer internalbb = ByteBuffer.allocate(BUFFER_SIZE); // write-mode
    private PrivateConnexionTransmission value;

    @Override
    public ProcessStatus process(ByteBuffer bb) {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException("première erreur");
        }
        bb.flip();
        try {
            internalbb = bb;
        } finally {
            bb.compact();
        }
        state = State.DONE;
        internalbb.flip();
        value = new PrivateConnexionTransmission(internalbb);
        return ProcessStatus.DONE;
    }

	@Override
	public PrivateConnexionTransmission get() throws IOException {
		if (state != State.DONE) {
            throw new IllegalStateException("deuxieme erreur");
        }
        return value;
    }

    @Override
    public void reset() {
        state = State.WAITING;
        internalbb.clear();
    }

}
