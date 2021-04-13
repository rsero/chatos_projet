package fr.upem.net.tcp.nonblocking.data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import fr.upem.net.tcp.nonblocking.client.ClientChatos;
import fr.upem.net.tcp.nonblocking.server.ContextServer;
import fr.upem.net.tcp.nonblocking.server.ServerChatos;

public class PrivateLogin implements Data {

	private final Long connectId;
	
	public PrivateLogin(Long connectId) {
		this.connectId = connectId;
	}
	
	public ByteBuffer encode(ByteBuffer req) throws IOException {
    	req.clear();
        if(req.remaining() < Long.BYTES + 1) {
            return null;
        }
        req.put((byte) 9).putLong(connectId);
        return req;
    }
	
	public ByteBuffer encodeResponse(ByteBuffer req) {
    	req.clear();
        if(req.remaining() < 1) {
            return null;
        }
        req.put((byte) 10);
        return req;
    }

	@Override
	public boolean processOut(ByteBuffer bbout, ContextServer context, ServerChatos server) throws IOException {
		var bb = encodeResponse(bbout);
		return bb != null;
	}

	@Override
	public void decode(ClientChatos server, SelectionKey key) {
	}

	@Override
	public void broadcast(Selector selector, ContextServer context, SelectionKey key) throws IOException {
		context.updatePrivateConnexion(connectId, key);
		if(!context.connectionReady(connectId))
			return;
		var contexts = context.findContext(connectId);
        ((ContextServer) contexts.get(0).attachment()).queueMessage(this);
        ((ContextServer) contexts.get(1).attachment()).queueMessage(this);
	}
}
