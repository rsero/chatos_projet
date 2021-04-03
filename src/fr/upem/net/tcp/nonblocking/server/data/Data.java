package fr.upem.net.tcp.nonblocking.server.data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;

import fr.upem.net.tcp.nonblocking.client.ClientChatos;
import fr.upem.net.tcp.nonblocking.server.Context;
import fr.upem.net.tcp.nonblocking.server.ServerChatos;

public interface Data {
    public boolean processOut(ByteBuffer bbout, Context context, ServerChatos server) throws IOException;
    public void decode(ClientChatos server);
    public void broadcast(Selector selector, Context context) throws IOException;
}
