package fr.upem.net.tcp.nonblocking.data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import fr.upem.net.tcp.nonblocking.client.ClientChatos;
import fr.upem.net.tcp.nonblocking.client.Context;

public class RefuseRequest extends RequestOperation {

    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final int BUFFER_SIZE = 1024;

    public RefuseRequest(Login loginRequester, Login loginTarget) {
    	super(loginRequester, loginTarget);
    }

    public String toString() {
        return loginTarget() + " refused the connection with you";
    }

    public ByteBuffer encode() {
        var req = ByteBuffer.allocate(BUFFER_SIZE);
        var senderbuff = UTF8.encode(loginRequester());
        var targetbuff = UTF8.encode(loginTarget());
        int senderlen =senderbuff.remaining();
        int targetlen =targetbuff.remaining();
        if(req.remaining() < senderlen + targetlen + 2 * Integer.BYTES + 1) {
            return null;
        }
        req.put((byte) 7).putInt(senderlen).put(senderbuff).putInt(targetlen).put(targetbuff);
        return req;
    }

    @Override
    public void deleteRequestConnection(ClientChatos client){
    	super.deleteRequestConnection(client);
    }

    @Override
    public void accept(DataVisitor visitor) throws IOException {
        visitor.visit(this);
    }

}
