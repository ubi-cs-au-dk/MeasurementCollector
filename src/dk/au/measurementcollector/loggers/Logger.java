package dk.au.measurementcollector.loggers;

import dk.au.measurementcollector.writers.LogWriter;

public interface Logger {
	public void start();				// Start logging data.
	public void stop();					// Stop logging data 
	public void setSampleRate(int sec);	// Set sample rate in seconds. 0 = log all changes.
	public void setLogWriter(LogWriter writer);
	public void setTimeOffset(long timeOffset); //Set timeOffset, e.g., from NTP
}
