package fr.upem.net.tcp.nonblocking.data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import fr.upem.net.tcp.nonblocking.client.ClientChatos;
import fr.upem.net.tcp.nonblocking.server.ContextServer;
import fr.upem.net.tcp.nonblocking.server.ServerChatos;

public class HTTPError implements Data{

    private final String file;

    public HTTPError(String file) {
        this.file = file;
    }

    @Override
    public boolean processOut(ByteBuffer bbout, ContextServer context, ServerChatos server)
            throws IOException {
        return false;
    }

    @Override
    public void accept(DataClientVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void accept(DataServerVisitor visitor) {
        visitor.visit(this);
    }

    public String getFile() {
        return file;
    }
}

