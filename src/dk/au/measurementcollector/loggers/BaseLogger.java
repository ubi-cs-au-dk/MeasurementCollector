package dk.au.measurementcollector.loggers;
/**
 * Standard functionality for loggers.
 * Handles 
 */
import dk.au.measurementcollector.writers.LogWriter;
import android.content.Context;


abstract class BaseLogger implements Logger {

	LogWriter writer;
	Context context;
	boolean isSampling = false;
	int sampleRate = 0;	// Sample rate. 0 = log whenever new data is available.
	long timeOffset = 0; // TimeOffset, e.g. measured via NTP
	
	public BaseLogger(Context context, LogWriter writer) {
		this.context = context;
		this.writer = writer;
	}
	
	public void setTimeOffset(long timeOffset) {
		this.timeOffset = timeOffset;
	}
	
	public void setSampleRate(int sec) {
		sampleRate = sec;
	}
	
	public void setLogWriter(LogWriter writer) {
		this.writer = writer;
	}

	public void start() {
		writer.createNewLog();
	}
	public void stop() {
		writer.closeLog();
	}
}
