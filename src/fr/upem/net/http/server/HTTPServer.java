package fr.upem.net.http.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
		System.out.println("httpserver debut");
		var shortPath = directory+"/"+file;
		var path = new File(shortPath).toURI().getPath();
		var ctx = (ContextPrivateClient) key.attachment();
		File initialFile = new File(path);
		System.out.println("httpserver File created");
		if (!initialFile.exists()) {
			System.out.println("Path : "+ path);
			System.out.println("Fichier " + file + " pas trouvÃ©");
			String header = "HTTP/1.1 404 Not Found\r\nErrorDocument 404 /" + file + "\r\n\r\n";
			var bbHeader = charsetASCII.encode(header);
			ctx.queueMessage(bbHeader);
			return;
		}

		var contentType = findHTTPExtension(shortPath);

		if (isTextFile(file)) {
			String content = Files.readString(Paths.get(shortPath));
			String header = "HTTP/1.1 200 OK\r\nContent-Length: " + charsetASCII.encode(content).remaining() + "\r\nContent-Type: "
					+ contentType + "\r\nName-File: " + file + "\r\n\r\n";
			var bbHeader = charsetASCII.encode(header);
			ctx.queueMessage(bbHeader);
			var contentb = StandardCharsets.UTF_8.encode(content);
			textFile(ctx, contentb);
		} else {
			InputStream in = new FileInputStream(initialFile);
			String header = "HTTP/1.1 200 OK\r\nContent-Length: " + in.available() + "\r\nContent-Type: " + contentType
					+ "\r\nName-File: " + file + "\r\n\r\n";
			var bbHeader = charsetASCII.encode(header);
			ctx.queueMessage(bbHeader);
			inputStream(ctx, in);
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
		int read = 0;
		try {
			while ((read = in.read(buff)) != -1) {
				bb.clear();
				bb.limit(read);
				ctx.queueMessage(bb);
			}
			in.close();
		} catch (IOException e) {
			logger.severe("IOException in ServerHTTP");
			return;
		}
	}

	private void textFile(ContextPrivateClient ctx, ByteBuffer file) {
		var bool = true;
		while(!Thread.interrupted() && bool) {
			ctx.queueMessage(file);
			bool = false;
		}
	}

	public void serve() {
//		t.start();
	}

	public void shutdown() {
//		t.interrupt();
	}

}

