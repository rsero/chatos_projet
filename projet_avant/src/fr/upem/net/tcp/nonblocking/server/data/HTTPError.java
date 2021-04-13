package fr.upem.net.tcp.nonblocking.server.data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.upem.net.tcp.nonblocking.client.ClientChatos;
import fr.upem.net.tcp.nonblocking.server.ContextServer;
import fr.upem.net.tcp.nonblocking.server.ServerChatos;

public class HTTPError implements Data{

    private final String file;
    private static final Logger logger = Logger.getLogger(HTTPError.class.getName());

    public HTTPError(String file) {
        this.file = file;
    }

    @Override
    public boolean processOut(ByteBuffer bbout, ContextServer context, ServerChatos server)
            throws IOException {
        return false;
    }

    @Override
    public void decode(ClientChatos server, SelectionKey key) throws IOException {
        logger.log(Level.INFO,"The file " + file + " was not found");
    }

    @Override
    public void broadcast(Selector selector, ContextServer context, SelectionKey key) throws IOException {
    }

}
