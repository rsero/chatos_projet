package fr.upem.net.tcp.nonblocking.server.reader;

import java.io.IOException;
import java.nio.ByteBuffer;

import fr.upem.net.tcp.nonblocking.server.data.HTTPRequest;

public class HTTPReader implements Reader<HTTPRequest>{

	@Override
	public ProcessStatus process(ByteBuffer bb) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HTTPRequest get() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub
		
	}

}
