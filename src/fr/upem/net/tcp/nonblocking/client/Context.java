package fr.upem.net.tcp.nonblocking.client;

import java.io.IOException;
import java.nio.ByteBuffer;

public interface Context {

    /**
     * Process the content of bbin
     *
     * The convention is that bbin is in write-mode before the call to process and
     * after the call
     *
     */
    void processIn() throws IOException;

    /**
     * Add a buffer to the message queue, tries to fill bbOut and updateInterestOps
     *
     * @param bb buffer
     */
    void queueMessage(ByteBuffer bb);

    /**
     * Try to fill bbout from the message queue
     *
     */
    void processOut();

    /**
     * Update the interestOps of the key looking only at values of the boolean
     * closed and of both ByteBuffers.
     *
     * The convention is that both buffers are in write-mode before the call to
     * updateInterestOps and after the call. Also it is assumed that process has
     * been be called just before updateInterestOps.
     */
    void updateInterestOps();

    /**
     * Closes the connection on the socket channel
     */
    void silentlyClose();

    /**
     * Performs the read action on sc
     *
     * The convention is that both buffers are in write-mode before the call to
     * doRead and after the call
     *
     * @throws IOException
     */
    void doRead() throws IOException;

    /**
     * Performs the write action on sc
     *
     * The convention is that both buffers are in write-mode before the call to
     * doWrite and after the call
     *
     * @throws IOException
     */
    void doWrite() throws IOException;

    /**
     *  Checks is the socket channel and is connected and changes its interestop to OP_READ
     * @throws IOException if the connection is closed
     */
    void doConnect() throws IOException;
}
