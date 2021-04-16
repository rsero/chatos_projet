package fr.upem.net.tcp.nonblocking.data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.logging.Logger;

public class PrivateRequest extends RequestOperation {

	private Byte opCode;
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final Logger logger = Logger.getLogger(PrivateRequest.class.getName());
    private static final int BUFFER_SIZE = 1024;

    public PrivateRequest(Login loginRequester, Login loginTarget) {
    	super(loginRequester, loginTarget);
    	this.opCode = (byte) 5;
    }

    public String toString() {
        return loginRequester() + " wants to establish a private connection with you \n" +
                "\"/y "+ loginRequester() +"\" to accept \n" +
                "\"/n "+ loginRequester() +"\" to refuse";
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
        req.put(opCode).putInt(senderlen).put(senderbuff).putInt(targetlen).put(targetbuff);
        return req;
    }

    @Override
    public void accept(DataVisitor visitor) throws IOException {
        visitor.visit(this);
    }

    public ByteBuffer encodeAskPrivateRequest() {
    	opCode = (byte) 5;
		return encode();
	}
    
	public ByteBuffer encodeAcceptPrivateRequest() {
		opCode = (byte) 6;
		return encode();
	}
	
	public ByteBuffer encodeRefusePrivateRequest() {
		opCode = (byte) 7;
		return encode();
	}

	/*@Override
	public boolean processOut(ContextServer context, ServerChatos server) throws IOException {
		//return processOut(encode(bbout));
        return true;
	}*/
}