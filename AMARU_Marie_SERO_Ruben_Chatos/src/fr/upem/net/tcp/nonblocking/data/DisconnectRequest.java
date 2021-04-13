package fr.upem.net.tcp.nonblocking.data;

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

public class DisconnectRequest extends RequestOperation{

    private final Long connectId;
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final Logger logger = Logger.getLogger(DisconnectRequest.class.getName());

    public DisconnectRequest(Long connectId, Login loginRequester, Login loginTarget){
        super(loginRequester, loginTarget);
        this.connectId = connectId;
    }


    @Override
    public boolean processOut(ByteBuffer bbout, ContextServer context, ServerChatos server) throws IOException {
        var bb = encode(bbout);
        return bb != null;
    }

    public ByteBuffer encode(ByteBuffer req) throws IOException {
        req.clear();
        var loginRequester = UTF8.encode(loginRequester());
        int lenRequester = loginRequester.remaining();
        var loginTarget = UTF8.encode(loginTarget());
        int lenTarget = loginTarget.remaining();
        if(req.remaining() < lenTarget + lenRequester + 2 * Integer.BYTES + Long.BYTES + 1) {
            return null;
        }
        req.put((byte) 11).putLong(connectId).putInt(lenRequester).put(loginRequester).putInt(lenTarget).put(loginTarget);
        return req;
    }

    @Override
    public void decode(ClientChatos client, SelectionKey key) throws IOException {

    }

    @Override
    public void broadcast(Selector selector, ContextServer context, SelectionKey key) throws IOException {
        context.disconnectClient(connectId);
        var ctx = context.findContextClient(getLoginTarget());
        System.out.println(ctx);
        System.out.println(context);
        /*if(ctx != null)
            ctx.queueMessage(new DisconnectRequestConnection(getLoginRequester()));
        else
            logger.info("This client is not connected to the server");
*/
        logger.log(Level.INFO,"Connection is now closed");
    }
}
