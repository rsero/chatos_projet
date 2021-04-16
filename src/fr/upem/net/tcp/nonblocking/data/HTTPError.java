package fr.upem.net.tcp.nonblocking.data;

import java.io.IOException;

import fr.upem.net.tcp.nonblocking.client.Context;

public class HTTPError implements Data{

    private final String file;

    public HTTPError(String file) {
        this.file = file;
    }

    @Override
    public void accept(DataVisitor visitor) {
        visitor.visit(this);
    }

    public String getFile() {
        return file;
    }
}

