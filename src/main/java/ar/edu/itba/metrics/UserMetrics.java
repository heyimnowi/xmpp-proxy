package ar.edu.itba.metrics;

public class UserMetrics {
	
	private long bytesSent;
	private long bytesReceived;
	private long accesses;
	
	public UserMetrics(){
		this.bytesReceived = 0;
		this.bytesSent = 0;
		this.accesses = 0;
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
	
	
}