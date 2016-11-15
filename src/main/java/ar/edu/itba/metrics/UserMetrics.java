package ar.edu.itba.metrics;

public class UserMetrics {
	
	private long bytesSent;
	private long bytesReceived;
	private long accesses;
	private final String jid;
	
	public UserMetrics(String jid) {
		this.bytesReceived = 0;
		this.bytesSent = 0;
		this.accesses = 0;
		this.jid = jid;
	}

	public long getBytesSent() {
		return bytesSent;
	}

	public void addBytesSent(long bytesSent) {
		this.bytesSent += bytesSent;
	}

	public long getBytesReceived() {
		return bytesReceived;
	}

	public void addBytesReceived(long bytesReceived) {
		this.bytesReceived += bytesReceived;
	}

	public long getAccesses() {
		return accesses;
	}

	public void access() {
		this.accesses++;
	}
	
	public String toString () {
		return "\nmetrics: {\n" + 
				"\t current_time_millis: " + System.currentTimeMillis() + ",\n" +
				"\t jid: " + jid  + ",\n" +
				"\t bytes_received: " +  bytesReceived  + ",\n" +
				"\t bytes_sent: " +  bytesSent  + ",\n" +
				"\t accesses: " +  accesses  + "\n" +
				"}";
	}
	
	
}