package fr.upem.net.tcp.nonblocking.data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Objects;

import fr.upem.net.tcp.nonblocking.client.Context;
import fr.upem.net.tcp.nonblocking.server.ServerChatos;

public class Login implements Data {
	
	/**
	 * Charset of the encoding
	 */
	private static final Charset UTF8 = Charset.forName("UTF8");
	/**
	 * Maximum buffer capacity
	 */
	private static final int BUFFER_SIZE = 34;
	/**
	 * Name of the person
	 */
	private final String name;

	/**
	 * Create an object to record a person's name
	 */
	public Login() { this.name = ""; }

	/**
	 * Create an object to record a person's name
	 * @param name Name of the person
	 */
	public Login(String name) {
		this.name = Objects.requireNonNull(name);
	}

	/**
	 * Give the name of the person
	 * @return Name of the person
	 */
	public String getLogin() {
		return name;
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Login)) {
			return false;
		}
		Login other = (Login) obj;
		return name.equals(other.getLogin());
	}

	/**
	 * Check if a login is given
	 * @return Return true if a login is given
	 */
	public boolean isNotConnected() {
		return name.equals("");
	}

	/**
	 * Adds a client to the server
	 * @param context Client context
	 * @param server Main server
	 * @return Return true if the client could be added
	 */
	public boolean processOut(Context context, ServerChatos server) {
		if (server.addClient(name, context)) {
			return true;
		}
		return false;
	}

	@Override
	public String toString() {
		return name;
	}

	/**
	 * Encodes the opCode to send it to the server
	 * @param opCode Opcode to send to the server
	 * @return Encoded frame containing the opCode
	 */
	public ByteBuffer encode(Byte opCode){
		var req = ByteBuffer.allocate(Byte.BYTES);
		req.put(opCode);
		return req;
	}

	/**
	 * Encodes the frame requesting the connection to the main server
	 * @param log Login requested by a client trying to connect
	 * @return Encoded frame for the connection
	 */
	public ByteBuffer encodeLogin(String log) {
		var req = ByteBuffer.allocate(BUFFER_SIZE);
		var loginbuff = UTF8.encode(log);
		int len = loginbuff.remaining();
		if (BUFFER_SIZE < len + Integer.BYTES + 1) {
			return null;
		}
		req.put((byte) 0).putInt(len).put(loginbuff);
		return req;
	}

	@Override
	public void accept(DataVisitor visitor) throws IOException { visitor.visit(this); }

}
