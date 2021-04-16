package fr.upem.net.tcp.nonblocking.data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class DisconnectRequestConnection implements Data{

    private final Login login;
    private static final Charset UTF8 = Charset.forName("UTF-8");

    public DisconnectRequestConnection(Login login){
        this.login = login;
    }

    private ByteBuffer encode(ByteBuffer req){
        req.clear();
        var loginRequester = UTF8.encode(login.getLogin());
        var lenRequester = loginRequester.remaining();
        if(req.remaining() < Integer.BYTES + lenRequester + 1)
            return null;
        req.put((byte) 12).putInt(lenRequester).put(loginRequester);
        return req;
    }

    @Override
    public void accept(DataVisitor visitor) throws IOException { visitor.visit(this); }

    public Login getLogin() {
        return login;
    }
}
