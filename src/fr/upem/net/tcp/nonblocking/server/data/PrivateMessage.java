package fr.upem.net.tcp.nonblocking.server.data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;


import fr.upem.net.tcp.nonblocking.client.ClientChatos;
import fr.upem.net.tcp.nonblocking.server.Context;
import fr.upem.net.tcp.nonblocking.server.ServerChatos;

public class PrivateMessage implements Data {
    private final Login loginSender;
    private final Login loginTarget;
    private final String msg;
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static int BUFFER_SIZE = 1024;

    public PrivateMessage(Login loginSender, Login loginTarget, String msg) {
        this.loginSender=loginSender;
        this.loginTarget=loginTarget;
        this.msg=msg;
    }

    public String toString() {
        return "* "+loginSender + " : " + msg+" *";
    }

    @Override
    public boolean processOut(ByteBuffer bbout, Context context, ServerChatos server) {
        var senderbuff = UTF8.encode(loginSender.getLogin());
        var targetbuff = UTF8.encode(loginTarget.getLogin());
        var msgbuff = UTF8.encode(msg);
        int senderlen =senderbuff.remaining();
        int targetlen =targetbuff.remaining();
        int msglen = msgbuff.remaining();
        if(bbout.remaining() < senderlen + targetlen + msglen + 3 * Integer.BYTES + 1) {
            return false;
        }
        bbout.put((byte) 4).putInt(senderlen).put(senderbuff).putInt(targetlen).put(targetbuff).putInt(msglen).put(msgbuff);
        return true;
    }

    public ByteBuffer encodePrivateMessage(SocketChannel sc) throws IOException {
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
    public void decode(ClientChatos client) {
        //if(client.getLogin().equals(loginTarget))
        System.out.println(loginSender + " : " + msg);
    }

    @Override
    public void broadcast(Selector selector, Context context) {
//        for (SelectionKey key : selector.keys()){
//            if (key.attachment()==null)
//                continue;
//            var ctx = (Context) key.attachment();
    		var ctx = context.findContextClient(loginTarget);
            ctx.queueMessage(this);
        //}


    }
}