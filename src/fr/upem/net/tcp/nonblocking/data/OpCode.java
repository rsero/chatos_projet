package fr.upem.net.tcp.nonblocking.data;

import java.io.IOException;
import java.nio.channels.SelectionKey;

public class OpCode implements Data{
	
	private final Byte opCode;
	private final SelectionKey key;

	public OpCode(byte opCode, SelectionKey key) {
		this.opCode = opCode;
		this.key=key;
	}

	public Byte getByte() {
		return opCode;
	}

	@Override
	public void accept(DataVisitor visitor) throws IOException { visitor.visit(this); }

	public SelectionKey getKey() {
		return key;
	}
}
