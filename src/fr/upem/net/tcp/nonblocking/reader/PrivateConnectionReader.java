package fr.upem.net.tcp.nonblocking.reader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import fr.upem.net.http.exception.HTTPException;
import fr.upem.net.tcp.nonblocking.data.Data;

public class PrivateConnectionReader implements Reader<Data>{

	private enum State {
		DONE, WAITING_OPCODE, WAITING_FIRST_LINE, ERROR
	}

	private Reader<?> reader;

	private State state = State.WAITING_OPCODE;
	private final ByteReader byteReader = new ByteReader();

	public String readLineCRLF(ByteBuffer buff, SocketChannel sc) throws IOException {
		var end = false;
		var sb = new StringBuilder();
		byte str;
		char lastchar = 0;
		while (!end) {
			buff.flip();
			while (buff.hasRemaining()) {
				str = buff.get();
				if (str == '\n') {
					if (lastchar == '\r') {
						end = true;
						break;
					}
				}
				sb.append((char) str);
				lastchar = (char) str;
			}
			buff.compact();
			if (!end) {
				HTTPException.ensure(sc.read(buff) != -1, "The connection is closed");
			}
		}
		return sb.substring(0, sb.length() - 1);
	}

	@Override
	public ProcessStatus process(ByteBuffer bb, SelectionKey key) throws IOException {
		if (state == State.DONE || state == State.ERROR) {
			throw new IllegalStateException();
		}
		var sc = (SocketChannel) key.channel();
		switch (state) {
			case WAITING_OPCODE:
				var status = byteReader.process(bb, key);
				if(status!=ProcessStatus.DONE){
					return status;
				}
				var opCode = byteReader.get();
				if(opCode.getByte() == (byte) 10){
					reader = byteReader;
					state = State.DONE;
					return ProcessStatus.DONE;
				}
				state = State.WAITING_FIRST_LINE;
			case WAITING_FIRST_LINE:
				var firstLine = readLineCRLF(bb, sc);
				if(firstLine.startsWith("GET")) {
					reader = new HTTPRequestReader(firstLine, key);
				}
				else if(firstLine.startsWith("HTTP/1.1 200")) {
					reader = new HTTPFileReader(firstLine);
				}
				else {
					reader = new HTTPErrorReader(firstLine);
				}
				reader.process(bb, key);
				bb.clear();
				state = State.DONE;
				return ProcessStatus.DONE;
			default:
				throw new IllegalStateException();
		}
	}

	@Override
	public Data get() throws IOException {
		if (state != State.DONE) {
			throw new IllegalStateException();
		}
		return (Data) reader.get();
	}

	@Override
	public void reset() {
		reader.reset();
		state = State.WAITING_FIRST_LINE;
	}
}
