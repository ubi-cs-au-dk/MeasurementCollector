package dk.au.measurementcollector.loggers;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;

import dk.au.measurementcollector.writers.LogWriter;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;

public class ActivityRecognitionLogger extends BaseLogger implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener  {

	public static final String LOG_TAG_SENSOR_ACTIVITY = "ACTIVITY";
	
	SensorManager sm;
	private Context context;
	private static ActivityRecognitionLogger current;
	private PendingIntent actIntent;
	
	GoogleApiClient client;
	

	public ActivityRecognitionLogger(Context context, LogWriter writer) {
		super(context,writer);
		sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		client = new GoogleApiClient.Builder(context)
        .addApi(ActivityRecognition.API)
        .addConnectionCallbacks(this)
        .addOnConnectionFailedListener(this)
        .build();
		current = this;
		this.context = context;	
		Log.d("Activity", "Constructed");
	}
	
	@Override
	public void start() {
		super.start();		
		client.connect();
		Log.d("Activity", "Starting connect");
	}
	
	@Override 
	public void stop() {
		super.stop();
		if (isSampling) {
			isSampling = false;
			ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(client,  actIntent);
		}		
	}

	@Override
	public void onConnected(Bundle arg0) {
		isSampling = true;
		Log.d("PlayServices", "Connected to google play");
		Log.d("Activity", "Connected!");
		Intent i = new Intent(context, ActivityRecognitionIntentService.class);
		actIntent = PendingIntent.getService(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
		ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(client, 0, actIntent); //Check tal			
	}

	@Override
	public void onConnectionSuspended(int arg0) {
		Log.d("PlayServices", "Lost connection to google play");
	}

	@Override
	public void onConnectionFailed(ConnectionResult arg0) {
		Log.d("PlayServices", "Could not connect to google play");		
	}
	
	private void write(String type, long sTime){		
		long currentTime = System.currentTimeMillis();
		long timeStampInMillis = currentTime - SystemClock.uptimeMillis() + (sTime/1000000) + timeOffset; 
		StringBuilder measurement = new StringBuilder();
		measurement.append("t=");
		measurement.append(timeStampInMillis);
		measurement.append(";sT=");
		measurement.append(sTime);
		measurement.append(";cT=");
		measurement.append(currentTime);
		measurement.append(";activity=" + type);
		if(isSampling){
			writer.writeSample(LOG_TAG_SENSOR_ACTIVITY, measurement);
		}		
	}
	
	public static void writeSample(String measurement, long sTime){
		current.write(measurement, sTime);
	}
}