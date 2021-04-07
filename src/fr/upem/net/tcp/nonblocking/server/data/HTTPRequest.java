package fr.upem.net.tcp.nonblocking.server.data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import fr.upem.net.tcp.nonblocking.client.ClientChatos;
import fr.upem.net.tcp.nonblocking.server.ContextServer;
import fr.upem.net.tcp.nonblocking.server.ServerChatos;

public class HTTPRequest implements Data {

	private final String file;

	public HTTPRequest(String file) {
		this.file = file;
	}

	@Override
	public boolean processOut(ByteBuffer bbout, ContextServer context, ServerChatos server)
			throws IOException {
		return false;
	}

	@Override
	public void decode(ClientChatos server, SelectionKey key) throws IOException {
		System.out.println(file);
	}

	@Override
	public void broadcast(Selector selector, ContextServer context, SelectionKey key) throws IOException {
		// TODO Auto-generated method stub

	}

	public String getFile() {
		return file;
	}

}