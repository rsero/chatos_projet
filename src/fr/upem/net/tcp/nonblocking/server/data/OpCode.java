package fr.upem.net.tcp.nonblocking.server.data;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import fr.upem.net.tcp.nonblocking.client.ClientChatos;
import fr.upem.net.tcp.nonblocking.server.Context;
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
	public boolean processOut(ByteBuffer bbout, Context context, ServerChatos server) {
		return false;
	}

	@Override
	public void decode(ClientChatos server) {
		if(opCode == 1) {
			server.updateLogin();
			System.out.println("Client ajouté");
		}
		else if(opCode == 2) {
			System.out.println("Login déja existant");
		}
		else {
			System.out.println("Opération impossible à exécuter");
		}
	}

	@Override
	public void broadcast(Selector selector, Context context) {
		context.queueMessage(this);
	}

}
