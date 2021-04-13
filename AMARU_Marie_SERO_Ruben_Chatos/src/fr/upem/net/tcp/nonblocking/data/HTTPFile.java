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

    private boolean isTextFile() {
        return nameFile.endsWith(".txt");
    }

    @Override
    public boolean processOut(ByteBuffer bbout, ContextServer context, ServerChatos server)
            throws IOException {
        return false;
    }

    @Override
    public void decode(ClientChatos client, SelectionKey key) throws IOException {
        if (isTextFile()) {
            buffRead.flip();
            System.out.println("File received : \n" + charsetASCII.decode(buffRead).toString());
        } else {
            var shortPath = client.getDirectory()+"/"+nameFile;
            var path = new File(shortPath).toURI().getPath();
            File initialFile = new File(path);
            buffRead.flip();

            OutputStream outputStream = new FileOutputStream(initialFile);

            byte[] arr = buffRead.array();
            outputStream.write(arr);
            outputStream.close();
        }
    }

    @Override
    public void broadcast(Selector selector, ContextServer context, SelectionKey key) throws IOException {

    }
}