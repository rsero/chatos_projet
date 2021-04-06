package fr.upem.net.tcp.nonblocking.server.data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import fr.upem.net.tcp.nonblocking.client.ClientChatos;
import fr.upem.net.tcp.nonblocking.server.ContextServer;
import fr.upem.net.tcp.nonblocking.server.ServerChatos;

public class PrivateLogin implements Data {

	private final Long connectId;
	
	public PrivateLogin(Long connectId) throws IOException {
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
	
	public ByteBuffer encodeResponse(ByteBuffer req) throws IOException {
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
		
    	if (bb==null) {
    		return false;
    	}    	
    	return true;
	}

	@Override
	public void decode(ClientChatos server, SelectionKey key) {
		System.out.println("Private login : connection established");
	}

	@Override
	public void broadcast(Selector selector, ContextServer context, SelectionKey key) throws IOException {
		context.updatePrivateConnexion(connectId, key);
		System.out.println("broadcast entre");
		if(!context.connectionReady(connectId))
			return;
		System.out.println("broadcast sort");
		var contexts = context.findContext(connectId);
        ((ContextServer) contexts.get(0).attachment()).queueMessage(this);
        ((ContextServer) contexts.get(1).attachment()).queueMessage(this);
	}
}
