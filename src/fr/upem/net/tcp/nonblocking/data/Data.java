package fr.upem.net.tcp.nonblocking.data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import fr.upem.net.tcp.nonblocking.client.ClientChatos;
import fr.upem.net.tcp.nonblocking.client.Context;
import fr.upem.net.tcp.nonblocking.server.ContextServer;
import fr.upem.net.tcp.nonblocking.server.ServerChatos;

public interface Data {
    //boolean processOut(Context context, ServerChatos server) throws IOException;
    //void decode(ClientChatos server, SelectionKey key) throws IOException;
    //void broadcast(Selector selector, ContextServer context, SelectionKey key) throws IOException;
    void accept(DataClientVisitor visitor) throws IOException;
    void accept(DataServerVisitor visitor, Context context) throws IOException;
}
