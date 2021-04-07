package fr.upem.net.tcp.nonblocking.server.reader;

import java.io.IOException;
import java.nio.ByteBuffer;

import fr.upem.net.tcp.nonblocking.server.data.HTTPRequest;

public class HTTPRequestReader implements Reader<HTTPRequest>{

	private enum State {
		DONE, WAITING_FIRST_LINE, WAITING_SECOND_LINE, WAITING_END, ERROR
	};


	private State state = State.WAITING_FIRST_LINE;
	private String file;
	private String directory;
	private String path;

	public String readLineCRLF(ByteBuffer buff) throws IOException {
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
	public ProcessStatus process(ByteBuffer bb) throws IOException {
		if (state == State.DONE || state == State.ERROR) {
			throw new IllegalStateException();
		}
		switch (state) {
			case WAITING_FIRST_LINE:
				var firstLine = readLineCRLF(bb);
				parseFirstLine(firstLine);
				state = State.WAITING_SECOND_LINE;
			case WAITING_SECOND_LINE:
				var secondLine = readLineCRLF(bb);
				parseSecondLine(secondLine);
				state = State.WAITING_END;
			case WAITING_END:
				var lastLine = readLineCRLF(bb);
				if (!lastLine.isEmpty()){
					return ProcessStatus.ERROR;
				}
				path = directory+"/"+file;
				state = State.DONE;
				return ProcessStatus.DONE;
			default:
				throw new IllegalStateException();
		}
	}

	@Override
	public HTTPRequest get() throws IOException {
		if (state != State.DONE) {
			throw new IllegalStateException("careful");
		}
		return new HTTPRequest(path);
	}

	@Override
	public void reset() {
		state = State.WAITING_FIRST_LINE;
	}

}

