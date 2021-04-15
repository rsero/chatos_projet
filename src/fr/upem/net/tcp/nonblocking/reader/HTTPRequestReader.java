package fr.upem.net.tcp.nonblocking.reader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import fr.upem.net.http.exception.HTTPException;
import fr.upem.net.tcp.nonblocking.data.HTTPRequest;

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

	public void parseFirstLine(String str){
		var firstPart = str.split("/");
		var secondPart = firstPart[1].split(" ");
		file = secondPart[0];
	}

	public void parseSecondLine(String str){
		directory = str.replaceFirst("Host: ","");
	}

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

	@Override
	public HTTPRequest get() throws IOException {
		if (state != State.DONE) {
			throw new IllegalStateException();
		}
		return new HTTPRequest(file, key);
	}

	@Override
	public void reset() {
		state = State.WAITING_SECOND_LINE;
	}

}

