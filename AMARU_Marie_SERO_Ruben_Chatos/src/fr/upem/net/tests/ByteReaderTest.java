package fr.upem.net.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

import fr.upem.net.tcp.nonblocking.reader.ByteReader;
import fr.upem.net.tcp.nonblocking.reader.ProcessStatus;

public class ByteReaderTest {
    @Test
    public void simple(){
        Byte bit = (byte) 1;
        var bb = ByteBuffer.allocate(1024);
        bb.put(bit);
        ByteReader sr = new ByteReader();
        assertEquals(ProcessStatus.DONE,sr.process(bb));
        assertEquals(bit,sr.get().getByte());
        assertEquals(0,bb.position());
        assertEquals(bb.capacity(),bb.limit());
    }

    @Test
    public void reset(){
        Byte bit = (byte) 1;
        Byte bit2 = (byte) 2;
        var bb = ByteBuffer.allocate(1024);
        bb.put(bit).put(bit2);
        ByteReader sr = new ByteReader();
        assertEquals(ProcessStatus.DONE,sr.process(bb));
        assertEquals(bit,sr.get().getByte());
        assertEquals(1,bb.position());
        assertEquals(bb.capacity(),bb.limit());
        sr.reset();
        assertEquals(ProcessStatus.DONE,sr.process(bb));
        assertEquals(bit2,sr.get().getByte());
        assertEquals(0,bb.position());
        assertEquals(bb.capacity(),bb.limit());
    }

    @Test
    public void errorGet(){
        ByteReader sr = new ByteReader();
        assertThrows(IllegalStateException.class,() -> {var res=sr.get();});
    }

}
