package fr.upem.net.tcp.nonblocking.reader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import fr.upem.net.http.exception.HTTPException;
import fr.upem.net.tcp.nonblocking.data.HTTPFile;

public class HTTPFileReader implements ReaderHTTP<HTTPFile> {

	private enum State {
		DONE, WAITING_CONTENTLENGTH, WAITING_CONTENTTYPE, WAITING_FILENAME, WAITING_EMPTYLINE, WAITING_DATA, WAITING_END,
		ERROR
	};

	private State state = State.WAITING_CONTENTLENGTH;
	private int content_length;
	private String content_type;
	private int byteRead = 0;
	private ByteBuffer buffRead;
	private String nameFile;

	public HTTPFileReader(String firstLine) {
		state = State.WAITING_CONTENTLENGTH;
	}

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

	public void parseContentLength(String str) {
		content_length = Integer.parseInt(str.replaceFirst("Content-Length: ", ""));
	}

	public void parseContentType(String str) {
		content_type = str.replaceFirst("Content-Type: ", "");
	}

	public void parseNameFile(String str) {
		nameFile = str.replaceFirst("Name-File: ", "");
	}

	@Override
	public ProcessStatus process(ByteBuffer bb, SelectionKey key) throws IOException {
		if (state == State.DONE || state == State.ERROR) {
			throw new IllegalStateException();
		}
		var sc = (SocketChannel) key.channel();
		switch (state) {
			case WAITING_CONTENTLENGTH:
				var secondLine = readLineCRLF(bb, sc);
				parseContentLength(secondLine);
				state = State.WAITING_CONTENTTYPE;
			case WAITING_CONTENTTYPE:
				var thirdLine = readLineCRLF(bb, sc);
				parseContentType(thirdLine);
				state = State.WAITING_FILENAME;
			case WAITING_FILENAME:
				var fourthLine = readLineCRLF(bb, sc);
				parseNameFile(fourthLine);
				state = State.WAITING_EMPTYLINE;
			case WAITING_EMPTYLINE:
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

	@Override
	public HTTPFile get() throws IOException {
		if (state != State.DONE) {
			throw new IllegalStateException();
		}
		return new HTTPFile(content_type, buffRead, nameFile);
	}

	@Override
	public void reset() {
		state = State.WAITING_CONTENTLENGTH;
		byteRead = 0;
		buffRead.clear();
	}

}
