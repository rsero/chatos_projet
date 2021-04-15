package fr.upem.net.tcp.nonblocking.data;

import fr.upem.net.tcp.nonblocking.client.ClientChatos;
import fr.upem.net.tcp.nonblocking.server.ContextServer;
import fr.upem.net.tcp.nonblocking.server.ServerChatos;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
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
    public boolean processOut(ByteBuffer bbout, ContextServer context, ServerChatos server) throws IOException {
        var bb = encode(bbout);
        return bb != null;
    }

    @Override
    public void accept(DataClientVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void accept(DataServerVisitor visitor) { visitor.visit(this); }

    public Login getLogin() {
        return login;
    }
}
