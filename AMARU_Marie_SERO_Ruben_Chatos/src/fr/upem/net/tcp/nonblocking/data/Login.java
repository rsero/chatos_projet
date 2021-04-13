package fr.upem.net.tcp.nonblocking.data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.Charset;
import java.util.Objects;

import fr.upem.net.tcp.nonblocking.client.ClientChatos;
import fr.upem.net.tcp.nonblocking.server.ContextServer;
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

	@Override
	public boolean processOut(ByteBuffer bbout, ContextServer context, ServerChatos server) {
		if (!bbout.hasRemaining()) {
			return false;
		}
		if (server.addClient(name, context)) {
			bbout.put((byte) 1);
		} else {
			bbout.put((byte) 2);
		}
		return true;
	}

	@Override
	public String toString() {
		return name;
	}

	public ByteBuffer encodeLogin(String log) {
		var req = ByteBuffer.allocate(BUFFER_SIZE);
		var loginbuff = UTF8.encode(log);
		int len = loginbuff.remaining();
		if (BUFFER_SIZE < len + Integer.BYTES + 1) {
			return null;
		}
		req.put((byte) 0);
		req.putInt(len);
		req.put(loginbuff);
		return req;
	}

	@Override
	public void decode(ClientChatos client, SelectionKey key) {
	}

	@Override
	public void broadcast(Selector selector, ContextServer context, SelectionKey key) throws IOException {
		context.queueMessage(this);
	}

}
