package fr.upem.net.tcp.nonblocking.server.reader;

import fr.upem.net.tcp.nonblocking.server.data.Login;
import fr.upem.net.tcp.nonblocking.server.data.MessageGlobal;

import java.nio.ByteBuffer;

public class GlobalMessageReader implements Reader<MessageGlobal> {
	private enum State {
		DONE, WAITING_LOGIN, WAITING_MSG, ERROR
	}

	private State state = State.WAITING_LOGIN;
	private String msg;
	private Login login;
	private final StringReader stringReader = new StringReader();

	@Override
	public ProcessStatus process(ByteBuffer bb) {
		if (state == State.DONE || state == State.ERROR) {
			throw new IllegalStateException();
		}
		if (state == State.WAITING_LOGIN) {
			var processLogin = stringReader.process(bb);
			switch (processLogin) {
			case DONE:
				login = new Login(stringReader.get());
				stringReader.reset();
				state = State.WAITING_MSG;
				break;
			case REFILL:
				return ProcessStatus.REFILL;
			case ERROR:
				state = State.ERROR;
				return ProcessStatus.ERROR;
			}
		}
		if (state == State.WAITING_MSG) {
			var processMsg = stringReader.process(bb);
			switch (processMsg) {
			case DONE:
				msg = stringReader.get();
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
	public MessageGlobal get() {
		if (state != State.DONE) {
			throw new IllegalStateException();
		}
		return new MessageGlobal(login, msg);
	}

	@Override
	public void reset() {
		state = State.WAITING_LOGIN;
		stringReader.reset();
	}

}