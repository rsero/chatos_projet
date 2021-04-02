package fr.upem.net.tcp.nonblocking.server.reader;

import fr.upem.net.tcp.nonblocking.server.data.Login;
import fr.upem.net.tcp.nonblocking.server.data.MessageGlobal;

import java.nio.ByteBuffer;

public class GlobalMessageReader implements Reader<MessageGlobal> {
	private enum State {
		DONE, WAITING_LOGIN, WAITING_MSG, ERROR
	};

	private State state = State.WAITING_LOGIN;
	private String msg;
	private Login login;
	private final StringReader loginReader = new StringReader();
	private final StringReader messageReader = new StringReader();

	@Override
	public ProcessStatus process(ByteBuffer bb) {
		if (state == State.DONE || state == State.ERROR) {
			throw new IllegalStateException();
		}
		if (state == State.WAITING_LOGIN) {
			var processlogin = loginReader.process(bb);
			switch (processlogin) {
			case DONE:
				login = new Login(loginReader.get());
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
			//bb.flip();
			var processmsg = messageReader.process(bb);
			switch (processmsg) {
			case DONE:
				msg = messageReader.get();
				System.out.println("mssg >>" + msg);
				state = State.DONE;
				break;
			case REFILL:
				System.out.println("RREffil");
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
		loginReader.reset();
		messageReader.reset();
	}

}