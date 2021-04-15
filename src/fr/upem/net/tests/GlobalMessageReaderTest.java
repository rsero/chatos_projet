//package fr.upem.net.tests;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertThrows;
//
//import java.nio.ByteBuffer;
//import java.nio.charset.StandardCharsets;
//
//import org.junit.jupiter.api.Test;
//
//import fr.upem.net.tcp.nonblocking.reader.GlobalMessageReader;
//import fr.upem.net.tcp.nonblocking.reader.ProcessStatus;
//import fr.upem.net.tcp.nonblocking.reader.StringReader;
//
//public class GlobalMessageReaderTest {
//
//        @Test
//        public void simple(){
//            var login = "bob";
//            var string = "coucou";
//            var bb = ByteBuffer.allocate(1024);
//            var bytess = StandardCharsets.UTF_8.encode(login);
//            var bytes = StandardCharsets.UTF_8.encode(string);
//            bb.putInt(bytes.remaining()).put(bytes).putInt(bytess.remaining()).put(bytess);
//            GlobalMessageReader sr = new GlobalMessageReader();
//            assertEquals(ProcessStatus.DONE,sr.process(bb));
//            assertEquals(0,bb.position());
//            assertEquals(bb.capacity(),bb.limit());
//        }
//
//        @Test
//        public void reset(){
//            var login = "bob";
//            var string = "coucou";
//            var login2 = "alice";
//            var string2 = "salut";
//            var bb = ByteBuffer.allocate(1024);
//            var bytes = StandardCharsets.UTF_8.encode(string);
//            var bytes2 = StandardCharsets.UTF_8.encode(string2);
//            var bytes3 = StandardCharsets.UTF_8.encode(login);
//            var bytes4 = StandardCharsets.UTF_8.encode(login2);
//            bb.putInt(bytes.remaining()).put(bytes).putInt(bytes2.remaining()).put(bytes2);
//            GlobalMessageReader sr = new GlobalMessageReader();
//            assertEquals(ProcessStatus.DONE,sr.process(bb));
//            assertEquals(0,bb.position());
//            assertEquals(bb.capacity(),bb.limit());
//            sr.reset();
//            assertEquals(0,bb.position());
//            assertEquals(bb.capacity(),bb.limit());
//        }
//
//        @Test
//        public void smallBuffer(){
//            var string = "\u20ACa\u20AC";
//            var bb = ByteBuffer.allocate(1024);
//            var bytes = StandardCharsets.UTF_8.encode(string);
//            bb.putInt(bytes.remaining()).put(bytes).flip();
//            var bbSmall = ByteBuffer.allocate(2);
//            var sr = new StringReader();
//            while (bb.hasRemaining()) {
//                while(bb.hasRemaining() && bbSmall.hasRemaining()){
//                    bbSmall.put(bb.get());
//                }
//                if (bb.hasRemaining()) {
//                    assertEquals(ProcessStatus.REFILL,sr.process(bbSmall));
//                } else {
//                    assertEquals(ProcessStatus.DONE,sr.process(bbSmall));
//                }
//            }
//            assertEquals(string,sr.get());
//        }
//
//        @Test
//        public void errorGet(){
//            GlobalMessageReader sr = new GlobalMessageReader();
//            assertThrows(IllegalStateException.class,() -> {var res=sr.get();});
//        }
//
//        @Test
//        public void errorNeg(){
//            GlobalMessageReader sr = new GlobalMessageReader();
//            var bb = ByteBuffer.allocate(1024);
//            var bytes = StandardCharsets.UTF_8.encode("aaaaa");
//            bb.putInt(-1).put(bytes);
//            assertEquals(ProcessStatus.ERROR,sr.process(bb));
//        }
//
//        @Test
//        public void errorTooBig(){
//            GlobalMessageReader sr = new GlobalMessageReader();
//            var bb = ByteBuffer.allocate(1024);
//            var bytes = StandardCharsets.UTF_8.encode("aaaaa");
//            bb.putInt(1025).put(bytes);
//            assertEquals(ProcessStatus.ERROR,sr.process(bb));
//        }
//}
