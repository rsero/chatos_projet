package fr.upem.net.tcp.nonblocking.data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

import fr.upem.net.tcp.nonblocking.client.Context;

public class PrivateConnexionTransmission implements Data  {

	private final ByteBuffer bbin;
	private final SelectionKey key;

	public PrivateConnexionTransmission(ByteBuffer bbin, SelectionKey key) {
		this.bbin = bbin;
		this.key=key;
	}

	public ByteBuffer encode(){
		return bbin;
	}

	@Override
	public void accept(DataVisitor visitor) throws IOException {
		visitor.visit(this);
	}

	public SelectionKey getKey(){
		return key;
	}

}
