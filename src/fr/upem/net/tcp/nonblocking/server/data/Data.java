package fr.upem.net.tcp.nonblocking.server.data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import fr.upem.net.tcp.nonblocking.client.ClientChatos;
import fr.upem.net.tcp.nonblocking.server.ContextServer;
import fr.upem.net.tcp.nonblocking.server.ServerChatos;

public interface Data {
    public boolean processOut(ByteBuffer bbout, ContextServer context, ServerChatos server) throws IOException, IOException;
    public void decode(ClientChatos server) throws IOException;
    public void broadcast(Selector selector, ContextServer context, SelectionKey key) throws IOException;
}
