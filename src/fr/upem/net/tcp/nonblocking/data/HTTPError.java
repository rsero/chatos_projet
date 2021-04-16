package fr.upem.net.tcp.nonblocking.data;

import java.io.IOException;

public class HTTPError implements Data{

    private final String file;

    public HTTPError(String file) {
        this.file = file;
    }

    @Override
    public void accept(DataVisitor visitor) throws IOException { visitor.visit(this); }

    public String getFile() {
        return file;
    }
}

