package fr.upem.net.tcp.nonblocking.reader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import fr.upem.net.http.exception.HTTPException;
import fr.upem.net.tcp.nonblocking.data.HTTPFile;
/**
 * Represents a reader that produces an HTTP File data
 */
public class HTTPFileReader implements Reader<HTTPFile> {
	/**
	 * Different states the reader can be in
	 */
	private enum State {
		DONE, WAITING_CONTENT_LENGTH, WAITING_CONTENT_TYPE, WAITING_FILENAME, WAITING_EMPTY_LINE, WAITING_DATA, ERROR
	}

	private State state = State.WAITING_CONTENT_LENGTH;
	private int content_length;
	private String content_type;
	private int byteRead = 0;
	private ByteBuffer buffRead;
	private String nameFile;

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
	 * @param buff the bytebuffer to fill with the content
	 * @param sc the socketchannel to perform the read from
	 * The method assume that buff is in write mode and leaves it in
	 * write-mode The method does perform a read from the socket if the
	 * buffer data. The will process the data from the buffer if
	 * necessary will read from the socket.
	 * @throws IOException HTTPException is the connection is closed before all
	 *                     bytes could be read
	 */
	public void readMessage(ByteBuffer buff, SocketChannel sc) throws IOException {
		buffRead = ByteBuffer.allocate(content_length);
		buffRead.clear();
		boolean end = false;
		while (!end) {
			buff.flip();
			while (buff.hasRemaining()) {
				var bytes = buff.get();
				buffRead.put(bytes);
				byteRead++;
				if (content_length == byteRead) {
					end = true;
					break;
				}
			}
			buff.compact();
			if (!end) {
				HTTPException.ensure(sc.read(buff) != -1, "The connection is closed");
			}
		}
	}

	/**
	 * Parses the content length value in the header to the file received
	 * @param str
	 */
	public void parseContentLength(String str) {
		content_length = Integer.parseInt(str.replaceFirst("Content-Length: ", ""));
	}

	/**
	 * Parses the content type value in the header to the file received
	 * @param str
	 */
	public void parseContentType(String str) {
		content_type = str.replaceFirst("Content-Type: ", "");
	}

	/**
	 * Parses the name of the file in the header to the file received
	 * @param str
	 */
	public void parseNameFile(String str) {
		nameFile = str.replaceFirst("Name-File: ", "");
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
			case WAITING_CONTENT_LENGTH:
				var secondLine = readLineCRLF(bb, sc);
				parseContentLength(secondLine);
				state = State.WAITING_CONTENT_TYPE;
			case WAITING_CONTENT_TYPE:
				var thirdLine = readLineCRLF(bb, sc);
				parseContentType(thirdLine);
				state = State.WAITING_FILENAME;
			case WAITING_FILENAME:
				var fourthLine = readLineCRLF(bb, sc);
				parseNameFile(fourthLine);
				state = State.WAITING_EMPTY_LINE;
			case WAITING_EMPTY_LINE:
				readLineCRLF(bb, sc);
				state = State.WAITING_DATA;
			case WAITING_DATA:
				readMessage(bb, sc);
				state = State.DONE;
				bb.clear();
				return ProcessStatus.DONE;
			default:
				throw new IllegalStateException();
		}
	}

	/**
	 * Gets the Data that have been processed previously
	 * @return an HTTP File object
	 * @throws IllegalStateException if the state is not DONE
	 */
	@Override
	public HTTPFile get() throws IOException {
		if (state != State.DONE) {
			throw new IllegalStateException();
		}
		return new HTTPFile(content_type, buffRead, nameFile);
	}

	/**
	 * Resets the reader
	 */
	@Override
	public void reset() {
		state = State.WAITING_CONTENT_LENGTH;
		byteRead = 0;
		buffRead.clear();
	}

}
