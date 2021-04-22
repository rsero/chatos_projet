package fr.upem.net.http.server;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

import fr.upem.net.tcp.nonblocking.client.ContextPrivateClient;

public class HTTPServer {

	private final Charset charsetASCII = Charset.forName("ASCII");
	private static final Logger logger = Logger.getLogger(HTTPServer.class.getName());

	public HTTPServer(String file, SelectionKey key, String directory) throws IOException {
		var shortPath = directory+"/"+file;
		var path = new File(shortPath).toURI().getPath();
		var ctx = (ContextPrivateClient) key.attachment();
		File initialFile = new File(path);
		if (!initialFile.exists()) {
			encodeError(path,file,ctx);
			return;
		}
		var contentType = findHTTPExtension(shortPath);
		if (isTextFile(file)) {
			encodeTextFile(shortPath,contentType,file,ctx);
		} else {
			encodeFile(initialFile,contentType,file,ctx);
		}
	}

	private String findHTTPExtension(String file) throws IOException {
		Path path = new File(file).toPath();
		return Files.probeContentType(path);
	}

	private boolean isTextFile(String file) {
		return file.endsWith(".txt");
	}

	private void inputStream(ContextPrivateClient ctx, InputStream in) {
		byte[] buff = new byte[500];
		ByteBuffer bb = ByteBuffer.wrap(buff);
		int read;
		try {
			while ((read = in.read(buff)) != -1) {
				bb.clear();
				bb.limit(read);
				ctx.queueMessage(bb);
			}
			in.close();
		} catch (IOException e) {
			logger.severe("IOException in ServerHTTP");
		}
	}

	private void encodeTextFile(String shortPath, String contentType, String file, ContextPrivateClient context) throws IOException {
		String content = Files.readString(Paths.get(shortPath));
		String header = "HTTP/1.1 200 OK\r\nContent-Length: " + charsetASCII.encode(content).remaining() + "\r\nContent-Type: "
				+ contentType + "\r\nName-File: " + file + "\r\n\r\n";
		var bbHeader = charsetASCII.encode(header);
		context.queueMessage(bbHeader);
		var fileContent = StandardCharsets.UTF_8.encode(content);
		context.queueMessage(fileContent);
	}

	private void encodeFile(File initialFile, String contentType, String file, ContextPrivateClient context) throws IOException {
		InputStream in = new FileInputStream(initialFile);
		String header = "HTTP/1.1 200 OK\r\nContent-Length: " + in.available() + "\r\nContent-Type: " + contentType
				+ "\r\nName-File: " + file + "\r\n\r\n";
		var bbHeader = charsetASCII.encode(header);
		context.queueMessage(bbHeader);
		inputStream(context, in);
	}

	private void encodeError(String path, String file, ContextPrivateClient context){
		System.out.println("Path : "+ path);
		System.out.println("File " + file + " not found");
		String header = "HTTP/1.1 404 Not Found\r\nErrorDocument 404 /" + file + "\r\n\r\n";
		var bbHeader = charsetASCII.encode(header);
		context.queueMessage(bbHeader);
	}

}

