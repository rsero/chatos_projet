package fr.upem.net.tcp.nonblocking.server.data;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Optional;

import fr.upem.net.tcp.nonblocking.server.Context;
import fr.upem.net.tcp.nonblocking.server.ServerChatos;

public class MessageGlobal implements Data{

    private final Login login;
    private final String msg;
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static int BUFFER_SIZE = 1024;

    public MessageGlobal(Login login, String msg) {
        this.login=login;
        this.msg=msg;
    }

    public String getLogin() {
        return login.getLogin();
    }

    public String getMsg() {
        return msg;
    }

    public String toString() {
        return login + " : " + msg;
    }

	@Override
    public boolean processOut(ByteBuffer bbout, Context context, ServerChatos server) {
		var bb = UTF8.encode(this.toString());
		var sizeBb = bb.remaining();
		//bbout.flip();
		if(bbout.remaining() < sizeBb + Integer.BYTES) {
    		return false;
    	}
    	
    	bbout.putInt(sizeBb);
    	bbout.put(bb);
    	
    	return true;
    }
	
	public ByteBuffer encodeGlobalMessage(SocketChannel sc, String msg) throws IOException {
		var req = ByteBuffer.allocate(BUFFER_SIZE);
		var loginbuff = UTF8.encode(login.getLogin());
		var msgbuff = UTF8.encode(msg);
		int loginlen = loginbuff.remaining();
		int msglen = msgbuff.remaining();

		if (BUFFER_SIZE < loginlen + msglen + 2 * Integer.BYTES + 1) {
			return null;
		}
		req.put((byte) 3).putInt(loginlen).put(loginbuff).putInt(msglen).put(msgbuff);
		req.flip();
		return req;
	}

	@Override
	public void decode() {
		// TODO Auto-generated method stub
		System.out.println(login + " : " + msg);
	}



}
