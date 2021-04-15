package fr.upem.net.tcp.nonblocking.data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.Charset;
import java.util.Objects;

import fr.upem.net.tcp.nonblocking.client.ClientChatos;
import fr.upem.net.tcp.nonblocking.client.Context;
import fr.upem.net.tcp.nonblocking.server.ContextServer;
import fr.upem.net.tcp.nonblocking.server.ServerChatos;

public class AcceptRequest extends RequestOperation{

	private static final Charset UTF8 = Charset.forName("UTF-8");
	private static final int BUFFER_SIZE = 1024;
	private boolean clientOneConnect = false;
	private boolean clientTwoConnect = false;
	private SelectionKey keyClientOne;
	private SelectionKey keyClientTwo;
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

	//@Override
	public boolean processOut(ContextServer context, ServerChatos server) throws IOException {
		return processOut(encode());
	}

	public ByteBuffer encode() throws IOException {
		var req = ByteBuffer.allocate(BUFFER_SIZE);
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
	/*

	@Override
	public void broadcast(Selector selector, ContextServer context, SelectionKey key) throws IOException {
		connect_id = context.definedConnectId(this);
	}
	*/

	@Override
	public void accept(DataClientVisitor visitor) throws IOException { visitor.visit(this);
	}

	@Override
	public void accept(DataServerVisitor visitor, Context context) throws IOException { visitor.visit(this, context); }

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

	public long getConnect_id(){
		return connect_id;
	}

	public void setConnect_id(long connect_id){
		this.connect_id=connect_id;
	}
}
