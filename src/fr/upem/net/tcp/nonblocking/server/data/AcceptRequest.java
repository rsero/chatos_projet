package fr.upem.net.tcp.nonblocking.server.data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.charset.Charset;
import java.util.Objects;

import fr.upem.net.tcp.nonblocking.client.ClientChatos;
import fr.upem.net.tcp.nonblocking.server.Context;
import fr.upem.net.tcp.nonblocking.server.ServerChatos;

public class AcceptRequest extends RequestOperation{

	private static final Charset UTF8 = Charset.forName("UTF-8");
	private boolean clientOneConnect = false;
	private boolean clientTwoConnect = false;
    private long connect_id;
	
    public AcceptRequest(Login loginRequester, Login loginTarget, long connect_id) {
    	super(loginRequester, loginTarget);
    	this.connect_id = Objects.requireNonNull(connect_id);
    }
    
    public AcceptRequest(Login loginRequester, Login loginTarget) {
    	super(loginRequester, loginTarget);
    }
    
    public String toString() {
        return loginTarget() + " accepted the connection with you";
    }
    
	public void updatePrivateConnexion() {
		if(!clientOneConnect) {
			clientOneConnect = true;
			return;
		}
		clientTwoConnect = true;
	}
    
	public boolean connexionReady() {
		return clientOneConnect && clientTwoConnect;
	}
	
    @Override
	public boolean processOut(ByteBuffer bbout, Context context, ServerChatos server) throws IOException {
    	return processOut(encode(bbout));
	}
    
    private ByteBuffer encode(ByteBuffer req) throws IOException {
    	req.clear();
        var senderbuff = UTF8.encode(loginRequester());
        var targetbuff = UTF8.encode(loginTarget());
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
		Login login;
		if(client.getLogin().equals(getLoginRequester())){
			login = getLoginTarget();
		}
		else {
			login = getLoginRequester();
		}
		client.addConnect_id(connect_id, login);
		System.out.println("Connection " + loginRequester() + " : " + loginTarget() + " is established with id : "+ connect_id);
	}

	@Override
	public void broadcast(Selector selector, Context context) throws IOException {
		connect_id = context.definedConnectId(this);
		var ctx = findContextRequester(context);
        ctx.queueMessage(this);
        ctx = findContextTarget(context);
        ctx.queueMessage(this);
	}
}
