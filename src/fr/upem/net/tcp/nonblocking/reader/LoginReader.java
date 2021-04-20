package fr.upem.net.tcp.nonblocking.reader;

import fr.upem.net.tcp.nonblocking.data.Login;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
/**
 * Represents a reader that produces a Login data
 */
public class LoginReader implements Reader<Login> {

	private enum State {
		DONE, WAITING_STR, ERROR
	}

	private State state = State.WAITING_STR;
	private static final int BUFFER_SIZE = 34;
	private final ByteBuffer internalbb = ByteBuffer.allocate(BUFFER_SIZE); // write-mode
	private final StringReader stringReader = new StringReader();
	private Login login;

	/**
	 * Reads the ByteBuffer bb passed
	 * @param key
	 * @param bb
	 * @return ProcessStatus.REFILL if some content is missing, ProcessStatus.ERROR if an error
	 * occurred and ProcessStatus.DONE if all the content was processed
	 * @throws IllegalStateException if the state is DONE or ERROR at the beginning
	 */
	public ProcessStatus process(ByteBuffer bb, SelectionKey key) {
		var processlogin = stringReader.process(bb,key);
		switch (processlogin) {
		case DONE:
			var log = stringReader.get();
			login = new Login(log);
			stringReader.reset();
			state = State.DONE;
			break;
		case REFILL:
			return ProcessStatus.REFILL;
		case ERROR:
			state = State.ERROR;
			return ProcessStatus.ERROR;
		}
		return ProcessStatus.DONE;
	}

	/**
	 * Gets the Login that have been processed previously
	 * @return a Login object
	 * @throws IllegalStateException if the state is not DONE
	 */
	@Override
	public Login get() {
		if (state != State.DONE) {
			throw new IllegalStateException();
		}
		return login;
	}

	/**
	 * Resets the reader
	 */
	@Override
	public void reset() {
		state = State.WAITING_STR;
		internalbb.clear();
	}
}
