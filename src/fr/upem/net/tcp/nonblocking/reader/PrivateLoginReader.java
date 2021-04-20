package fr.upem.net.tcp.nonblocking.reader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

import fr.upem.net.tcp.nonblocking.data.PrivateLogin;
/**
 * Represents a reader that produces a Private Login Reader data
 */
public class PrivateLoginReader implements Reader<PrivateLogin> {
	
	private enum State {
		DONE, WAITING_CONNECT_ID, ERROR
	}
	
	private State state = State.WAITING_CONNECT_ID;
	private Long connectId;
	private final LongReader longReader = new LongReader();

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
        if (state == State.WAITING_CONNECT_ID) {
            var processLogin = longReader.process(bb, key);
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
