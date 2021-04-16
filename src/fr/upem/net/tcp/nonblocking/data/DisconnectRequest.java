package fr.upem.net.tcp.nonblocking.data;

import fr.upem.net.tcp.nonblocking.client.Context;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.logging.Logger;

public class DisconnectRequest extends RequestOperation{

    private final Long connectId;
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final Logger logger = Logger.getLogger(DisconnectRequest.class.getName());

    public DisconnectRequest(Long connectId, Login loginRequester, Login loginTarget){
        super(loginRequester, loginTarget);
        this.connectId = connectId;
    }

    public ByteBuffer encode(ByteBuffer req) throws IOException {
        req.clear();
        var loginRequester = UTF8.encode(loginRequester());
        int lenRequester = loginRequester.remaining();
        var loginTarget = UTF8.encode(loginTarget());
        int lenTarget = loginTarget.remaining();
        if (req.remaining() < lenTarget + lenRequester + 2 * Integer.BYTES + Long.BYTES + 1) {
            return null;
        }
        req.put((byte) 11).putLong(connectId).putInt(lenRequester).put(loginRequester).putInt(lenTarget).put(loginTarget);
        return req;
    }

    @Override
    public void accept(DataVisitor visitor) {
        visitor.visit(this);
    }

    public Long getConnectId() {
        return connectId;
    }
}
