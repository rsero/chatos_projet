package fr.upem.net.tcp.nonblocking.data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import fr.upem.net.tcp.nonblocking.client.ClientChatos;
import fr.upem.net.tcp.nonblocking.server.ContextServer;
import fr.upem.net.tcp.nonblocking.server.ServerChatos;

public class PrivateConnexionTransmission implements Data  {

	private final ByteBuffer bbin;

	public PrivateConnexionTransmission(ByteBuffer bbin) {
		this.bbin = bbin;
	}

	@Override
	public boolean processOut(ByteBuffer bbout, ContextServer context, ServerChatos server)
			throws IOException {
		bbout.clear();
		if(bbin.remaining() < bbout.remaining()) {
			return false;
		}
		bbout.put(bbin);
		return true;
	}

	@Override
	public void decode(ClientChatos server, SelectionKey key) throws IOException {

	}

	@Override
	public void broadcast(Selector selector, ContextServer context, SelectionKey key) throws IOException {
		var keyTarget = context.findKeyTarget(key);
		((ContextServer) keyTarget.attachment()).queueMessage(this);
	}

	public ByteBuffer getbb(){
		return bbin;
	}

}
