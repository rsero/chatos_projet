package fr.upem.net.tcp.nonblocking.server.reader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import fr.upem.net.tcp.nonblocking.server.data.HTTPRequest;

public class HTTPReader implements Reader<HTTPRequest>{

	private enum State {
        DONE, WAITING_FIRTLINE, WAITING_SECONDLINE, WAITING_END, ERROR
    };
    
    private State state = State.WAITING_FIRTLINE;
    private String file;
    
    private State updateState() {
    	if(state == State.WAITING_FIRTLINE)
    		return State.WAITING_SECONDLINE;
    	if(state == State.WAITING_SECONDLINE)
    		return State.WAITING_END;
    	return State.DONE;
    }

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
    
    private ProcessStatus returnState() {
    	if(state == State.WAITING_FIRTLINE)
    		return ProcessStatus.REFILL;
    	if(state == State.WAITING_SECONDLINE)
    		return ProcessStatus.REFILL;
    	if(state == State.WAITING_END)
    		return ProcessStatus.REFILL;
    	return ProcessStatus.DONE;
    }
    
	@Override
	public ProcessStatus process(ByteBuffer bb) throws IOException {
		if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }
		bb.flip();
		var str = Charset.forName("ASCII").decode(bb).toString();
		var line = str.split("\r\n");
		for(var i = 0; i < line.length; i++) {
			state = updateState();
			if(state == State.WAITING_END) {
				analyzeString(line[i]);
			}
		}
		return returnState();
	}

	private void analyzeString(String line) {
		file = line.replaceFirst("Host: ", "");
	}

	@Override
	public HTTPRequest get() throws IOException {
		if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return new HTTPRequest(file);
    }

    @Override
    public void reset() {
        state = State.WAITING_FIRTLINE;
    }

}
