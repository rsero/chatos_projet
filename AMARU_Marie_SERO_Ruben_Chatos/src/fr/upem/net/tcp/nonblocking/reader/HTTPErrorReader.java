package fr.upem.net.tcp.nonblocking.reader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import fr.upem.net.http.exception.HTTPException;
import fr.upem.net.tcp.nonblocking.data.HTTPError;

public class HTTPErrorReader  implements ReaderHTTP<HTTPError> {

	private enum State {
		DONE, WAITING_FILENAME, ERROR
	};

	private State state = State.WAITING_FILENAME;
	private int errorNumber = 0;
	private String file;
	//"HTTP/1.1 404 Not Found\r\nErrorDocument 404 /"+file+"\r\n\r\n";

	public HTTPErrorReader(String firstLine) {
		var elements = firstLine.split(" ");
		errorNumber = Integer.parseInt(elements[1]);
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

	public void parseFileName(String str){
		var firstPart = str.split("/");
		file = firstPart[1];
	}

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

	@Override
	public HTTPError get() throws IOException {
		if (state != State.DONE) {
			throw new IllegalStateException("careful");
		}
		return new HTTPError(file);
	}

	@Override
	public void reset() {
		state = State.WAITING_FILENAME;
		errorNumber = 0;
		file = "";
	}
}

