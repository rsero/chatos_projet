package fr.upem.net.tcp.nonblocking.server.reader;

import java.nio.ByteBuffer;

import fr.upem.net.tcp.nonblocking.server.data.Data;
import fr.upem.net.tcp.nonblocking.server.data.Login;

public class InstructionReader implements Reader<Data> {
	private enum State {
		DONE, WAITING_OPCODE, WAITING_DATA, ERROR
	};

	private State state = State.WAITING_OPCODE;
	private Data value;
	private Byte opCode;
	private Reader<?> reader;
	
	private void definedReader(Byte opcode) {
		switch (opcode) {
		case 0:
			reader = new LoginReader();
			break;
		case 3:
			reader = new GlobalMessageReader();
			break;
		default:
			break;
		}
	}

	private void definedValue() {
		switch (opCode) {
		case 0:
			value = (Data) reader.get();
			break;
		case 3:
			value = (Data) reader.get();
			break;
		default:
			break;
		}
	}
	
	@Override
	public ProcessStatus process(ByteBuffer bb) {
		if (state == State.DONE || state == State.ERROR) {
			throw new IllegalStateException();
		}
		if (state == State.WAITING_OPCODE) {
			bb.flip();
			if(bb.remaining()<1){
				return ProcessStatus.REFILL;
			}
			opCode = bb.get();
			bb.compact();
			definedReader(opCode);
			
			state = State.WAITING_DATA;
		}
		if (state == State.WAITING_DATA) {
			var stateProcess = reader.process(bb);
			if (stateProcess == ProcessStatus.REFILL) {
				return ProcessStatus.REFILL;
			} else if (stateProcess == ProcessStatus.ERROR) {
				state = State.ERROR;
				return ProcessStatus.ERROR;
			}
			definedValue();
			state = State.DONE;
			return ProcessStatus.DONE;
		}
		return ProcessStatus.REFILL;
	}

	@Override
	public Data get() {
		if (state != State.DONE) {
			throw new IllegalStateException();
		}
		return value;
	}

	@Override
	public void reset() {
		reader.reset();
		state = State.WAITING_OPCODE;
		opCode = null;
	}

}
