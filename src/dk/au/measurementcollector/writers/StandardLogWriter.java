package dk.au.measurementcollector.writers;

import java.io.BufferedWriter;
import java.io.File;
import java.util.Collection;
import java.util.ConcurrentModificationException;

import android.util.Log;

import dk.au.measurementcollector.Toaster;
import dk.au.perpos.sensing.measurements.Measurement;
import dk.au.perpos.sensing.webservice.PerPosSerializer;
import dk.au.perpos.sensing.webservice.SerializationException;

public class StandardLogWriter implements LogWriter {

	private String filePrefix;
	private File folder;
	private BufferedWriter logFile;
	private boolean showTag = false;
	private long timeOffset = 0L;
	
	public StandardLogWriter(String filePrefix, File folder, boolean showTag, long timeOffset) {
		this.filePrefix = filePrefix;
		this.showTag = showTag;
		this.folder = folder;
		this.timeOffset = timeOffset;
	}
	
	public void writeSample(String tag, Object data) {
		writeSample(tag, data, System.currentTimeMillis());
	}

	public synchronized void writeSample(String tag, Object data, long timeStamp) {
		if (logFile == null) {
			return;
		}
		
		try {
			String dataRepresentation;
			
			if (data == null) {
				dataRepresentation = "null";
			
			} else if (data instanceof String) {
				dataRepresentation = (String) data;

			} else if (data instanceof Collection<?>) {
				Collection<?> list = (Collection<?>) data;
				StringBuilder sb = new StringBuilder("[ ");
				for (Object o : list) {
					try {
						sb.append(PerPosSerializer.serialize(o) + ",");
					} catch (SerializationException e) {
						Log.e("StandardLogWriter", "Could not serialize: " + o);
						Toaster.showToast("Could not serialize: " + o.toString());
					}
				}
				sb.deleteCharAt(sb.length()-1); // Delete last ","
				sb.append(" ]");
				dataRepresentation = sb.toString();
				
			} else if (data instanceof Measurement ){
				// Serialize all PerPos measurement objects
				dataRepresentation = PerPosSerializer.serialize(data);

			} else {
				// Just save string representation of everything else
				dataRepresentation = data.toString();
			}
			
			String sample = (showTag ? tag + "::" : "") + dataRepresentation + "\n";
			FileUtil.writeToLog(sample, logFile);

		} catch (SerializationException e) {
			Toaster.showToast("Could not serialize: " + e.getMessage());
		} catch (ConcurrentModificationException cme) {
			Log.e("StandardLogWriter", cme.getMessage(), cme);
		}
	}
	
	/**
	 * Create a new log file. This will only work if the file isn't open already.
	 */
	public void createNewLog() {
		if (logFile == null) {
			logFile = FileUtil.open(filePrefix + "_" + (System.currentTimeMillis() + timeOffset) + ".log", folder);
		}
	}

	/**
	 * Close any currently open log file.
	 */
	public synchronized void closeLog() {
		if (logFile != null) {
			FileUtil.close(logFile);
			logFile = null;
		}
	}
}
