package fr.upem.net.tcp.nonblocking.data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import fr.upem.net.tcp.nonblocking.client.ClientChatos;
import fr.upem.net.tcp.nonblocking.client.Context;
import fr.upem.net.tcp.nonblocking.server.ContextServer;
import fr.upem.net.tcp.nonblocking.server.ServerChatos;

public class PrivateLogin implements Data {

	private final Long connectId;
	private static final int BUFFER_SIZE = 1024;
	
	public PrivateLogin(Long connectId) {
		this.connectId = connectId;
	}

	public Long getConnectId() {
		return connectId;
	}

	public ByteBuffer encode(ByteBuffer req) throws IOException {
    	req.clear();
        if(req.remaining() < Long.BYTES + 1) {
            return null;
        }
        System.out.println("client envoie 9");
        req.put((byte) 9).putLong(connectId);
        return req;
    }
	
	public ByteBuffer encodeResponse() {
		var req = ByteBuffer.allocate(BUFFER_SIZE);
        if(req.remaining() < 1) {
            return null;
        }
		System.out.println("serveur envoie 10");
        req.put((byte) 10);
        return req;
    }

	/*@Override
	public boolean processOut(ContextServer context, ServerChatos server) throws IOException {
		//var bb = encodeResponse(bbout);
		//return bb != null;
		return true;
	}
*/
	@Override
	public void accept(DataClientVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public void accept(DataServerVisitor visitor, Context context) throws IOException {
		visitor.visit(this, context);
	}
}
