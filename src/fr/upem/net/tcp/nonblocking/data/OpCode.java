package fr.upem.net.tcp.nonblocking.data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import fr.upem.net.tcp.nonblocking.client.ClientChatos;
import fr.upem.net.tcp.nonblocking.client.Context;
import fr.upem.net.tcp.nonblocking.server.ContextServer;
import fr.upem.net.tcp.nonblocking.server.ServerChatos;

public class OpCode implements Data{
	
	private final Byte opCode;

	public OpCode(byte opCode) {
		this.opCode = opCode;
	}

	public Byte getByte() {
		return opCode;
	}
	
	/*@Override
	public boolean processOut(ContextServer context, ServerChatos server) {
		return false;
	}

	 */

	@Override
	public void accept(DataClientVisitor visitor) { visitor.visit(this); }

	@Override
	public void accept(DataServerVisitor visitor, Context context) throws IOException {
		visitor.visit(this, context);
	}

}
