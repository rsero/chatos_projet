package fr.upem.net.tcp.nonblocking.data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

public class PrivateConnectionTransmission implements Data  {

	private final ByteBuffer bbin;
	private final SelectionKey key;

	public PrivateConnectionTransmission(ByteBuffer bbin, SelectionKey key) {
		this.bbin = bbin;
		this.key = key;
	}

	public ByteBuffer encode(){
		return bbin;
	}

	@Override
	public void accept(DataVisitor visitor) throws IOException { visitor.visit(this); }

	public SelectionKey getKey(){
		return key;
	}

}
