package fr.upem.net.tcp.nonblocking.data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.Charset;


import fr.upem.net.tcp.nonblocking.client.ClientChatos;
import fr.upem.net.tcp.nonblocking.client.Context;
import fr.upem.net.tcp.nonblocking.server.ContextServer;
import fr.upem.net.tcp.nonblocking.server.ServerChatos;

public class PrivateMessage implements Data {
    private final Login loginSender;
    private final Login loginTarget;
    private final String msg;
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final int BUFFER_SIZE = 1024;


    public PrivateMessage(Login loginSender, Login loginTarget, String msg) {
        this.loginSender=loginSender;
        this.loginTarget=loginTarget;
        this.msg=msg;
    }

    //@Override
    public boolean processOut(ContextServer context, ServerChatos server) throws IOException {
    	//var bb = encode(bbout);
        //return bb != null;
        return true;
    }

    public ByteBuffer encode() throws IOException {
        var req = ByteBuffer.allocate(BUFFER_SIZE);
        var senderbuff = UTF8.encode(loginSender.getLogin());
        var targetbuff = UTF8.encode(loginTarget.getLogin());
        var msgbuff = UTF8.encode(msg);
        int senderlen =senderbuff.remaining();
        int targetlen =targetbuff.remaining();
        int msglen = msgbuff.remaining();
        if(req.remaining() < senderlen + targetlen + msglen + 3 * Integer.BYTES + 1) {
            return null;
        }
        req.put((byte) 4).putInt(senderlen).put(senderbuff).putInt(targetlen).put(targetbuff).putInt(msglen).put(msgbuff);
        return req;
    }

    @Override
    public void accept(DataClientVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void accept(DataServerVisitor visitor, Context context) throws IOException {
        visitor.visit(this, context);
    }

    public String getLoginSender() {
        return loginSender.toString();
    }

    public Login getLoginTarget() { return loginTarget; }

    public String getMsg() {
        return msg;
    }
}