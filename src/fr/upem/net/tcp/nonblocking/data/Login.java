package fr.upem.net.tcp.nonblocking.data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Objects;

import fr.upem.net.tcp.nonblocking.client.Context;
import fr.upem.net.tcp.nonblocking.server.ServerChatos;

public class Login implements Data {
	private static final Charset UTF8 = Charset.forName("UTF8");
	private static final int BUFFER_SIZE = 34;
	private final String name;

	public Login() { this.name = ""; }

	public Login(String name) {
		this.name = Objects.requireNonNull(name);
	}

	public String getLogin() {
		return name;
	}

	public int hashCode() {
		return Objects.hash(name);
	}

	public boolean equals(Object obj) {
		if (!(obj instanceof Login)) {
			return false;
		}
		Login other = (Login) obj;
		return name.equals(other.getLogin());
	}

	public boolean isNotConnected() {
		return name.equals("");
	}

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

	public ByteBuffer encode(Byte opCode){
		var req = ByteBuffer.allocate(Byte.BYTES);
		req.put(opCode);
		return req;
	}

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
