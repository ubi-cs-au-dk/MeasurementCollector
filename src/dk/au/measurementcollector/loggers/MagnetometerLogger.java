package dk.au.measurementcollector.loggers;

import java.util.List;

import dk.au.measurementcollector.Controller;
import dk.au.measurementcollector.writers.LogWriter;
import dk.au.perpos.sensing.measurements.Measurement;
import dk.au.perpos.sensing.measurements.compass.MagnetometerMeasurement;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.util.Log;

public class MagnetometerLogger extends BaseLogger {

	public static final String LOG_TAG_SENSOR_MAGNETOMETER = "MAGNETOMETER";

	private SensorManager sm;
	private String lastMagnetometerMeasurement;
	private Thread sampler;
	private HandlerThread mHandlerThread;
	
	public MagnetometerLogger(Context context, LogWriter writer) {
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
							writer.writeSample(LOG_TAG_SENSOR_MAGNETOMETER, lastMagnetometerMeasurement);
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
		
		Sensor sensor = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
			Log.d(LOG_TAG_SENSOR_MAGNETOMETER,"Addlistener: " + sensor);
			sm.registerListener(sensorlistener, sensor, SensorManager.SENSOR_DELAY_FASTEST, handler); //SENSOR_DELAY_NORMAL
		
		isSampling = true;
		Log.d(LOG_TAG_SENSOR_MAGNETOMETER, "Magnetometer enabled");
	}
	
	@Override 
	public void stop() {
		super.stop();
		if (isSampling) {
			isSampling = false;
			Sensor sensor = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
				sm.unregisterListener(sensorlistener, sensor);
			if(mHandlerThread.isAlive())
				mHandlerThread.quit();

			if (sampleRate > 0) sampler.interrupt();			
			Log.d(LOG_TAG_SENSOR_MAGNETOMETER, "Magnetometer stopped");
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
			lastMagnetometerMeasurement = measurement.toString();

			if (sampleRate == 0 && isSampling) {
				writer.writeSample(LOG_TAG_SENSOR_MAGNETOMETER, measurement);
			}
		}
	};
}
