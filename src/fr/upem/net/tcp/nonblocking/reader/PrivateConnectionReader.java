package fr.upem.net.tcp.nonblocking.reader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import fr.upem.net.http.exception.HTTPException;
import fr.upem.net.tcp.nonblocking.data.Data;
import fr.upem.net.tcp.nonblocking.data.OpCode;
/**
 * Represents a reader that produces a Data object
 */
public class PrivateConnectionReader implements Reader<Data>{
	/**
	 * Different states the reader can be in
	 */
	private enum State {
		DONE, WAITING_OPCODE, WAITING_FIRST_LINE, ERROR
	}

	private Reader<?> reader;
	private OpCode opCode;
	private State state = State.WAITING_OPCODE;
	private final ByteReader byteReader = new ByteReader();

	/**
	 * @return The ASCII string terminated by CRLF without the CRLF
	 *         <p>
	 *         The method assume that buff is in write mode and leaves it in
	 *         write-mode The method does perform a read from the socket if the
	 *         buffer data. Then will process the data from the buffer if necessary
	 *         will read from the socket.
	 * @throws IOException HTTPException if the connection is closed before a line
	 *                     could be read
	 */
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

	/**
	 * Reads the ByteBuffer bb passed and assigns the right HTTP reader according to the first line read
	 * @param key
	 * @param bb
	 * @return ProcessStatus.REFILL if some content is missing, ProcessStatus.ERROR if an error
	 * occurred and ProcessStatus.DONE if all the content was processed
	 * @throws IllegalStateException if the state is DONE or ERROR at the beginning
	 */
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
				opCode = byteReader.get();
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
					reader = new HTTPFileReader();
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

	/**
	 * Gets the Data that have been processed previously
	 * @return a Data object
	 * @throws IllegalStateException if the state is not DONE
	 */
	@Override
	public Data get() throws IOException {
		if (state != State.DONE) {
			throw new IllegalStateException();
		}
		return (Data) reader.get();
	}

	/**
	 * Resets the reader
	 */
	@Override
	public void reset() {
		reader.reset();
		if(opCode.getByte() == (byte) 10){
			state=State.WAITING_FIRST_LINE;
		} else {
			state = State.WAITING_OPCODE;
		}
	}
}
