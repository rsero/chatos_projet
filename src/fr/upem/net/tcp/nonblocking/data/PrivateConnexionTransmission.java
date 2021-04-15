package fr.upem.net.tcp.nonblocking.data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import fr.upem.net.tcp.nonblocking.client.ClientChatos;
import fr.upem.net.tcp.nonblocking.client.Context;
import fr.upem.net.tcp.nonblocking.server.ContextServer;
import fr.upem.net.tcp.nonblocking.server.ServerChatos;

public class PrivateConnexionTransmission implements Data  {

	private final ByteBuffer bbin;
	private final SelectionKey key;

	public PrivateConnexionTransmission(ByteBuffer bbin, SelectionKey key) {
		this.bbin = bbin;
		this.key=key;
	}

	/*@Override
	public boolean processOut(ContextServer context, ServerChatos server)
			throws IOException {
		bbout.clear();
		if(bbin.remaining() < bbout.remaining()) {
			return false;
		}
		bbout.put(bbin);


		return true;
	}
*/
	public ByteBuffer encode(){
		return bbin;
	}

	@Override
	public void accept(DataClientVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public void accept(DataServerVisitor visitor, Context context) throws IOException {
		visitor.visit(this, context);
	}

	public SelectionKey getKey(){
		return key;
	}

}
