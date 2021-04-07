package fr.upem.net.tests;

import fr.upem.net.tcp.nonblocking.server.reader.HTTPRequestReader;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.io.IOException;
import java.nio.charset.Charset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class HTTPRequestReaderTest {

    @Test
    public void testReadLine() throws IOException {

            final String BUFFER_INITIAL_CONTENT = "Debut\rSuite\n\rFin\n\r\nANEPASTOUCHER";
            ByteBuffer buff = ByteBuffer.wrap(BUFFER_INITIAL_CONTENT.getBytes("ASCII")).compact();
            HTTPRequestReader reader = new HTTPRequestReader();
            assertEquals("Debut\rSuite\n\rFin\n", reader.readLineCRLF(buff));
            ByteBuffer buffFinal = ByteBuffer.wrap("ANEPASTOUCHER".getBytes("ASCII")).compact();
            assertEquals(buffFinal.flip(), buff.flip());
    }

    @Test
    public void testReadRequest() throws IOException {
        final String BUFFER_INITIAL_CONTENT = "GET /file HTTP/1.1\r\n"
                + "Host: directory\r\n"
                + "\r\n";
        ByteBuffer buff = ByteBuffer.wrap(BUFFER_INITIAL_CONTENT.getBytes("ASCII")).compact();
        HTTPRequestReader reader = new HTTPRequestReader();
        reader.process(buff);
        assertEquals("directory/file",reader.get().getFile());
    }
}
