package fr.upem.net.tcp.nonblocking.reader;

import java.io.IOException;
import java.nio.ByteBuffer;

import fr.upem.net.tcp.nonblocking.data.Data;
import fr.upem.net.tcp.nonblocking.data.OpCode;

public class InstructionReader implements Reader<Data> {
	private enum State {
		DONE, WAITING_OPCODE, WAITING_DATA, ERROR
	}

	private State state = State.WAITING_OPCODE;
	private Data value;
	private OpCode opCode;
	private Reader<?> reader;
	private final ByteReader byteReader = new ByteReader();

	private void definedReader(OpCode opcode, ByteReader byteReader) {
		switch (opcode.getByte()) {
		case 0://Identification
			reader = new LoginReader();
			state = State.WAITING_DATA;
			break;
		case 1://Identification accepted
		case 2://identification refused
		case 10://Private connection established
			System.out.println("instruction reader case 10");
			reader = byteReader;
			state = State.DONE;
			break;
		case 3://Global message
			reader = new GlobalMessageReader();
			state = State.WAITING_DATA;
			break;
		case 4://Private message
			reader = new PrivateMessageReader();
			state = State.WAITING_DATA;
			break;
		case 5://Private request
			reader = new PrivateRequestReader((byte) 5);
			state = State.WAITING_DATA;
			break;
		case 6://Private connexion accepted
			reader = new PrivateRequestReader((byte) 6);
			state = State.WAITING_DATA;
			break;
		case 7://Private connexion refused
			reader = new PrivateRequestReader((byte) 7);
			state = State.WAITING_DATA;
			break;
		case 8://Accept connection and give connect_id
			reader = new AcceptRequestReader();
			state = State.WAITING_DATA;
			break;
		case 9://Send private login
			System.out.println("instruction reader case 9");
			reader = new PrivateLoginReader();
			state = State.WAITING_DATA;
			break;
		case 11://Private connection closed
			reader = new DisconnectRequestReader();
			state = State.WAITING_DATA;
			break;
		case 12: //Private connection close by server
			reader = new DisconnectRequestConnectionReader();
			state = State.WAITING_DATA;
			break;
		default:
			break;
		}
	}

	@Override
	public ProcessStatus process(ByteBuffer bb) throws IOException {
		if (state == State.DONE || state == State.ERROR) {
			throw new IllegalStateException();
		}
		if (state == State.WAITING_OPCODE) {
			var stat = byteReader.process(bb);
			if(stat!=ProcessStatus.DONE){
				return stat;
			}
			opCode = byteReader.get();
			definedReader(opCode, byteReader);
		}
		if (state == State.WAITING_DATA) {
			var stateProcess = reader.process(bb);
			if (stateProcess == ProcessStatus.REFILL) {
				return ProcessStatus.REFILL;
			} else if (stateProcess == ProcessStatus.ERROR) {
				state = State.ERROR;
				return ProcessStatus.ERROR;
			}
			state = State.DONE;
		}
		if(state == State.DONE) {
			value = (Data) reader.get();
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
		byteReader.reset();
		state = State.WAITING_OPCODE;
		opCode = null;
	}

}
