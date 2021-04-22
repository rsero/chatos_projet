package fr.upem.net.tcp.nonblocking.data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.logging.Logger;

public class DisconnectRequest extends RequestOperation{

	/**
	 * Connection password
	 */
    private final Long connectId;
    /**
     * Charset of the encoding
     */
    private static final Charset UTF8 = Charset.forName("UTF-8");
    /**
     * Logger of the class DisconnectRequest
     */
    private static final Logger logger = Logger.getLogger(DisconnectRequest.class.getName());
    /**
     * Maximum buffer capacity
     */
    private static final int BUFFER_SIZE = 1024;

    /**
     * Initialize an object allowing the disconnection
     * @param connectId Connection password
     * @param loginRequester Login to the first connected client
     * @param loginTarget Login to the second connected client
     */
    public DisconnectRequest(Long connectId, Login loginRequester, Login loginTarget){
        super(loginRequester, loginTarget);
        this.connectId = connectId;
    }

    /**
     * Returns the frame that informs about the disconnection
     * @return Byte buffer containing the encoded frame
     * @throws IOException
     */
    public ByteBuffer encode() throws IOException {
        var req = ByteBuffer.allocate(BUFFER_SIZE);
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
    public void accept(DataVisitor visitor) throws IOException { visitor.visit(this); }

    /**
	 * Give the password of the connection
	 * @return The password of the connection
	 */
    public Long getConnectId() {
        return connectId;
    }
}
