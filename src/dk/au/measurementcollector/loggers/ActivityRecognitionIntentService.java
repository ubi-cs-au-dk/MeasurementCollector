package dk.au.measurementcollector.loggers;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

public class ActivityRecognitionIntentService extends IntentService{

	public ActivityRecognitionIntentService(String name) {
		super(name);
	}
	
	public ActivityRecognitionIntentService() {
		super("ActivityRecognitionIntentService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		 if (ActivityRecognitionResult.hasResult(intent)) { 
			 // Extract result from received intent 
			 ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent); 
			 DetectedActivity detected = result.getMostProbableActivity(); 
			 int activityType = detected.getType();
		     String activityName = getNameFromType(activityType);
		     result.getTime();
		     ActivityRecognitionLogger.writeSample(activityName, result.getTime());
			 Log.d("Activity", "Activity: " + activityName);
		 } 

	}
	
	private String getNameFromType(int activityType) {
	    switch(activityType) {
	        case DetectedActivity.IN_VEHICLE:
	            return "in_vehicle";
	        case DetectedActivity.ON_BICYCLE:
	            return "on_bicycle";
	        case DetectedActivity.ON_FOOT:
	            return "on_foot";
	        case DetectedActivity.STILL:
	            return "still";
	        case DetectedActivity.UNKNOWN:
	            return "unknown";
	        case DetectedActivity.TILTING:
	            return "tilting";
	    }
	    return "unknown";
	}
}
