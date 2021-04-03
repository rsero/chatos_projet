package fr.upem.net.tcp.nonblocking.server.reader;

import fr.upem.net.tcp.nonblocking.server.data.Login;

import java.nio.ByteBuffer;

public class LoginReader implements Reader<Login> {

	private enum State {
		DONE, WAITING_STR, ERROR
	};

	private State state = State.WAITING_STR;
	private static final int BUFFER_SIZE = 34;
	private final ByteBuffer internalbb = ByteBuffer.allocate(BUFFER_SIZE); // write-mode
	private StringReader stringReader = new StringReader();
	private Login login;

	public Reader.ProcessStatus process(ByteBuffer bb) {
		var processlogin = stringReader.process(bb);
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

	@Override
	public Login get() {
		if (state != State.DONE) {
			throw new IllegalStateException();
		}
		return login;
	}

	@Override
	public void reset() {
		state = LoginReader.State.WAITING_STR;
		internalbb.clear();
	}
}
