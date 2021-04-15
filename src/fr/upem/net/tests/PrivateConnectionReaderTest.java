package fr.upem.net.tests;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.io.IOException;

public class PrivateConnectionReaderTest {
    /**
     * Test for ReadLineLFCR with a null Socket
     */

    @Test
    public void testReadLineLFCR1() throws IOException {
        try {
            final String BUFFER_INITIAL_CONTENT = "Debut\rSuite\n\rFin\n\r\nANEPASTOUCHER";
            ByteBuffer buff = ByteBuffer.wrap(BUFFER_INITIAL_CONTENT.getBytes("ASCII")).compact();
            //HTTPReader reader = new HTTPReader(null, buff);
            //assertEquals("Debut\rSuite\n\rFin\n", reader.readLineCRLF());
            ByteBuffer buffFinal = ByteBuffer.wrap("ANEPASTOUCHER".getBytes("ASCII")).compact();
           // assertEquals(buffFinal.flip(), buff.flip());
        } catch (NullPointerException e) {
           // fail("The socket must not be read until buff is entirely consumed.");
        }
    }

    /**
     * Test for ReadLineLFCR with a fake server
     * @throws IOException
     */
    /*
    @Test
    public void testLineReaderLFCR2() throws IOException {
        FakeHTTPServer server = new FakeHTTPServer("Line1\r\nLine2\nLine2cont\r\n",7);
        try {
            server.serve();
            SocketChannel sc = SocketChannel.open();
            sc.connect(new InetSocketAddress("localhost", server.getPort()));
            var buff = ByteBuffer.allocate(12);
            buff.put("AA\r".getBytes("ASCII"));
            HTTPReader reader = new HTTPReader(sc, buff);
            assertEquals("AA\rLine1", reader.readLineCRLF());
            assertEquals("Line2\nLine2cont", reader.readLineCRLF());
        } finally {
            server.shutdown();
        }
    }*/
}
