package fr.upem.net.tcp.nonblocking.server.data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.charset.Charset;
import java.util.Objects;

import fr.upem.net.tcp.nonblocking.client.ClientChatos;
import fr.upem.net.tcp.nonblocking.server.Context;
import fr.upem.net.tcp.nonblocking.server.ServerChatos;

public class AcceptRequest implements Data{

	private static final Charset UTF8 = Charset.forName("UTF-8");
	private final Login loginRequester;
    private final Login loginTarget;
    private long connect_id;
	
    
    
    public AcceptRequest(Login loginRequester, Login loginTarget, long connect_id) {
    	this.loginRequester = loginRequester;
		this.loginTarget = loginTarget;
    	this.connect_id = Objects.requireNonNull(connect_id);
    }
    
    public String toString() {
        return loginTarget + " accept the connection with you";
    }
    
    @Override
	public boolean processOut(ByteBuffer bbout, Context context, ServerChatos server) throws IOException {
    	//connect_id = server.definedConnectId();
    	var bb = encode(bbout);
    	if (bb==null) {
    		return false;
    	}
    	return true;
	}
    
    private ByteBuffer encode(ByteBuffer req) throws IOException {
    	req.clear();
        var senderbuff = UTF8.encode(loginRequester.getLogin());
        var targetbuff = UTF8.encode(loginTarget.getLogin());
        int senderlen =senderbuff.remaining();
        int targetlen =targetbuff.remaining();
        if(req.remaining() < senderlen + targetlen + 2 * Integer.BYTES + Long.BYTES + 1) {
            return null;
        }
        req.put((byte) 8).putInt(senderlen).put(senderbuff).putInt(targetlen).put(targetbuff).putLong(connect_id);
        return req;
    }

	@Override
	public void decode(ClientChatos client) {
		System.out.println("Connection " + loginRequester + " : " + loginTarget + " is established with id : "+ connect_id);
	}

	@Override
	public void broadcast(Selector selector, Context context) throws IOException {
		var ctx = context.findContextClient(loginRequester);
        ctx.queueMessage(this);
        ctx = context.findContextClient(loginTarget);
        ctx.queueMessage(this);
	}
}
