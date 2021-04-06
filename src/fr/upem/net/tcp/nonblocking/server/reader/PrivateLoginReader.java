package fr.upem.net.tcp.nonblocking.server.reader;

import java.io.IOException;
import java.nio.ByteBuffer;

import fr.upem.net.tcp.nonblocking.server.data.PrivateLogin;

public class PrivateLoginReader implements Reader<PrivateLogin> {
	
	private enum State {
		DONE, WAITING_CONNECTID, ERROR
	};
	
	private State state = State.WAITING_CONNECTID;
	private Long connectId;
	private final LongReader longReader = new LongReader();

	@Override
	public ProcessStatus process(ByteBuffer bb) {
		if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }
        if (state == State.WAITING_CONNECTID) {
            var processLogin = longReader.process(bb);
            switch (processLogin) {
                case DONE:
                    connectId = longReader.get();
                    longReader.reset();
                    state = State.DONE;
                    break;
                case REFILL:
                    return ProcessStatus.REFILL;
                case ERROR:
                    state = State.ERROR;
                    return ProcessStatus.ERROR;
            }
        }
        return ProcessStatus.DONE;
	}

	@Override
	public PrivateLogin get() throws IOException {
		if (state != State.DONE) {
			throw new IllegalStateException();
		}
		return new PrivateLogin(connectId);
	}

	@Override
	public void reset() {
		longReader.reset();
	}

}
