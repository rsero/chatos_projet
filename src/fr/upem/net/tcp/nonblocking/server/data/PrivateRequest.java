package fr.upem.net.tcp.nonblocking.server.data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.Charset;

import fr.upem.net.tcp.nonblocking.client.ClientChatos;
import fr.upem.net.tcp.nonblocking.server.ContextServer;
import fr.upem.net.tcp.nonblocking.server.ServerChatos;

public class PrivateRequest extends RequestOperation {

	private Byte opCode;
    private static final Charset UTF8 = Charset.forName("UTF-8");

    public PrivateRequest(Login loginRequester, Login loginTarget) {
    	super(loginRequester, loginTarget);
    	this.opCode = (byte) 5;
    }

    public String toString() {
        return loginRequester() + " wants to establish a private connection with you \n" +
                "\"/y "+ loginRequester() +"\" to accept \n" +
                "\"/n "+ loginRequester() +"\" to refuse";
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
        req.put(opCode).putInt(senderlen).put(senderbuff).putInt(targetlen).put(targetbuff);
        return req;
    }

    @Override
    public void decode(ClientChatos client) {
    	client.addSetPrivateRequest(this);
        System.out.println(this);
    }

    @Override
    public void broadcast(Selector selector, ContextServer context, SelectionKey key) throws IOException {
    		var ctx = findContextTarget(context);
            ctx.queueMessage(this);
    }

    public ByteBuffer encodeAskPrivateRequest(ByteBuffer req) throws IOException {
    	opCode = (byte) 5;
		return encode(req);
	}
    
	public ByteBuffer encodeAcceptPrivateRequest(ByteBuffer req) throws IOException {
		opCode = (byte) 6;
		return encode(req);
	}
	
	public ByteBuffer encodeRefusePrivateRequest(ByteBuffer req) throws IOException {
		opCode = (byte) 7;
		return encode(req);
	}

	@Override
	public boolean processOut(ByteBuffer bbout, ContextServer context, ServerChatos server) throws IOException {
		return processOut(encode(bbout));
	}
}