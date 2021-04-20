package fr.upem.net.tcp.nonblocking.reader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

/**
 * The Reader interface represents the different data reader types and allows to process them
 * and provides methods to get the different elements decoded and reset the reader
 */

public interface Reader<Data> {

    /**
     * Reads the ByteBuffer and passes the key to the reader
     * @param bb ByteBuffer
     * @param key Selection key
	 * @return the process status
     * @throws IOException
	 */
	ProcessStatus process(ByteBuffer bb, SelectionKey key) throws IOException;

    /**
     * Gets the Data Object which represents the ByteBuffer that have been processed
     * @return a Data Object
     */
    Data get() throws IOException;

    /**
     * Resets the reader
     */
    void reset();

}
