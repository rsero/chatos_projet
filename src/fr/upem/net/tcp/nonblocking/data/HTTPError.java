package fr.upem.net.tcp.nonblocking.data;

import java.io.IOException;

public class HTTPError implements Data{

	/**
	 * Name of the file that is not found
	*/
    private final String file;

    /**
     * Construct the HTTPError
     * @param Name of the file that is not found
     */
    public HTTPError(String file) {
        this.file = file;
    }

    @Override
    public void accept(DataVisitor visitor) throws IOException { visitor.visit(this); }

    /**
     * Give the name of the file that is not found
     * @return Name of the file that is not found
     **/
    public String getFile() {
        return file;
    }
}

