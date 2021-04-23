package fr.upem.net.tcp.nonblocking.data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

public class PrivateConnectionTransmission implements Data  {

	/**
	 * Buffer containing the data frame of a private connection
	 */
	private final ByteBuffer bbin;
	/**
	 * Key of the person who sent the buffer
	 */
	private final SelectionKey key;

	/**
	 * Creates an object that allows the frame to be transmitted without looking at it
	 * @param bbin Buffer containing the data frame of a private connection
	 * @param key Key of the person who sent the buffer
	 */
	public PrivateConnectionTransmission(ByteBuffer bbin, SelectionKey key) {
		this.bbin = bbin;
		this.key = key;
	}

	/**
	 * Encode the buffer
	 * @return The buffer with no change
	 */
	public ByteBuffer encode(){
		return bbin;
	}

	@Override
	public void accept(DataVisitor visitor) throws IOException { visitor.visit(this); }

	/**
	 * Give the key of the person who sent the buffer
	 * @return Key of the person who sent the buffer
	 */
	public SelectionKey getKey(){
		return key;
	}

}
