package fr.upem.net.tcp.nonblocking.data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.charset.Charset;
import java.util.Objects;

public class AcceptRequest extends RequestOperation{

	/**
	 * Charset of the encoding
	 */
	private static final Charset UTF8 = Charset.forName("UTF-8");
	/**
	 * Maximum buffer capacity
	 */
	private static final int BUFFER_SIZE = 1024;
	/**
	 * true if a client is connected to the private connection
	 */
	private boolean clientOneConnect = false;
	/**
	 * true if two client are connected to the private connection
	 */
	private boolean clientTwoConnect = false;
	/**
	 * Key to the first connected client
	 */
	private SelectionKey keyClientOne;
	/**
	 * Key to the second connected client
	 */
	private SelectionKey keyClientTwo;
	/**
	 * Connection password
	 */
	private long connect_id;

	/**
	 * Manages a private connection between two clients
	 * @param loginRequester Login of the first client
	 * @param loginTarget Login of the second client
	 * @param connect_id Connection password
	 */
	public AcceptRequest(Login loginRequester, Login loginTarget, long connect_id) {
		super(loginRequester, loginTarget);
		this.connect_id = Objects.requireNonNull(connect_id);
	}

	/**
	 * Manages a private connection between two clients
	 * @param loginRequester Login of the first client
	 * @param loginTarget Login of the second client
	 */
	public AcceptRequest(Login loginRequester, Login loginTarget) {
		super(loginRequester, loginTarget);
	}

	/**
	 * Announce that the connection has been accepted
	 * @return Message announcing that the connection has been accepted
	 */
	public String toString() {
		return loginTarget() + " accepted the connection with you";
	}

	/**
	 * Adds a client to the private connection
	 * @param keyClient Key of the client who connects
	 */
	public void updatePrivateConnexion(SelectionKey keyClient) {
		if(!clientOneConnect) {
			clientOneConnect = true;
			keyClientOne = keyClient;
			return;
		}
		keyClientTwo = keyClient;
		clientTwoConnect = true;
	}

	/**
	 * Defined if the connection is ready
	 * @return Return true is the private connection is established
	 */
	public boolean connexionReady() {
		return clientOneConnect && clientTwoConnect;
	}

	/**
	 * Returns the frame informing that the connection can be established with the password
	 * @return Byte buffer containing the encoded frame
	 * @throws IOException
	 */
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

	@Override
	public void accept(DataVisitor visitor) throws IOException { visitor.visit(this); }

	/**
	 * Gives the key to the first connected client
	 * @return The key to the first connected client
	 */
	public SelectionKey getKeyRequester() {
		return keyClientOne;
	}
	
	/**
	 * Gives the key to the second connected client
	 * @return The key to the second connected client
	 */
	public SelectionKey getKeyTarget() { return keyClientTwo; }

	/**
	 * Test if the client in parameter belongs to this private connection
	 * @param key Client's key which is compared with the keys of the private connection
	 * @return Return true if the client in parameter belongs to this private connection
	 */
	public boolean containsKey(SelectionKey key) {
		return key.equals(keyClientOne) || key.equals(keyClientTwo);
	}

	/**
	 * Finds the opposite key to the one passed in parameter
	 * @param keyTarget key Client's key which is compared with the keys of the private connection
	 * @return The opposite key to the one passed in parameter
	 */
	public SelectionKey findKey(SelectionKey keyTarget) {
		if(keyTarget.equals(keyClientOne))
			return keyClientTwo;
		if(keyTarget.equals(keyClientTwo))
			return keyClientOne;
		return null;
	}

	/**
	 * Close the key connection
	 * @param key Key to the closed connection
	 */
	private void silentlyClose(SelectionKey key) {
		Channel sc = (Channel) key.channel();
		try {
			sc.close();
		} catch (IOException e) {
			// ignore exception
		}
	}
	
	/**
	 * Disconnects the two keys from the private connection
	 */
	public void disconnectSocket(){
		silentlyClose(keyClientOne);
		silentlyClose(keyClientTwo);
	}

	/**
	 * Give the password of the connection
	 * @return The password of the connection
	 */
	public long getConnect_id(){
		return connect_id;
	}

	/**
	 * Update password of the connection
	 * @param connect_id New password for the connection
	 */
	public void setConnect_id(long connect_id){
		this.connect_id=connect_id;
	}
}
