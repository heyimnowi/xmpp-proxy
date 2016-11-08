package ar.edu.itba.metrics;

import java.util.HashMap;
import java.util.Map;

public class MetricsCollector {
	
	private static MetricsCollector instance = null;
	
	private Map<String,UserMetrics> metrics;
	
	private long totalBytesSent;
	private long totalBytesReceived;
	private long totalAccesses;
	private long totalMessages;
	
	private MetricsCollector(){
		this.metrics = new HashMap<String,UserMetrics>();
		this.totalMessages = 0;
		this.totalAccesses = 0;
		this.totalBytesReceived = 0;
		this.totalBytesSent = 0;
	}
	
	public long getTotalMessages() {
		return totalMessages;
	}

	public void setTotalMessages(long totalMessages) {
		this.totalMessages = totalMessages;
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
				um = new UserMetrics();
				metrics.put(jid, um);
			}
		}
		if(sent){
			//XMPPProxyLogger.getInstance().info("Message sent by " + jid + " - size: " + length);
			totalBytesSent += length;
			if(um != null)
				um.addBytesSent(length);
		}else{
			//XMPPProxyLogger.getInstance().info("Message sent by " + jid + " - size: " + length);
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

	public long getTotalAccesses() {
		return totalAccesses;
	}
}