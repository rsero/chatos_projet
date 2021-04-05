package fr.upem.net.tcp.nonblocking.server.data;

import java.util.ArrayList;
import java.util.List;

public class PrivateConnectionClients {

	private Long connect_id = null;
	private final List<String> file;
	
	public PrivateConnectionClients() {
		this.file = new ArrayList<>();
	}
	
	public PrivateConnectionClients(Long connect_id) {
		this();
		this.connect_id = connect_id;
	}
	
	public void addConnectId(Long connect_id) {
		this.connect_id = connect_id;
	}
	
	public void addFileToSend(String newFile) {
		file.add(newFile);
	}
	
	public void removeFileToSend(String lastFile) {
		file.remove(lastFile);
	}
	
	public boolean correctConnectId(Long id) {
		return id != null && id.equals(connect_id);
	}
	
}
