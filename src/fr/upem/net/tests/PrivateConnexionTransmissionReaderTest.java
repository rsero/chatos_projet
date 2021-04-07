package fr.upem.net.tests;
import fr.upem.net.tcp.nonblocking.server.data.PrivateConnexionTransmission;
import fr.upem.net.tcp.nonblocking.server.reader.PrivateConnexionTransmissionReader;
import fr.upem.net.tcp.nonblocking.server.reader.Reader;
import fr.upem.net.tcp.nonblocking.server.reader.StringReader;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
public class PrivateConnexionTransmissionReaderTest {
    @Test
    public void simple() throws IOException {
        var string = "\u20ACa\u20AC";
        var bb = ByteBuffer.allocate(1024);
        var bytes = StandardCharsets.UTF_8.encode(string);
        bb.put(bytes);
        PrivateConnexionTransmissionReader sr = new PrivateConnexionTransmissionReader();
        assertEquals(Reader.ProcessStatus.DONE,sr.process(bb));
        assertEquals(bytes.flip(), sr.get().getbb());
        assertEquals(0,bb.position());
    }

    @Test
    public void reset() throws IOException {
        var string = "\u20ACa\u20AC";
        var string2 = "\u20ACa\u20ACabcd";
        var bb = ByteBuffer.allocate(1024);
        var bytes = StandardCharsets.UTF_8.encode(string);
        var bytes2 = StandardCharsets.UTF_8.encode(string2);
        bb.put(bytes).put(bytes2);
        PrivateConnexionTransmissionReader sr = new PrivateConnexionTransmissionReader();
        assertEquals(Reader.ProcessStatus.DONE,sr.process(bb));
        assertEquals(0,bb.position());
        sr.reset();
        assertEquals(Reader.ProcessStatus.DONE,sr.process(bb));
        assertEquals(0,bb.position());
    }

    @Test
    public void smallBuffer(){
        var string = "\u20ACa\u20AC";
        var bb = ByteBuffer.allocate(1024);
        var bytes = StandardCharsets.UTF_8.encode(string);
        bb.putInt(bytes.remaining()).put(bytes).flip();
        var bbSmall = ByteBuffer.allocate(2);
        var sr = new StringReader();
        while (bb.hasRemaining()) {
            while(bb.hasRemaining() && bbSmall.hasRemaining()){
                bbSmall.put(bb.get());
            }
            if (bb.hasRemaining()) {
                assertEquals(Reader.ProcessStatus.REFILL,sr.process(bbSmall));
            } else {
                assertEquals(Reader.ProcessStatus.DONE,sr.process(bbSmall));
            }
        }
        assertEquals(string,sr.get());
    }

    @Test
    public void errorGet(){
        var sr = new StringReader();
        assertThrows(IllegalStateException.class,() -> {var res=sr.get();});
    }

    @Test
    public void errorNeg(){
        var sr = new StringReader();
        var bb = ByteBuffer.allocate(1024);
        var bytes = StandardCharsets.UTF_8.encode("aaaaa");
        bb.putInt(-1).put(bytes);
        assertEquals(Reader.ProcessStatus.ERROR,sr.process(bb));
    }

    @Test
    public void errorTooBig(){
        var sr = new StringReader();
        var bb = ByteBuffer.allocate(1024);
        var bytes = StandardCharsets.UTF_8.encode("aaaaa");
        bb.putInt(1025).put(bytes);
        assertEquals(Reader.ProcessStatus.ERROR,sr.process(bb));
    }
}
