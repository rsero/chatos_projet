package fr.upem.net.tcp.nonblocking.data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class MessageGlobal implements Data{

	/**
	 * Login of the person sending the global message
	 */
    private final Login login;
    /**
     * Global message to send
     */
    private final String msg;
    /**
	 * Charset of the encoding
	 */
    private static final Charset UTF8 = Charset.forName("UTF-8");
    /**
	 * Maximum buffer capacity
	 */
    private static int BUFFER_SIZE = 1024;
    /**
     * Buffer in which the request is encoded
     */
    private ByteBuffer req = ByteBuffer.allocate(BUFFER_SIZE);

    /**
     * Create the object containing the global message
     * @param login Login of the person sending the global message
     * @param msg Global message to send
     */
    public MessageGlobal(Login login, String msg) {
        this.login=login;
        this.msg=msg;
    }

    /*@Override
    public boolean processOut(ContextServer context, ServerChatos server) throws IOException {
    	var bb = encode();
    	if (bb==null) {
    		return false;
    	}
    	return true;
    }
    */
    /**
     * Encodes the frame allowing the sending of a global message
     * @return The frame allowing the sending of a global message
     * @throws IOException
     */
    public ByteBuffer encode() throws IOException {
        var req = ByteBuffer.allocate(BUFFER_SIZE);
        var loginbuff = UTF8.encode(login.getLogin());
        var msgbuff = UTF8.encode(msg);
        int loginlen = loginbuff.remaining();
        int msglen = msgbuff.remaining();

        if (BUFFER_SIZE < loginlen + msglen + 2 * Integer.BYTES + 1) {
            return null;
        }
        req.put((byte) 3).putInt(loginlen).put(loginbuff).putInt(msglen).put(msgbuff);
        return req;
    }

    /**
     * Give the login of the person sending the global message
     * @return Login of the person sending the global message
     */
    public Login getLogin(){
        return login;
    }

    /**
     * Give the global message to send
     * @return Global message to send
     */
    public String getMsg(){
        return msg;
    }

    @Override
    public void accept(DataVisitor visitor) throws IOException { visitor.visit(this); }
}
