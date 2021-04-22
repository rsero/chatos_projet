package fr.upem.net.tcp.nonblocking.data;

import java.io.IOException;
import java.nio.channels.SelectionKey;

public class OpCode implements Data{
	
	/**
	 * Opcode representing an action containing no data
	 */
	private final Byte opCode;
	/**
	 * Key of the person who sent the opCode
	 */
	private final SelectionKey key;

	/**
	 * Builds an object representing an action containing no data
	 * @param opCode OpCode
	 * @param key Key of the person who sent the opCode
	 */
	public OpCode(byte opCode, SelectionKey key) {
		this.opCode = opCode;
		this.key=key;
	}

	/**
	 * Give the opCode
	 * @return The opCode
	 */
	public Byte getByte() {
		return opCode;
	}

	@Override
	public void accept(DataVisitor visitor) throws IOException { visitor.visit(this); }

	/**
	 * Give the key of the person who sent the opCode
	 * @return Key of the person who sent the opCode
	 */
	public SelectionKey getKey() {
		return key;
	}
}
