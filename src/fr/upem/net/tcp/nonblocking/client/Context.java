package fr.upem.net.tcp.nonblocking.client;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

public interface Context {
    void processIn(ClientChatos client, SelectionKey key) throws IOException;
    void queueMessage(ByteBuffer bb);
    void processOut();
    void updateInterestOps();
    void silentlyClose();
    void doRead(ClientChatos client, SelectionKey key) throws IOException;
    void doWrite() throws IOException;
    void doConnect() throws IOException;
    void closeConnection();
}
