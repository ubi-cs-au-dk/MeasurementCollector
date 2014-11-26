package dk.au.measurementcollector.loggers;

import android.content.Context;
import dk.au.measurementcollector.writers.LogWriter;

public class ButtonEventLogger extends BaseLogger {

	public static final String LOG_TAG_BUTTON_EVENT = "BUTTON_EVENT";
	private long lastTimestamp;

	
	public ButtonEventLogger(Context context, LogWriter writer) {
		super(context, writer);
	}

	public Object getLastScan() {
		return lastTimestamp;
	}
	
	/**
	 * Log the current timestamp
	 */
	public void logButtonEvent(String event) {
		lastTimestamp = System.currentTimeMillis() + timeOffset;
		writer.writeSample(LOG_TAG_BUTTON_EVENT, lastTimestamp + " " + event);
	}
}
