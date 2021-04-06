package fr.upem.net.tcp.nonblocking.server.data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.Charset;

import fr.upem.net.tcp.nonblocking.client.ClientChatos;
import fr.upem.net.tcp.nonblocking.server.ContextServer;
import fr.upem.net.tcp.nonblocking.server.ServerChatos;

public class RefuseRequest extends RequestOperation {

    private static int BUFFER_SIZE = 1024;
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private ByteBuffer req = ByteBuffer.allocate(BUFFER_SIZE);

    public RefuseRequest(Login loginRequester, Login loginTarget) {
    	super(loginRequester, loginTarget);
    }

    public String toString() {
        return loginTarget() + " refused the connection with you";
    }

    private ByteBuffer encode(ByteBuffer req) throws IOException {
    	req.clear();
        var senderbuff = UTF8.encode(loginRequester());
        var targetbuff = UTF8.encode(loginTarget());
        int senderlen =senderbuff.remaining();
        int targetlen =targetbuff.remaining();
        if(req.remaining() < senderlen + targetlen + 2 * Integer.BYTES + 1) {
            return null;
        }
        req.put((byte) 7).putInt(senderlen).put(senderbuff).putInt(targetlen).put(targetbuff);
        return req;
    }

    @Override
    public void decode(ClientChatos client) {
    	System.out.println(this);
    }

    @Override
    public void broadcast(Selector selector, ContextServer context, SelectionKey key) throws IOException {
    		var ctx = findContextRequester(context);
            ctx.queueMessage(this);
    }

    @Override
	public boolean processOut(ByteBuffer bbout, ContextServer context, ServerChatos server) throws IOException {
		return processOut(encode(bbout));
	}
}
