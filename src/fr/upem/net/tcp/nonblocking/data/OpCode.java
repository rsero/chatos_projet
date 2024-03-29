package fr.upem.net.tcp.nonblocking.data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import fr.upem.net.tcp.nonblocking.client.ClientChatos;
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
	
	@Override
	public boolean processOut(ByteBuffer bbout, ContextServer context, ServerChatos server) {
		return false;
	}

	@Override
	public void decode(ClientChatos client, SelectionKey key) {
		switch (opCode) {
		case 1:
			client.updateLogin();
			System.out.println("Identification accepted");
			break;
		case 2:
			System.out.println("Login already taken");
			break;
		case 10:
			System.out.println("Connexion was established");
			client.activePrivateConnection(key);
			break;
		default:
			System.out.println("Operation does not exist");
			break;
		}
	}

	@Override
	public void broadcast(Selector selector, ContextServer context, SelectionKey key) throws IOException {
		context.queueMessage(this);
	}

}
