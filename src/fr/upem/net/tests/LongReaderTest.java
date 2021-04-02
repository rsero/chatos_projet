package fr.upem.net.tests;

import fr.upem.net.tcp.nonblocking.server.reader.LongReader;
import fr.upem.net.tcp.nonblocking.server.reader.Reader;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LongReaderTest {

    @Test
    public void simple(){
        var num = (long) 2;
        var bb = ByteBuffer.allocate(1024);
        bb.putLong(num);
        LongReader sr = new LongReader();
        assertEquals(Reader.ProcessStatus.DONE,sr.process(bb));
        assertEquals(num,sr.get());
        assertEquals(0,bb.position());
        assertEquals(bb.capacity(),bb.limit());
    }

    @Test
    public void reset(){
        var num = (long) 2;
        var num2 = (long) 3;
        var bb = ByteBuffer.allocate(1024);
        bb.putLong(num).putLong(num2);
        LongReader sr = new LongReader();
        assertEquals(Reader.ProcessStatus.DONE,sr.process(bb));
        assertEquals(num,sr.get());
        assertEquals(8,bb.position());
        assertEquals(bb.capacity(),bb.limit());
        sr.reset();
        assertEquals(Reader.ProcessStatus.DONE,sr.process(bb));
        assertEquals(num2,sr.get());
        assertEquals(0,bb.position());
        assertEquals(bb.capacity(),bb.limit());
    }

    @Test
    public void smallBuffer(){
        var num = (long) 2;
        var bb = ByteBuffer.allocate(1024);
        bb.putLong(num).flip();
        var bbSmall = ByteBuffer.allocate(2);
        LongReader sr = new LongReader();
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
        assertEquals(num,sr.get());
    }

    @Test
    public void errorGet(){
        LongReader sr = new LongReader();
        assertThrows(IllegalStateException.class,() -> {var res=sr.get();});
    }
}
