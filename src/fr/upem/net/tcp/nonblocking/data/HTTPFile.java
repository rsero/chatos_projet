package fr.upem.net.tcp.nonblocking.data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import fr.upem.net.tcp.nonblocking.client.Context;

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

    @Override
    public void accept(DataVisitor visitor) throws IOException {
        visitor.visit(this);
    }
}