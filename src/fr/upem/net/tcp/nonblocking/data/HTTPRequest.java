package fr.upem.net.tcp.nonblocking.data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import fr.upem.net.http.server.HTTPServer;
import fr.upem.net.tcp.nonblocking.client.ClientChatos;
import fr.upem.net.tcp.nonblocking.server.ContextServer;
import fr.upem.net.tcp.nonblocking.server.ServerChatos;

public class HTTPRequest implements Data {

	private final String file;
	private SelectionKey key;

	public HTTPRequest(String file, SelectionKey key) {
		this.file = file;
		this.key = key;
	}

	@Override
	public boolean processOut(ByteBuffer bbout, ContextServer context, ServerChatos server)
			throws IOException {
		return false;
	}
	/*
	@Override
		public void decode(ClientChatos client) throws IOException {
		new HTTPServer(file, key, client.getDirectory()).serve();
	}
	*/
	@Override
	public void accept(DataClientVisitor visitor) throws IOException { visitor.visit(this);
	}

	@Override
	public void accept(DataServerVisitor visitor) { visitor.visit(this); }

	public String getFile() {
		return file;
	}

	public SelectionKey getKey() {
		return key;
	}
}