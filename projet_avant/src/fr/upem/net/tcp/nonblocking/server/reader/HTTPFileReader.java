package fr.upem.net.tcp.nonblocking.server.reader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import fr.upem.net.http.exception.HTTPException;
import fr.upem.net.tcp.nonblocking.server.data.HTTPFile;

public class HTTPFileReader implements ReaderHTTP<HTTPFile> {

	private enum State {
		DONE, WAITING_CONTENT_LENGTH, WAITING_CONTENT_TYPE, WAITING_FILENAME, WAITING_EMPTY_LINE, WAITING_DATA,
		ERROR
	}

	private State state = State.WAITING_CONTENT_LENGTH;
	private int content_length;
	private int byteRead = 0;
	private ByteBuffer buffRead;
	private String nameFile;

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
					System.out.println("Byte read" + byteRead);
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
		case WAITING_CONTENT_LENGTH:
			var secondLine = readLineCRLF(bb, sc);
			parseContentLength(secondLine);
			state = State.WAITING_CONTENT_TYPE;
		case WAITING_CONTENT_TYPE:
			readLineCRLF(bb, sc);
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

	@Override
	public HTTPFile get() throws IOException {
		if (state != State.DONE) {
			throw new IllegalStateException();
		}
		return new HTTPFile(buffRead, nameFile);
	}

	@Override
	public void reset() {
		state = State.WAITING_CONTENT_LENGTH;
		byteRead = 0;
		buffRead.clear();
	}

}
