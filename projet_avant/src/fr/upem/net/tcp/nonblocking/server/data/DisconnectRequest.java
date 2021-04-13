package fr.upem.net.tcp.nonblocking.server.data;

import fr.upem.net.tcp.nonblocking.client.ClientChatos;
import fr.upem.net.tcp.nonblocking.server.ContextServer;
import fr.upem.net.tcp.nonblocking.server.ServerChatos;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DisconnectRequest implements Data{

    private final Long connectId;
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final Logger logger = Logger.getLogger(DisconnectRequest.class.getName());

    public DisconnectRequest(Long connectId){
        this.connectId = connectId;
    }
    @Override
    public boolean processOut(ByteBuffer bbout, ContextServer context, ServerChatos server) throws IOException {
        var bb = encode(bbout);
        if (bb==null) {
            return false;
        }
        return true;
    }

    public ByteBuffer encode(ByteBuffer req) throws IOException {
        req.clear();
        if(req.remaining() < Long.BYTES + 1) {
            return null;
        }
        req.put((byte) 11).putLong(connectId);
        return req;
    }

    @Override
    public void decode(ClientChatos client, SelectionKey key) throws IOException {

    }

    @Override
    public void broadcast(Selector selector, ContextServer context, SelectionKey key) throws IOException {
        context.disconnectClient(connectId);
        logger.log(Level.INFO,"Connection is now closed");
    }
}
