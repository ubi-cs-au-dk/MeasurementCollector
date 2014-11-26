package dk.au.measurementcollector.loggers;

import java.util.List;

import dk.au.measurementcollector.Controller;
import dk.au.measurementcollector.writers.LogWriter;
import dk.au.perpos.sensing.measurements.Measurement;
import dk.au.perpos.sensing.measurements.compass.OrientationMeasurement;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;
import android.util.Log;

public class OrientationLogger extends BaseLogger {

	public static final String LOG_TAG_SENSOR_ORIENTATION = "ORIENTATION";
	
	private SensorManager sm;
	private Measurement lastOrientationMeasurement;
	private Thread sampler;
	
	public OrientationLogger(Context context, LogWriter writer) {
		super(context,writer);
		sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
	}
	
	@Override
	public void start() {
		super.start();
		if (sampleRate > 0) {
			sampler = new Thread(new Runnable() {
				public void run() {
					boolean run = true;
					while(run) {
						try {
							Thread.sleep(sampleRate*1000);
							writer.writeSample(LOG_TAG_SENSOR_ORIENTATION, lastOrientationMeasurement);
						} catch (InterruptedException e) {
							run = false;
						}
					}
				}
			});
			
			sampler.start();
		}
		
		Sensor sensor = sm.getDefaultSensor(Sensor.TYPE_ORIENTATION);
		sm.registerListener(sensorlistener,sensor,SensorManager.SENSOR_DELAY_UI); //SENSOR_DELAY_NORMAL
		
		isSampling = true;
		Log.d(LOG_TAG_SENSOR_ORIENTATION, "Orientation logging enabled");
	}
	
	@Override 
	public void stop() {
		super.stop();
		if (isSampling) {
			Sensor sensor = sm.getDefaultSensor(Sensor.TYPE_ORIENTATION);
			sm.unregisterListener(sensorlistener, sensor);

			if (sampleRate > 0) sampler.interrupt();
			isSampling = false;
			Log.d(LOG_TAG_SENSOR_ORIENTATION, "Orientation logging stopped");
		}
	}

	/**
	 * Sensor value change listener
	 */
	private SensorEventListener sensorlistener = new SensorEventListener() {
		public void onAccuracyChanged(Sensor arg0, int arg1) {}

		public void onSensorChanged(SensorEvent event) {
			long timeStampInMillis = System.currentTimeMillis() - SystemClock.uptimeMillis() + (event.timestamp/1000000) + timeOffset;
			Measurement measurement = new OrientationMeasurement(timeStampInMillis, event.values[0], event.values[1], event.values[2], event);
			lastOrientationMeasurement = measurement;

			if (sampleRate == 0) {
				writer.writeSample(LOG_TAG_SENSOR_ORIENTATION, measurement);
			}
		}
	};

}
