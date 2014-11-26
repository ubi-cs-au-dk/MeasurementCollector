package dk.au.measurementcollector.loggers;
/**
 * Class
 */
import android.content.Context;
import dk.au.measurementcollector.Controller;
import dk.au.measurementcollector.writers.LogWriter;


public class UserTimestampLogger extends BaseLogger {

	public static final String LOG_TAG_USER_TIMESTAMP = "USER_TIMESTAMP";
	private long lastTimestamp;

	
	public UserTimestampLogger(Context context, LogWriter writer) {
		super(context, writer);
	}

	public Object getLastScan() {
		return lastTimestamp;
	}
	
	/**
	 * Log the current timestamp
	 */
	public void logTimestamp() {
		lastTimestamp = System.currentTimeMillis() + timeOffset;
		writer.writeSample(LOG_TAG_USER_TIMESTAMP, lastTimestamp);
	}
}
