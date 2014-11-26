package dk.au.measurementcollector.loggers;

import dk.au.measurementcollector.Controller;
import dk.au.measurementcollector.Toaster;
import dk.au.measurementcollector.writers.LogWriter;
import dk.au.perpos.sensing.measurements.gps.GPSMeasurement;
import dk.au.perpos.sensing.measurements.gps.SatelliteSNRMeasurement;
import dk.au.perpos.spatialsupport.position.WGS84Position;

import android.content.Context;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.GpsStatus.NmeaListener;
import android.os.Bundle;
import android.util.Log;

public class GPSLogger extends BaseLogger {

	public static final String LOG_TAG_LOCATION = "GPS_LOCATION";
	public static final String LOG_TAG_SNR = "GPS_SNR";
	public static final String LOG_TAG_NMEA = "GPS_NMEA";
	
	private LocationManager lm;
	private boolean snrLogging = false;
	private boolean locationLogging = false;
	private boolean nmeaLogging = false;
	private boolean isSampling = false;
	private Thread sampler;
	
	// Last updated data
	private GPSMeasurement lastLocation;
	private SatelliteSNRMeasurement lastSNR;
	private String lastGGA;
	
		
	public GPSLogger(Context context, LogWriter writer) {
		super(context,writer);
		this.lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
	}
	
	public void setLocationLogging(boolean status) {
		locationLogging = status;
	}
	
	public void setSNRLogging(boolean status) {
		snrLogging = status;
	}
	
	public void setNMEALogging(boolean status) {
		nmeaLogging = status;
	}
	
	@Override
	public void start() {
		super.start();
		if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			
			// Periodic sampler thread
			if (sampleRate > 0) {
				sampler = new Thread(new Runnable() {
					public void run() {
						while(true) {
							try {
								Thread.sleep(sampleRate*1000);
								long timestamp = System.currentTimeMillis() + timeOffset;
								
								if (locationLogging) {
									writer.writeSample(LOG_TAG_LOCATION, lastLocation, timestamp);
								}
								
								if (snrLogging) {
									writer.writeSample(LOG_TAG_SNR, lastSNR, timestamp);
								}
								
								if (nmeaLogging) {
									writer.writeSample(LOG_TAG_NMEA, lastGGA, timestamp);
								}
							} catch (InterruptedException e) {
								break;
							}
						}
					}
				});
				sampler.start();
			}
			
			lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
			if (snrLogging) lm.addGpsStatusListener(statusListener);
			if (nmeaLogging) lm.addNmeaListener(nmeaListener);
			
			isSampling = true;
			Log.d("Logger", "GPS logging enabled");

		} else {
			Toaster.showToast("GPS is not enabled");
		}
	}

	@Override
	public void stop() {
		super.stop();
		if (isSampling) {
			if (sampleRate > 0) sampler.interrupt();
			lm.removeUpdates(locationListener);
			if (snrLogging) lm.removeGpsStatusListener(statusListener);
			if (nmeaLogging) lm.removeNmeaListener(nmeaListener);
			isSampling = false;
			Log.d(LOG_TAG_LOCATION, "GPS stopped");
		}
		
	}

	/**
	 * Location listener
	 */
	private LocationListener locationListener = new LocationListener() {
		public void onStatusChanged(String provider, int status, Bundle extras) {
			// TODO Auto-generated method stub
		}


		public void onProviderEnabled(String provider) {
			// TODO Auto-generated method stub

		}

		public void onProviderDisabled(String provider) {
			// TODO Auto-generated method stub

		}
 
		public void onLocationChanged(Location location) {
			//String outstr = "phonetime:" + System.currentTimeMillis() + " time:" + location.getTime() + " lat:" + location.getLatitude() + " long:" + location.getLongitude() + " alt:" + location.getAltitude() + " acc:" + location.getAccuracy() + " bear:" + location.getBearing() + " speed:" + location.getSpeed();
			lastLocation = new GPSMeasurement(location.getTime() + timeOffset,new WGS84Position(location.getLatitude(),location.getLongitude(),location.getAltitude()),-1,-1,location.getSpeed(),location.getBearing(),Double.NaN,Double.NaN,location.getAccuracy(),location.getAccuracy(),Double.NaN,Double.NaN,Double.NaN,location);

			if (sampleRate == 0) {
				writer.writeSample(LOG_TAG_LOCATION, lastLocation);
			}
		}
	};
	
	/**
	 * Status update listener
	 */
	private GpsStatus.Listener statusListener = new GpsStatus.Listener() {
		public void onGpsStatusChanged(int arg0) {
			GpsStatus status = lm.getGpsStatus(null);
			//StringBuffer outstr = new StringBuffer("phonetime:" + System.currentTimeMillis() + " max:" +status.getMaxSatellites() + " ttff:"+ status.getTimeToFirstFix() + " ");
			synchronized (this) {
				lastSNR = new SatelliteSNRMeasurement(System.currentTimeMillis() + timeOffset,null);
				for(GpsSatellite satellite: status.getSatellites()) {
					lastSNR.addSatellite(satellite.getPrn(), satellite.getElevation(), satellite.getAzimuth(), satellite.getSnr());
				}
			}
			
			if (sampleRate == 0) {
				writer.writeSample(LOG_TAG_SNR, lastSNR);
			}
		}		
	};
	
	private NmeaListener nmeaListener = new NmeaListener() {
		public void onNmeaReceived(long timestamp, String nmea) {
			if (nmea.startsWith("$GPGGA")) {
				lastGGA = nmea;
			}
			if (sampleRate == 0) {
				writer.writeSample(LOG_TAG_NMEA, nmea.replace("\n", ""));
			}
		}
	};
}
