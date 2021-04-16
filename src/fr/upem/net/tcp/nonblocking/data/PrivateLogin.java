package fr.upem.net.tcp.nonblocking.data;

import java.io.IOException;
import java.nio.ByteBuffer;

import fr.upem.net.tcp.nonblocking.client.Context;

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

	@Override
	public void accept(DataVisitor visitor) throws IOException {
		visitor.visit(this);
	}

}
