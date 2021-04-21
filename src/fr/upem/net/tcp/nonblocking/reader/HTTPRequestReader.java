package fr.upem.net.tcp.nonblocking.reader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import fr.upem.net.http.exception.HTTPException;
import fr.upem.net.tcp.nonblocking.data.HTTPRequest;
/**
 * Represents a reader that produces an HTTP Request data
 */
public class HTTPRequestReader implements Reader<HTTPRequest>{

	private enum State {
		DONE, WAITING_SECOND_LINE, WAITING_END, ERROR
	};


	private State state = State.WAITING_SECOND_LINE;
	private String file;
	private String directory;
	private String path;
	private SelectionKey key;

	public HTTPRequestReader(String firstLine, SelectionKey key) {
		parseFirstLine(firstLine);
		this.key=key;
		state = State.WAITING_SECOND_LINE;
	}

	/**
	 * @param buff the bytebuffer to fill with the content
	 * @param sc the socketchannel to perform the read from
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
	 * Parses the string value which is a path to set the file variable
	 * with the name of the file
	 * @param str the first line of the GET request
	 */
	public void parseFirstLine(String str){
		var firstPart = str.split("/");
		var secondPart = firstPart[1].split(" ");
		file = secondPart[0];
	}

	/**
	 * Parses the string value which is a path to set the directory variable
	 * with the path to the directory
	 * @param str the second line of the GET request
	 */
	public void parseSecondLine(String str){
		directory = str.replaceFirst("Host: ","");
	}

	/**
	 * Reads the ByteBuffer bb passed
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
			case WAITING_SECOND_LINE:
				var secondLine = readLineCRLF(bb, sc);
				parseSecondLine(secondLine);
				state = State.WAITING_END;
			case WAITING_END:
				var lastLine = readLineCRLF(bb, sc);
				path = directory+"/"+file;
				state = State.DONE;
				bb.clear();
				return ProcessStatus.DONE;
			default:
				throw new IllegalStateException();
		}
	}

	/**
	 * Gets the Data that have been processed previously
	 * @return a HTTP Request object
	 * @throws IllegalStateException if the state is not DONE
	 */
	@Override
	public HTTPRequest get() throws IOException {
		if (state != State.DONE) {
			throw new IllegalStateException();
		}
		return new HTTPRequest(file, key);
	}

	/**
	 * Resets the reader
	 */
	@Override
	public void reset() {
		state = State.WAITING_SECOND_LINE;
	}

}

