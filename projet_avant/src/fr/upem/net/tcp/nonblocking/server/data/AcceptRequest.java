package fr.upem.net.tcp.nonblocking.server.data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.upem.net.tcp.nonblocking.client.ClientChatos;
import fr.upem.net.tcp.nonblocking.server.ContextServer;
import fr.upem.net.tcp.nonblocking.server.ServerChatos;

public class AcceptRequest extends RequestOperation{

	private static final Charset UTF8 = Charset.forName("UTF-8");
	private boolean clientOneConnect = false;
	private boolean clientTwoConnect = false;
	private SelectionKey keyClientOne;
	private SelectionKey keyClientTwo;
    private long connect_id;
	private static final Logger logger = Logger.getLogger(AcceptRequest.class.getName());
	
    public AcceptRequest(Login loginRequester, Login loginTarget, long connect_id) {
    	super(loginRequester, loginTarget);
    	this.connect_id = connect_id;
    }
    
    public AcceptRequest(Login loginRequester, Login loginTarget) {
    	super(loginRequester, loginTarget);
    }
    
	public void updatePrivateConnexion(SelectionKey keyClient) {
		if(!clientOneConnect) {
			clientOneConnect = true;
			keyClientOne = keyClient;
			return;
		}
		keyClientTwo = keyClient;
		clientTwoConnect = true;
	}
    
	public boolean connexionReady() {
		return clientOneConnect && clientTwoConnect;
	}
	
    @Override
	public boolean processOut(ByteBuffer bbout, ContextServer context, ServerChatos server) throws IOException {
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
	public void decode(ClientChatos client, SelectionKey key) throws IOException {
		Login login;
		if(client.getLogin().equals(getLoginRequester())){
			login = getLoginTarget();
		}
		else {
			login = getLoginRequester();
		}
		client.addConnect_id(connect_id, login);
		System.out.println("Connection " + loginRequester() + " : " + loginTarget() + " is established with id : "+ connect_id
		+ "\n \"/id "+ connect_id +"\" to accept");

	}

	@Override
	public void broadcast(Selector selector, ContextServer context, SelectionKey key) throws IOException {
		connect_id = context.definedConnectId(this);
		var ctx = findContextRequester(context);
        ctx.queueMessage(this);
        ctx = findContextTarget(context);
        ctx.queueMessage(this);
	}

	public SelectionKey getKeyRequester() {
		return keyClientOne;
	}
	public SelectionKey getKeyTarget() {
		return keyClientTwo;
	}

	public boolean containsKey(SelectionKey key) {
		return key.equals(keyClientOne) || key.equals(keyClientTwo);
	}

	public SelectionKey findKey(SelectionKey keyTarget) {
		if(keyTarget.equals(keyClientOne))
			return keyClientTwo;
		if(keyTarget.equals(keyClientTwo))
			return keyClientOne;
		return null;
	}

	private void silentlyClose(SelectionKey key) {
		Channel sc = (Channel) key.channel();
		try {
			sc.close();
		} catch (IOException e) {
			// ignore exception
		}
	}
	public void disconnectSocket(){
    	silentlyClose(keyClientOne);
    	silentlyClose(keyClientTwo);
	}

}
