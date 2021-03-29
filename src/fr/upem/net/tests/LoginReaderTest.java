package fr.upem.net.tests;

import fr.upem.net.tcp.nonblocking.server.reader.LoginReader;
import fr.upem.net.tcp.nonblocking.server.reader.Reader;
import fr.upem.net.tcp.nonblocking.server.reader.StringReader;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LoginReaderTest {
    @Test
    public void simple(){
        var string = "josiane";
        var bb = ByteBuffer.allocate(1024);
        var bytes = StandardCharsets.UTF_8.encode(string);
        bb.putInt(bytes.remaining()).put(bytes);
        LoginReader sr = new LoginReader();
        assertEquals(Reader.ProcessStatus.DONE,sr.process(bb));
        assertEquals(string,sr.get().getLogin());
        assertEquals(0,bb.position());
        assertEquals(bb.capacity(),bb.limit());
    }

    @Test
    public void reset(){
        var string = "Josiane";
        var string2 = "Bob";
        var bb = ByteBuffer.allocate(1024);
        var bytes = StandardCharsets.UTF_8.encode(string);
        var bytes2 = StandardCharsets.UTF_8.encode(string2);
        bb.putInt(bytes.remaining()).put(bytes).putInt(bytes2.remaining()).put(bytes2);
        LoginReader sr = new LoginReader();
        assertEquals(Reader.ProcessStatus.DONE,sr.process(bb));
        assertEquals(string,sr.get().getLogin());
        assertEquals(7,bb.position());
        assertEquals(bb.capacity(),bb.limit());
        sr.reset();
        assertEquals(Reader.ProcessStatus.DONE,sr.process(bb));
        assertEquals(string2,sr.get().getLogin());
        assertEquals(0,bb.position());
        assertEquals(bb.capacity(),bb.limit());
    }

    @Test
    public void smallBuffer(){
        var string = "Josiane";
        var bb = ByteBuffer.allocate(1024);
        var bytes = StandardCharsets.UTF_8.encode(string);
        bb.putInt(bytes.remaining()).put(bytes).flip();
        var bbSmall = ByteBuffer.allocate(2);
        var sr = new LoginReader();
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
        assertEquals(string,sr.get().getLogin());
    }

    @Test
    public void errorGet(){
        var sr = new LoginReader();
        assertThrows(IllegalStateException.class,() -> {var res=sr.get();});
    }

    @Test
    public void errorNeg(){
        var sr = new LoginReader();
        var bb = ByteBuffer.allocate(1024);
        var bytes = StandardCharsets.UTF_8.encode("aaaaa");
        bb.putInt(-1).put(bytes);
        assertEquals(Reader.ProcessStatus.ERROR,sr.process(bb));
    }

    @Test
    public void errorTooBig(){
        var sr = new LoginReader();
        var bb = ByteBuffer.allocate(1024);
        var bytes = StandardCharsets.UTF_8.encode("aaaaa");
        bb.putInt(1025).put(bytes);
        assertEquals(Reader.ProcessStatus.ERROR,sr.process(bb));
    }
}
