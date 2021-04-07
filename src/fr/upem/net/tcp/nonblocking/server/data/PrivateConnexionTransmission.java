package fr.upem.net.tcp.nonblocking.server.data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import fr.upem.net.tcp.nonblocking.client.ClientChatos;
import fr.upem.net.tcp.nonblocking.server.ContextServer;
import fr.upem.net.tcp.nonblocking.server.ServerChatos;

public class PrivateConnexionTransmission implements Data  {
	
    private final ByteBuffer bbin;

    public PrivateConnexionTransmission(ByteBuffer buffer) {
		this.bbin = buffer;
	}
    
	@Override
	public boolean processOut(ByteBuffer bbout, ContextServer context, ServerChatos server)
			throws IOException, IOException {
		System.out.println(">>> in " + bbin);
		System.out.println(bbout);
		if(bbin.remaining() > bbout.remaining()) {
			System.out.println("hhhh");
            return false;
        }
		bbout.put(bbin);
    	return true;
	}

	@Override
	public void decode(ClientChatos server, SelectionKey key) throws IOException {
		
	}

	@Override
	public void broadcast(Selector selector, ContextServer context, SelectionKey key) throws IOException {
		var keyTarget = context.findKeyTarget(key);
		((ContextServer) keyTarget.attachment()).queueMessage(this);
	}

}
