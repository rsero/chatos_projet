package fr.upem.net.tcp.nonblocking.data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class HTTPFile implements Data {

	/**
	 * Type of data received
	 */
    private final String content_type;
    /**
     * Buffer containing read data
     */
    private final ByteBuffer buffRead;
    /**
     * Name of file received
     */
    private final String nameFile;
    /**
     * Charset of the encoding
     */
    private final Charset charsetASCII = Charset.forName("ASCII");

    /**
     * Builds an object containing the information of a received file
     * @param content_type Type of data received
     * @param buffRead Buffer containing read data
     * @param nameFile Name of file received
     */
    public HTTPFile(String content_type, ByteBuffer buffRead, String nameFile) {
        this.content_type = content_type;
        this.buffRead = buffRead;
        this.nameFile = nameFile;
    }

    /**
     * Give the buffer containing read data
     * @return Buffer containing read data
     */
    public ByteBuffer getBuffRead() {
        return buffRead;
    }

    /**
     * Give the name of file received
     * @return Name of file received
     */
    public String getNameFile() {
        return nameFile;
    }

    /**
     * Checks if the file is a .txt file
     * @return true if the file is a .txt file
     */
    public boolean isTextFile() {
        return nameFile.endsWith(".txt");
    }

    @Override
    public void accept(DataVisitor visitor) throws IOException { visitor.visit(this); }
}