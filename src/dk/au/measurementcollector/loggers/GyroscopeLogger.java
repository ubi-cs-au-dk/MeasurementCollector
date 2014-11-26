package dk.au.measurementcollector.loggers;

import java.util.Date;
import java.util.List;

import dk.au.measurementcollector.Controller;
import dk.au.measurementcollector.writers.LogWriter;
import dk.au.perpos.sensing.android.internal.MeasurementProducerClient;
import dk.au.perpos.sensing.measurements.Measurement;
import dk.au.perpos.sensing.measurements.accelerometer.XYZAccelerometerMeasurement;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.util.Log;

public class GyroscopeLogger extends BaseLogger {

	public static final String LOG_TAG_SENSOR_GYROSCOPE = "GYROSCOPE";
	
	SensorManager sm;
	private String lastGyroscopeMeasurement;
	private Thread sampler;
	private HandlerThread mHandlerThread;

	public GyroscopeLogger(Context context, LogWriter writer) {
		super(context,writer);
		sm = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
	}
	
	@Override
	public void start() {
		super.start();
		
		Sensor sensor = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		if(sensor != null){
			if (sampleRate > 0) {
				sampler = new Thread(new Runnable() {
					public void run() {
						boolean run = true;
						while(run) {
							try {
								Thread.sleep(sampleRate*1000);
								writer.writeSample(LOG_TAG_SENSOR_GYROSCOPE, lastGyroscopeMeasurement);
							} catch (InterruptedException e) {
								run = false;
							}
						}
					}
				});
				
				sampler.start();
			}
			mHandlerThread = new HandlerThread("AccelerometerLogListener");
			mHandlerThread.start();
			Handler handler = new Handler(mHandlerThread.getLooper());
			sm.registerListener(sensorlistener,sensor,SensorManager.SENSOR_DELAY_FASTEST, handler); 
			isSampling = true;
			Log.d(LOG_TAG_SENSOR_GYROSCOPE, "Gyroscope logging enabled");
		}
	}
	
	@Override 
	public void stop() {
		super.stop();
		Log.d(LOG_TAG_SENSOR_GYROSCOPE, "Gyroscope logging stopped1");
		if (isSampling) {
			isSampling = false;
			Log.d(LOG_TAG_SENSOR_GYROSCOPE, "Gyroscope logging stopped2");
			Sensor sensor = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
			Log.d(LOG_TAG_SENSOR_GYROSCOPE, "Gyroscope logging stopped3");
			sm.unregisterListener(sensorlistener, sensor);
			Log.d(LOG_TAG_SENSOR_GYROSCOPE, "Gyroscope logging stopped5");
			if(mHandlerThread.isAlive())
				mHandlerThread.quit();
			if (sampleRate > 0) sampler.interrupt();
			Log.d(LOG_TAG_SENSOR_GYROSCOPE, "Gyroscope logging stopped6");
		}		
	}

	/**
	 * Sensor value change listener
	 */
	private SensorEventListener sensorlistener = new SensorEventListener() {
		public void onAccuracyChanged(Sensor arg0, int arg1) {}

		public void onSensorChanged(SensorEvent event) {
			long currentTime = System.currentTimeMillis();
			long timeStampInMillis = currentTime - SystemClock.uptimeMillis() + (event.timestamp/1000000) + timeOffset;
			StringBuilder measurement = new StringBuilder();
			measurement.append("t=");
			measurement.append(timeStampInMillis);
			measurement.append(";sT=");
			measurement.append(event.timestamp);
			measurement.append(";cT=");
			measurement.append(currentTime);
			measurement.append(";x=" + Float.toString(event.values[0]));
			measurement.append(";y=" + Float.toString(event.values[1]));
			measurement.append(";z=" + Float.toString(event.values[2]));
			lastGyroscopeMeasurement = measurement.toString();

			if (sampleRate == 0 && isSampling) {
				writer.writeSample(LOG_TAG_SENSOR_GYROSCOPE, measurement);
			}
		}
	};
}