package fr.upem.net.tcp.nonblocking.data;

import java.io.IOException;

import fr.upem.net.tcp.nonblocking.client.Context;

public class OpCode implements Data{
	
	private final Byte opCode;

	public OpCode(byte opCode) {
		this.opCode = opCode;
	}

	public Byte getByte() {
		return opCode;
	}

	@Override
	public void accept(DataVisitor visitor) throws IOException { visitor.visit(this); }

}
