package fr.upem.net.tcp.nonblocking.data;

import java.io.IOException;
import java.nio.ByteBuffer;

public class PrivateLogin implements Data {

	/**
	 * Connection password
	 */
	private final Long connectId;
	/**
     * Maximum buffer capacity
     */
	private static final int BUFFER_SIZE = 1024;
	
	/**
	 * Create an object to initiate the private connection
	 * @param connectId Connection password
	 */
	public PrivateLogin(Long connectId) {
		this.connectId = connectId;
	}

	/**
	 * Give the password of the connection
	 * @return The password of the connection
	 */
	public Long getConnectId() {
		return connectId;
	}

	/**
	 * Encodes the frame allowing the client to connect with the sent password
	 * @return The frame allowing the client to connect with the sent password
	 */
	public ByteBuffer encode() {
    	var req = ByteBuffer.allocate(BUFFER_SIZE);
        if(req.remaining() < Long.BYTES + 1) {
            return null;
        }
        req.put((byte) 9).putLong(connectId);
        return req;
    }
	
	/**
	 * Encodes the frame informing that the private connection has been established after receiving the two private connections from the clients
	 * @return
	 */
	public ByteBuffer encodeResponse() {
		var req = ByteBuffer.allocate(BUFFER_SIZE);
        if(req.remaining() < 1) {
            return null;
        }
        req.put((byte) 10);
        return req;
    }

	@Override
	public void accept(DataVisitor visitor) throws IOException {
		visitor.visit(this);
	}
}
