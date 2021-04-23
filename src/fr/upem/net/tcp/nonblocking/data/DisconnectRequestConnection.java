package fr.upem.net.tcp.nonblocking.data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class DisconnectRequestConnection implements Data{

	/**
	 * Login of the client requesting the disconnection
	 */
    private final Login login;
    /**
     * Charset of the encoding
     */
    private static final Charset UTF8 = Charset.forName("UTF-8");
    /**
     * Maximum buffer capacity
     */
    private static int BUFFER_SIZE = 1024;
    /**
     * Create an object to request the disconnection
     * @param login Login of the client requesting the disconnection
     */
    public DisconnectRequestConnection(Login login){
        this.login = login;
    }

    /**
     * Returns the frame containing the disconnection information
     * @return Encoded disconnection frame
     */
    public ByteBuffer encode(){
        var req = ByteBuffer.allocate(BUFFER_SIZE);
        var loginRequester = UTF8.encode(login.getLogin());
        var lenRequester = loginRequester.remaining();
        if(req.remaining() < Integer.BYTES + lenRequester + 1)
            return null;
        req.put((byte) 12).putInt(lenRequester).put(loginRequester);
        return req;
    }

    @Override
    public void accept(DataVisitor visitor) throws IOException { visitor.visit(this); }

    /**
     * Gives the login of the person requesting the disconnection
     * @return The login of the person requesting the disconnection
     */
    public Login getLogin() {
        return login;
    }
}
