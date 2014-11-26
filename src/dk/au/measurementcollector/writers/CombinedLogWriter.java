package dk.au.measurementcollector.writers;
/**
 * Log writer that group different loggers output together.
 * TODO: Still buggy under high load
 */
import java.io.BufferedWriter;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import dk.au.measurementcollector.Toaster;
import dk.au.perpos.sensing.webservice.PerPosSerializer;
import dk.au.perpos.sensing.webservice.SerializationException;
import android.util.Log;

public class CombinedLogWriter implements LogWriter {

	private static final String GROUP_DIVIDER = "---";
	
	private String filePrefix;
	private File folder;
	private BufferedWriter logFile;
	
	private Set<String> loggerGroup = new HashSet<String>();
	private Map<String, Object> logData = new HashMap<String, Object>();
	
	public CombinedLogWriter(String filePrefix, File folder) {
		this.filePrefix = filePrefix;
		this.folder = folder;
	}
	
	/**
	 * Add a logger to the "synced" list. The file won't be written
	 * until all loggers have written their output.
	 * @param logger
	 */
	public void addTag(String tag) {
		loggerGroup.add(tag);
	}
	
	public void createNewLog() {
		if (logFile != null) {
			// This will get called by every logger in the logging group
			// so just ignore further calls
			return;
		}
		logFile = FileUtil.open(filePrefix + "_" + System.currentTimeMillis() + ".log", folder);
	}

	public void closeLog() {
		synchronized(this){
			if (logFile != null) {
				FileUtil.close(logFile);
				logFile = null;
			}
		}
	}

	public void writeSample(String tag, Object data, long timeStamp) {
		boolean reset = false;
		
		if (logData.containsKey(tag)) {
			reset = true;
			Log.w("LogWriter", "Data of type: " + tag + ", has already been written");
			Toaster.showToast("Log data being overriden: " + tag + ". " +
					"Some sensors aren't reporting results");
		}
		
		if (logFile == null) {
			Toaster.showToast("No open log file.");
			return;
		}
		
		// Force new group if data is being overriden
		// TODO: Perhaps do something a bit more clever to make sure that
		// every member of the logging group gets some output (even if it is
		// the empty string).
		if (reset) {
			FileUtil.writeToLog(GROUP_DIVIDER + "\n", logFile);
			logData.clear();
		}

		// Else just keep grouping as normal
		logData.put(tag, data);
		synchronized (this) {
			try {
				String dataRepresentation;
				if (data instanceof String) {
					dataRepresentation = (String) data;
				} else {
					dataRepresentation = PerPosSerializer.serialize(data);
				}
				String sample = timeStamp + "::" + tag + "::" + dataRepresentation + "\n";
				FileUtil.writeToLog(sample, logFile);

				// Test if 
				if (logData.size() == loggerGroup.size()) {
					FileUtil.writeToLog(GROUP_DIVIDER + "\n", logFile);
					logData.clear();
				}
				
			} catch (SerializationException e) {
				Toaster.showToast("Could not serialize: " + e.getMessage());
			}		
		}
	}

	public void writeSample(String tag, Object data) {
		writeSample(tag, data, System.currentTimeMillis());
	}
}
