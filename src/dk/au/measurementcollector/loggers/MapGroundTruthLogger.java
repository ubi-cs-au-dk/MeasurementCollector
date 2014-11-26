package dk.au.measurementcollector.loggers;

import android.content.Context;
import dk.au.measurementcollector.GroundTruth;
import dk.au.measurementcollector.writers.LogWriter;

public class MapGroundTruthLogger extends BaseLogger {
	
	public static final String LOG_TAG_MAP_GROUND_TRUTH = "MAP_GROUND_TRUTH";
	private GroundTruth last_sample;

	public MapGroundTruthLogger(Context context, LogWriter writer) {
		super(context, writer);
	}

	/*
	 * Log the current ground truth from map
	 */
	public void logGroundTruth() {
		String groundTruth = "t=" +  (System.currentTimeMillis() + timeOffset) + ";p=" + GroundTruth.get();
		last_sample = GroundTruth.get();
		writer.writeSample(LOG_TAG_MAP_GROUND_TRUTH, groundTruth);
	}

}
