package fr.upem.net.tcp.nonblocking.server.data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import fr.upem.net.tcp.nonblocking.client.ClientChatos;
import fr.upem.net.tcp.nonblocking.server.ContextServer;
import fr.upem.net.tcp.nonblocking.server.ServerChatos;

public class PrivateConnexionTransmission implements Data  {
	
	private static int BUFFER_SIZE = 1024;
   private final ByteBuffer bbin = ByteBuffer.allocate(BUFFER_SIZE);
    private final ByteBuffer bbout = ByteBuffer.allocate(BUFFER_SIZE);


	@Override
	public boolean processOut(ByteBuffer bbout, ContextServer context, ServerChatos server)
			throws IOException, IOException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void decode(ClientChatos server, SelectionKey key) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void broadcast(Selector selector, ContextServer context, SelectionKey key) throws IOException {
		// TODO Auto-generated method stub
		
	}

}