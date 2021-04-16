package fr.upem.net.tcp.nonblocking.data;

import java.io.IOException;
import java.nio.channels.SelectionKey;

public class HTTPRequest implements Data {

	private final String file;
	private SelectionKey key;

	public HTTPRequest(String file, SelectionKey key) {
		this.file = file;
		this.key = key;
	}

	@Override
	public void accept(DataVisitor visitor) throws IOException { visitor.visit(this); }

	public String getFile() {
		return file;
	}

	public SelectionKey getKey() {
		return key;
	}
}