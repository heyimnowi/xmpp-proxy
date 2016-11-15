package ar.edu.itba.metrics;

import java.util.HashMap;
import java.util.Map;

public class MetricsCollector {
	
	private static MetricsCollector instance = null;
	
	private Map<String,UserMetrics> metrics;
	
	private long totalBytesSent;
	private long totalBytesReceived;
	private long totalAccesses;
	
	private MetricsCollector(){
		this.metrics = new HashMap<String,UserMetrics>();
		this.totalAccesses = 0;
		this.totalBytesReceived = 0;
		this.totalBytesSent = 0;
	}
	
	public long getTotalAccesses() {
		return totalAccesses;
	}

	public void setTotalMessages(long totalMessages) {
		this.totalAccesses = totalMessages;
	}

	public static MetricsCollector getInstance(){
		if(instance == null)
			instance = new MetricsCollector();
		return instance;
	}
	
	public void msgSent(String jid, String msg){
		logData(jid,msg.length(),true);
	}
	
	public void msgReceived(String jid, String msg){
		logData(jid,msg.length(),false);
	}
	
	private void logData(String jid, int length, boolean sent){
		UserMetrics um = null;
		if(jid != null){
			if(metrics.containsKey(jid)){
				um = metrics.get(jid);
			}else{
				um = new UserMetrics(jid);
				metrics.put(jid, um);
			}
		}
		if(sent){
			totalBytesSent += length;
			if(um != null)
				um.addBytesSent(length);
		}else{
			totalBytesReceived += length;
			if( um != null)
				um.addBytesReceived(length);
		}
		if(um != null)
			um.access();
		totalAccesses++;
	}
	
	public UserMetrics getMetricsByJid(String jid){
		return metrics.get(jid);
	}

	public long getTotalBytesSent() {
		return totalBytesSent;
	}

	public long getTotalBytesReceived() {
		return totalBytesReceived;
	}

	
	public String toString () {
		return "\nmetrics: {\n" + 
				"\t current_time_millis: " + System.currentTimeMillis() + ",\n" +
				"\t bytes_received: " +  totalBytesReceived  + ",\n" +
				"\t bytes_sent: " +  totalBytesSent  + ",\n" +
				"\t accesses: " +  totalAccesses  + "\n" +
				"}";
	}
}