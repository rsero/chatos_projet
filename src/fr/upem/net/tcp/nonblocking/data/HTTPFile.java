package fr.upem.net.tcp.nonblocking.data;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.Charset;

import fr.upem.net.tcp.nonblocking.client.ClientChatos;
import fr.upem.net.tcp.nonblocking.client.Context;
import fr.upem.net.tcp.nonblocking.server.ContextServer;
import fr.upem.net.tcp.nonblocking.server.ServerChatos;

public class HTTPFile implements Data {

    private final String content_type;
    private final ByteBuffer buffRead;
    private final String nameFile;
    private final Charset charsetASCII = Charset.forName("ASCII");

    public HTTPFile(String content_type, ByteBuffer buffRead, String nameFile) {
        this.content_type = content_type;
        this.buffRead = buffRead;
        this.nameFile = nameFile;
    }

    public ByteBuffer getBuffRead() {
        return buffRead;
    }

    public String getNameFile() {
        return nameFile;
    }

    public boolean isTextFile() {
        return nameFile.endsWith(".txt");
    }

    //@Override
    public boolean processOut(ContextServer context, ServerChatos server)
            throws IOException {
        return false;
    }

    @Override
    public void accept(DataClientVisitor visitor) throws IOException {
        visitor.visit(this);
    }

    @Override
    public void accept(DataServerVisitor visitor, Context context) throws IOException { visitor.visit(this, context); }
}