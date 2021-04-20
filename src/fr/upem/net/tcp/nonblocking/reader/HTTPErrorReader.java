package fr.upem.net.tcp.nonblocking.reader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import fr.upem.net.http.exception.HTTPException;
import fr.upem.net.tcp.nonblocking.data.HTTPError;
/**
 * Represents a reader that produces an HTTP Error data
 */
public class HTTPErrorReader  implements Reader<HTTPError> {
	/**
	 * Different states the reader can be in
	 */
	private enum State {
		DONE, WAITING_FILENAME, ERROR
	}

	private State state = State.WAITING_FILENAME;
	private int errorNumber = 0;
	private String file;

	/**
	 * Constructor for the reader that parses the error number
	 * @param firstLine to parse
	 */
	public HTTPErrorReader(String firstLine) {
		var elements = firstLine.split(" ");
		errorNumber = Integer.parseInt(elements[1]);
	}

	/**
	 * @param buff the bytebuffer to fill with the content
	 * @param sc the socketchannel to perform the read from
	 * The method assume that buff is in write mode and leaves it in
	 * write-mode The method does perform a read from the socket if the
	 * buffer data. The will process the data from the buffer if
	 * necessary will read from the socket.
	 * @throws IOException HTTPException is the connection is closed before all
	 *                     bytes could be read
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
	 * Parses the filename passed to remove the '/' before the name of the file
	 * @param str
	 */
	public void parseFileName(String str){
		var firstPart = str.split("/");
		file = firstPart[1];
	}

	/**
	 * Reads the ByteBuffer bb passed
	 * @param key
	 * @param bb
	 * @return ProcessStatus.REFILL if the value is not the size of an integer,
	 * and ProcessStatus.DONE if all the content was processed
	 * @throws IllegalStateException if the state is DONE or ERROR at the beginning
	 */
	@Override
	public ProcessStatus process(ByteBuffer bb, SelectionKey key) throws IOException {
		if (state == State.DONE || state == State.ERROR) {
			throw new IllegalStateException();
		}
		var sc = (SocketChannel) key.channel();
		switch (state) {
			case WAITING_FILENAME:
				var line = readLineCRLF(bb, sc);
				parseFileName(line);
				state = State.DONE;
				bb.clear();
				return ProcessStatus.DONE;
			default:
				throw new IllegalStateException();
		}
	}

	/**
	 * Gets the Data that have been processed previously
	 * @return a HTTPError object
	 * @throws IllegalStateException if the state is not DONE
	 */
	@Override
	public HTTPError get() throws IOException {
		if (state != State.DONE) {
			throw new IllegalStateException("careful");
		}
		return new HTTPError(file);
	}

	/**
	 * Resets the reader
	 */
	@Override
	public void reset() {
		state = State.WAITING_FILENAME;
		errorNumber = 0;
		file = "";
	}
}

