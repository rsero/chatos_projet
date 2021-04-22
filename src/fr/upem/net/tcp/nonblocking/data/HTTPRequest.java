package fr.upem.net.tcp.nonblocking.data;

import java.io.IOException;
import java.nio.channels.SelectionKey;

public class HTTPRequest implements Data {

	/**
	 * Name of the file in the GET request
	 */
	private final String file;
	/**
	 * Key of the client sending the GET request
	 */
	private SelectionKey key;

	/**
	 * Builds an object containing the information of a client sending a GET request
	 * @param file Name of the file in the GET request
	 * @param key Key of the client sending the GET request
	 */
	public HTTPRequest(String file, SelectionKey key) {
		this.file = file;
		this.key = key;
	}

	@Override
	public void accept(DataVisitor visitor) throws IOException { visitor.visit(this); }

	/**
	 * Give the file in the GET request
	 * @return The file in the GET request
	 */
	public String getFile() {
		return file;
	}

	/**
	 * Give the key of the client sending the GET request
	 * @return Key of the client sending the GET request
	 */
	public SelectionKey getKey() {
		return key;
	}
}