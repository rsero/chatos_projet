package fr.upem.net.tcp.nonblocking.data;

import java.io.IOException;

public interface Data {
    void accept(DataVisitor visitor) throws IOException;
}
