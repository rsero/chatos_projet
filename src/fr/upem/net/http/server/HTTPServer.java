package fr.upem.net.http.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

import fr.upem.net.tcp.nonblocking.client.ContextPublicClient;

public class HTTPServer {

	private final Thread t;
	private final Charset charsetASCII = Charset.forName("ASCII");
	private static final Logger logger = Logger.getLogger(HTTPServer.class.getName());

	private String findHTTPExtension(String file) throws IOException {
		Path path = new File(file).toPath();
		return Files.probeContentType(path);
	}

	private boolean isTextFile(String file) {
		return file.endsWith(".txt");
	}

	private Thread inputStream(ContextPublicClient ctx, InputStream in) {
		return new Thread(() -> {
			byte[] buff = new byte[500];
			ByteBuffer bb = ByteBuffer.wrap(buff);
			int read = 0;
			try {
				while (!Thread.interrupted() && (read = in.read(buff)) != -1) {
					bb.clear();
					bb.limit(read);
					ctx.queueMessage(bb);
					Thread.sleep(100);
				}
				in.close();
			} catch (IOException e) {
				logger.severe("IOException in ServerHTTP");
				return;
			} catch (InterruptedException e) {
				logger.severe("InterruptedException in ServerHTTP");
				return;
			}
		});
	}

	private Thread textFile(ContextPublicClient ctx, String file) {
		return new Thread(() -> {
			var content = charsetASCII.encode(file);
			try {
				while (!Thread.interrupted() && content.hasRemaining()) {
					var oldlimit = content.limit();
					content.limit(Math.min(content.position() + 1024, oldlimit));
					ctx.queueMessage(content);
					Thread.sleep(100);
					content.limit(oldlimit);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		});
	}

	public HTTPServer(String file, SelectionKey key, String directory) throws IOException {
		var shortPath = directory+"/"+file;
		var path = new File(shortPath).toURI().getPath();
		var ctx = (ContextPublicClient) key.attachment();
		File initialFile = new File(path);
		if (!initialFile.exists()) {
			System.out.println("Path : "+ path);
			System.out.println("Fichier " + file + " pas trouvé");
			String header = "HTTP/1.1 404 Not Found\r\nErrorDocument 404 /" + file + "\r\n\r\n";
			var bbHeader = charsetASCII.encode(header);
			ctx.queueMessage(bbHeader);
			this.t = new Thread(() -> {
			});
			return;
		}

		var contentType = findHTTPExtension(shortPath);

		if (isTextFile(file)) {
			String content = Files.readString(Paths.get(shortPath));
			String header = "HTTP/1.1 200 OK\r\nContent-Length: " + charsetASCII.encode(content).remaining() + "\r\nContent-Type: "
					+ contentType + "\r\nName-File: " + file + "\r\n\r\n";
			var bbHeader = charsetASCII.encode(header);
			ctx.queueMessage(bbHeader);
			this.t = textFile(ctx, content);
		} else {
			InputStream in = new FileInputStream(initialFile);
			String header = "HTTP/1.1 200 OK\r\nContent-Length: " + in.available() + "\r\nContent-Type: " + contentType
					+ "\r\nName-File: " + file + "\r\n\r\n";
			var bbHeader = charsetASCII.encode(header);
			ctx.queueMessage(bbHeader);
			this.t = inputStream(ctx, in);
		}
	}

	public void serve() {
		t.start();
	}

	public void shutdown() {
		t.interrupt();
	}

}
