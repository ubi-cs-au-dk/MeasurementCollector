package dk.au.measurementcollector.loggers;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import dk.au.measurementcollector.Controller;
import dk.au.measurementcollector.Toaster;
import dk.au.measurementcollector.writers.LogWriter;
import dk.au.perpos.sensing.measurements.Measurement;
import dk.au.perpos.sensing.measurements.gps.GPSMeasurement;
import dk.au.perpos.spatialsupport.position.WGS84Position;

public class NetworkLocationLogger extends BaseLogger {

	public static final String LOG_TAG_NETWORK_LOCATION = "NETWORK_LOCATION";

	private LocationManager lm;
	private Thread sampler;
	private Measurement lastLocation;

	public NetworkLocationLogger(Context context, LogWriter writer) {
		super(context, writer);
		this.lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
	}
	
	public Measurement getLastScan() {
		return lastLocation;
	}

	public void start() {
		super.start();

		if (!lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
			Toaster.showToast("Network locations is not enabled");
			return;
		}
		
		if (sampleRate > 0) {
			if (sampleRate > 0) {
				sampler = new Thread(new Runnable() {
					public void run() {
						boolean run = true;
						while(run) {
							try {
								Thread.sleep(sampleRate*1000);
								writer.writeSample(LOG_TAG_NETWORK_LOCATION, lastLocation);
							} catch (InterruptedException e) {
								run = false;
							}
						}
					}
				});
				
				sampler.start();
			}
			
		} 
		
		// Initiate location listener
		lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
		isSampling = true;
		Log.d("Logger", "Network location logging enabled");
	}

	public void stop() {
		super.stop();
		if (isSampling) {
			if (sampleRate > 0) sampler.interrupt();
			lm.removeUpdates(locationListener);
			isSampling = false;
			Log.d("Logger", "Network location logging stopped");
		}
	}

	private LocationListener locationListener = new LocationListener() {
		public void onStatusChanged(String provider, int status, Bundle extras) {}

		public void onProviderEnabled(String provider) {}

		public void onProviderDisabled(String provider) {}
 
		public void onLocationChanged(Location location) {
			lastLocation = new GPSMeasurement(location.getTime() + timeOffset,new WGS84Position(location.getLatitude(),location.getLongitude(),location.getAltitude()),-1,-1,location.getSpeed(),location.getBearing(),Double.NaN,Double.NaN,location.getAccuracy(),location.getAccuracy(),Double.NaN,Double.NaN,Double.NaN,location);

			if (sampleRate == 0) {
				writer.writeSample(LOG_TAG_NETWORK_LOCATION, lastLocation);
			}
		}
	};
}
