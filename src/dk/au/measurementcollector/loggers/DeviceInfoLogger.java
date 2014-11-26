package dk.au.measurementcollector.loggers;
/**
 * Class
 */
import java.util.List;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.SystemClock;
import dk.au.measurementcollector.Controller;
import dk.au.measurementcollector.writers.LogWriter;


public class DeviceInfoLogger extends BaseLogger {

	public static final String LOG_TAG_DEVICE_INFO = "DEVICE_INFO";

	
	public DeviceInfoLogger(Context context, LogWriter writer) {
		super(context, writer);
	}

	
	@Override
	public void start() {
		super.start();
		logInfo();
	}
	
	private void logInfo(){
		StringBuilder deviceInfo = new StringBuilder();
		deviceInfo.append("NtpOffset: " + timeOffset);
		deviceInfo.append("CurrentTimeMillis: " + System.currentTimeMillis());
		deviceInfo.append("ElapsedRealTime: " + SystemClock.elapsedRealtime());
		deviceInfo.append("Brand: " + Build.BRAND + ";");
		deviceInfo.append("Serial: " + Build.SERIAL + ";");
		deviceInfo.append("Device: " + Build.DEVICE + ";");
		deviceInfo.append("Hardware: " + Build.HARDWARE + ";");
		deviceInfo.append("Manufacturer" + Build.MANUFACTURER + ";");
		deviceInfo.append("Build ID: " + Build.ID + ";");
		deviceInfo.append("Model: " + Build.MODEL + ";");
		deviceInfo.append("Product: " + Build.PRODUCT + ";");
		deviceInfo.append("Radio: " + Build.RADIO + ";");
		deviceInfo.append("Version codename: " + Build.VERSION.CODENAME + ";");
		deviceInfo.append("Release version: " + Build.VERSION.RELEASE + ";");
		deviceInfo.append("SDK version: " + Build.VERSION.SDK_INT + ";");
		SensorManager manager = (SensorManager)context.getSystemService(context.SENSOR_SERVICE);
		List<Sensor> sensors = manager.getSensorList(Sensor.TYPE_ALL);
		deviceInfo.append("Sensors: [");
		for(Sensor s : sensors){
			deviceInfo.append("[");
			deviceInfo.append("Type: " + s.getType() + ";");
			deviceInfo.append("Name: " + s.getName() + ";");
			deviceInfo.append("Max Range: " + s.getMaximumRange() + ";");
			deviceInfo.append("Min Delay: " + s.getMinDelay() + ";");
			deviceInfo.append("Power use: " + s.getPower() + ";");
			deviceInfo.append("Resolution: " + s.getResolution() + ";");
			deviceInfo.append("Vendor: " + s.getVendor() + ";");
			deviceInfo.append("Version: " + s.getVersion() + ";");
			deviceInfo.append("];");
		}
		deviceInfo.append("];");		
		writer.writeSample(LOG_TAG_DEVICE_INFO, deviceInfo.toString());
	}
}
