package dk.au.measurementcollector;
/**
 * Service responsible for sampling selected sensors.
 * TODO Do initialization in separate loggers?
 */
import dk.au.measurementcollector.loggers.AccelerometerLogger;
import dk.au.measurementcollector.loggers.ActivityRecognitionLogger;
import dk.au.measurementcollector.loggers.ButtonEventLogger;
import dk.au.measurementcollector.loggers.DeviceInfoLogger;
import dk.au.measurementcollector.loggers.GPSLogger;
import dk.au.measurementcollector.loggers.GSMLogger;
import dk.au.measurementcollector.loggers.GyroscopeLogger;
import dk.au.measurementcollector.loggers.MagnetometerLogger;
import dk.au.measurementcollector.loggers.MapGroundTruthLogger;
import dk.au.measurementcollector.loggers.NetworkLocationLogger;
import dk.au.measurementcollector.loggers.OrientationLogger;
import dk.au.measurementcollector.loggers.UserTimestampLogger;
import dk.au.measurementcollector.loggers.VoiceLogger;
import dk.au.measurementcollector.loggers.WifiLogger;
import dk.au.measurementcollector.writers.LogWriter;
import dk.au.measurementcollector.writers.StandardLogWriter;
import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.*;
import android.preference.PreferenceManager;
import android.util.Log;
import dk.au.measurementcollector.loggers.*;
import dk.au.measurementcollector.utils.HandsetButtonReceiver;
import dk.au.measurementcollector.utils.HandsetButtonReceiver.HandsetButtonListener;
import dk.au.measurementcollector.utils.NTPSync;
import dk.au.measurementcollector.writers.ASyncLogWriter;
import dk.au.measurementcollector.writers.LogWriter;
import dk.au.measurementcollector.writers.StandardLogWriter;

import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

public class SamplerService extends Service {

	private static final String LOG_TAG ="SamplerService";
	private IBinder binder = new ServiceBinder();
	private boolean isSampling = false;
	
    private GPSLogger gpsLogger;
    private NetworkLocationLogger networkLocationLogger;
    private WifiLogger wifiLogger;
    private GSMLogger gsmLogger;
    private MagnetometerLogger magnetometerLogger;
    private AccelerometerLogger accelerometerLogger;
    private OrientationLogger orientationLogger;
    private UserTimestampLogger userTimestampLogger;
    private MapGroundTruthLogger mapGroundTruthLogger;
    private ButtonEventLogger buttonEventLogger;
    private VoiceLogger voiceLogger;
    private Vibrator vibrator;
    private DeviceInfoLogger deviceInfoLogger;
    private GyroscopeLogger gyroscopeLogger;
    private ActivityRecognitionLogger activityLogger;
    
    SharedPreferences preferences;
	
	/**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     * 
     * @see http://developer.android.com/resources/samples/ApiDemos/src/com/example/android/apis/app/LocalService.html
     */
    public class ServiceBinder extends Binder {
        SamplerService getService() {
            return SamplerService.this;
        }
    }
	
	@Override
	public IBinder onBind(Intent arg0) {
		return binder;
	}
	
	HandsetButtonReceiver buttonReceiver;
	AudioManager audioManager;
	ComponentName compName;

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(LOG_TAG, "SamplerService created");		
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(LOG_TAG, "SamplerService started");
		new AsyncTask<Void,Void,Void> () {
			@Override
			protected Void doInBackground(Void... params) {
				startSampling();
				return null;
			} }.execute();
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d(LOG_TAG, "SamplerService destroyed");
	}
	
	HandsetButtonReceiver receiver;

	public void startSampling() {
		
		// Fetch new preferences
		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		
		// Determine loggers
		boolean isSynched = preferences.getBoolean(CollectorPreferencesActivity.SYNCHRONIZED, false);
		boolean isCombined = preferences.getBoolean(CollectorPreferencesActivity.COMBINED, false);
		
		boolean gps = preferences.getBoolean(CollectorPreferencesActivity.LOGGING_GPS, false);
		boolean snr = preferences.getBoolean(CollectorPreferencesActivity.LOGGING_SNR, false);
		boolean nmea = preferences.getBoolean(CollectorPreferencesActivity.LOGGING_NMEA, false);
		boolean networkLocation = preferences.getBoolean(CollectorPreferencesActivity.LOGGING_NETWORK_LOCATION, false);
		boolean wifi = preferences.getBoolean(CollectorPreferencesActivity.LOGGING_WIFI, false);
		boolean gsm = preferences.getBoolean(CollectorPreferencesActivity.LOGGING_GSM, false);
		boolean magnetometer = preferences.getBoolean(CollectorPreferencesActivity.LOGGING_MAGNETOMETER, false);
		boolean accelerometer = preferences.getBoolean(CollectorPreferencesActivity.LOGGING_ACCELEROMETER, false);
		boolean orientation = preferences.getBoolean(CollectorPreferencesActivity.LOGGING_ORIENTATION, false);
		boolean userTimestamp = preferences.getBoolean(CollectorPreferencesActivity.LOGGING_USER_TIMESTAMP, false);
		boolean mapGroundTruth = preferences.getBoolean(CollectorPreferencesActivity.LOGGING_MAP_GROUND_TRUTH, false);
		boolean syncWithNTP = preferences.getBoolean(CollectorPreferencesActivity.NTP_ENABLED, false);
		boolean buttonEvent = preferences.getBoolean(CollectorPreferencesActivity.LOGGING_BUTTON_EVENT, false);
		boolean voice = preferences.getBoolean(CollectorPreferencesActivity.LOGGING_VOICE, false);
		boolean vibratorOn = preferences.getBoolean(CollectorPreferencesActivity.VIBRATOR, false);
		boolean deviceInfo = preferences.getBoolean(CollectorPreferencesActivity.LOGGING_DEVICE_INFO, false);
		boolean gyroscope = preferences.getBoolean(CollectorPreferencesActivity.LOGGING_GYROSCOPE, false);
		boolean activity = preferences.getBoolean(CollectorPreferencesActivity.LOGGING_ACTIVITY, false);
		
		boolean playServices = checkGooglePlayServices(this);
		if(activity && !playServices){
			Log.d("PlayServices", "Play services not found!");
			Toaster.showToast("Play services not up to date!");
			activity = false;
		}
			
		String folderPref = preferences.getString(CollectorPreferencesActivity.OUTPUT_FOLDER, "");
		File outputFolder = folderPref.equals("") ? Environment.getExternalStorageDirectory() : new File(folderPref);
		
		Integer sampleRate;
		Integer syncedSampleRate = new Integer(preferences.getString(CollectorPreferencesActivity.SAMPLERATE_SYNCHED, "0"));
		long timeOffset = 0L;
		
		// Sync with NTP        
        if(syncWithNTP) {
	        try {
				timeOffset = (long) NTPSync.getTimeOffset();
			} catch (UnknownHostException e) {
				Toaster.showToast("Error: NTP Server unknown");
			} catch (SocketException e) {
				Toaster.showToast("Error: Connection to NTP Server failed");
			} catch (IOException e) {
				Toaster.showToast("Error: Unknown error with NTP sync");
			}
        }
        LogWriter combinedWriter = new ASyncLogWriter("samples", outputFolder, true, timeOffset);
		// GPS
		if (gps || snr || nmea) {
	        gpsLogger = new GPSLogger(this, new ASyncLogWriter("experiement_gps", outputFolder, false, timeOffset));
			sampleRate = new Integer(preferences.getString(CollectorPreferencesActivity.SAMPLERATE_LOCATION, "0"));
			if (isSynched) sampleRate = syncedSampleRate;
			if (isCombined) gpsLogger.setLogWriter(combinedWriter);
			gpsLogger.setLocationLogging(gps);
			gpsLogger.setSNRLogging(snr);
			gpsLogger.setNMEALogging(nmea);
			gpsLogger.setSampleRate(sampleRate);
			gpsLogger.setTimeOffset(timeOffset);
			gpsLogger.start();
		}

		// Network location
		if (networkLocation) {
			networkLocationLogger = new NetworkLocationLogger(this, new ASyncLogWriter("experiement_networklocation", outputFolder, false, timeOffset));
			sampleRate = new Integer(preferences.getString(CollectorPreferencesActivity.SAMPLERATE_LOCATION, "0"));
			if (isSynched) sampleRate = syncedSampleRate;
			if (isCombined) networkLocationLogger.setLogWriter(combinedWriter);
			networkLocationLogger.setSampleRate(sampleRate);
			networkLocationLogger.setTimeOffset(timeOffset);
			networkLocationLogger.start();
		}
		
		// Wifi
		if (wifi) {
	        wifiLogger = new WifiLogger(this, new ASyncLogWriter("experiement_wifi", outputFolder, false, timeOffset));
			sampleRate = new Integer(preferences.getString(CollectorPreferencesActivity.SAMPLERATE_WIFI, "0"));
			if (isSynched) sampleRate = syncedSampleRate;
			if (isCombined) wifiLogger.setLogWriter(combinedWriter);
			wifiLogger.setSampleRate(sampleRate);
			wifiLogger.setTimeOffset(timeOffset);
			wifiLogger.start();
		}
		
		// GSM
		if (gsm) {
	        gsmLogger = new GSMLogger(this, new ASyncLogWriter("experiement_gsm", outputFolder, false, timeOffset));
			sampleRate = new Integer(preferences.getString(CollectorPreferencesActivity.SAMPLERATE_GSM, "0"));
			if (isSynched) sampleRate = syncedSampleRate;
			if (isCombined) gsmLogger.setLogWriter(combinedWriter);
			gsmLogger.setSampleRate(sampleRate);
			gsmLogger.setTimeOffset(timeOffset);
			gsmLogger.start();
		}
		
		// Sensors
		if (magnetometer) {
			magnetometerLogger = new MagnetometerLogger(this, new ASyncLogWriter("sensor_magnetometer", outputFolder, false, timeOffset));
			sampleRate = new Integer(preferences.getString(CollectorPreferencesActivity.SAMPLERATE_SENSOR, "0"));
			if (isSynched) sampleRate = syncedSampleRate;
			if (isCombined) magnetometerLogger.setLogWriter(combinedWriter);
			magnetometerLogger.setSampleRate(sampleRate);
			magnetometerLogger.setTimeOffset(timeOffset);
			magnetometerLogger.start();
			
		}

		if (accelerometer) {
			accelerometerLogger = new AccelerometerLogger(this, new ASyncLogWriter("sensor_accelerometer", outputFolder, false, timeOffset));
			sampleRate = new Integer(preferences.getString(CollectorPreferencesActivity.SAMPLERATE_SENSOR, "0"));
			if (isSynched) sampleRate = syncedSampleRate;
			if (isCombined) accelerometerLogger.setLogWriter(combinedWriter);
			accelerometerLogger.setSampleRate(sampleRate);
			accelerometerLogger.setTimeOffset(timeOffset);
			accelerometerLogger.start();
			
		}
		
		if (gyroscope) {
			gyroscopeLogger = new GyroscopeLogger(this, new ASyncLogWriter("sensor_gyroscope", outputFolder, false, timeOffset));
			sampleRate = new Integer(preferences.getString(CollectorPreferencesActivity.SAMPLERATE_SENSOR, "0"));
			if (isSynched) sampleRate = syncedSampleRate;
			if (isCombined) gyroscopeLogger.setLogWriter(combinedWriter);
			gyroscopeLogger.setSampleRate(sampleRate);
			gyroscopeLogger.setTimeOffset(timeOffset);
			gyroscopeLogger.start();			
		}
		
		if (activity) {
			activityLogger = new ActivityRecognitionLogger(this, new StandardLogWriter("sensor_activity", outputFolder, false, timeOffset));
			if (isCombined) activityLogger.setLogWriter(combinedWriter);
			activityLogger.setSampleRate(0);
			activityLogger.setTimeOffset(timeOffset);
			activityLogger.start();			
		}

		if (orientation) {
			orientationLogger = new OrientationLogger(this, new ASyncLogWriter("sensor_orientation", outputFolder, false, timeOffset));
			sampleRate = new Integer(preferences.getString(CollectorPreferencesActivity.SAMPLERATE_SENSOR, "0"));
			if (isSynched) sampleRate = syncedSampleRate;
			if (isCombined) orientationLogger.setLogWriter(combinedWriter);
			orientationLogger.setSampleRate(sampleRate);
			orientationLogger.setTimeOffset(timeOffset);
			orientationLogger.start();
		}
		
		if (userTimestamp) {
			userTimestampLogger = new UserTimestampLogger(this, new ASyncLogWriter("user_timestamp", outputFolder, false, timeOffset));
			if (isCombined) userTimestampLogger.setLogWriter(combinedWriter);
			userTimestampLogger.setTimeOffset(timeOffset);
			userTimestampLogger.start();
		}
		
		if (buttonEvent) {
			buttonEventLogger = new ButtonEventLogger(this, new ASyncLogWriter("button_event", outputFolder, false, timeOffset));
			if (isCombined) buttonEventLogger.setLogWriter(combinedWriter);
			buttonEventLogger.setTimeOffset(timeOffset);
			buttonEventLogger.start();
		}
		
		if(voice){
			voiceLogger = new VoiceLogger(this, new ASyncLogWriter("voice", outputFolder, false, timeOffset), outputFolder, "voice");
			if(isCombined) voiceLogger.setLogWriter(combinedWriter);
			voiceLogger.setTimeOffset(timeOffset);
			voiceLogger.start();
			HandsetButtonReceiver.addListener(buttonListener);
		}
		
		if(vibratorOn){
			vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);			
		} else {
			vibrator = null;
		}
		
		if(mapGroundTruth){
			mapGroundTruthLogger = new MapGroundTruthLogger(this, new ASyncLogWriter("map_ground_truth", outputFolder, false, timeOffset));
			if (isCombined) mapGroundTruthLogger.setLogWriter(combinedWriter);
			mapGroundTruthLogger.setTimeOffset(timeOffset);
			mapGroundTruthLogger.start();
		}
		if(deviceInfo){
			deviceInfoLogger = new DeviceInfoLogger(this, new ASyncLogWriter("device_info", outputFolder, false, timeOffset));
			if(isCombined) deviceInfoLogger.setLogWriter(combinedWriter);
			deviceInfoLogger.start();
		}
		
		Log.d(LOG_TAG, "Sampling started");
		isSampling = true;
		for(SamplingStartedListener l : startedListeners){
			l.notifySamplingStarted();
		}
	}
	
	public static boolean checkGooglePlayServices(Context context) {
	    int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
	    if (resultCode != ConnectionResult.SUCCESS) {
	        return false;
	    }
	    return true;
	}
	
	public void logButtonEvent(String event){
		if(buttonEventLogger != null){
			buttonEventLogger.logButtonEvent(event);
		}
		
	}
	
	HandsetButtonListener buttonListener = new HandsetButtonListener() {
		@Override
		public void notifyHandsetButtonPressed() {
			if(isRecording())
				stopRecording();
			else 
				startRecording();
		} 		
	};

	/**
	 * Log timestamp on user request
	 */
	public void logTimestamp() {
		if (userTimestampLogger != null) {
			userTimestampLogger.logTimestamp();
		}
	}
	
	public void logMapGroundTruth(){
		if(mapGroundTruthLogger != null){
			mapGroundTruthLogger.logGroundTruth();
		}
	}
	
	public boolean startRecording(){
		if(voiceLogger != null){
			boolean recording = voiceLogger.startRecording();
			if(recording){
				notifyRecordingChanged(true);
				if(vibrator != null){
					long[] pattern = {0, 100, 1000};
					vibrator.vibrate(pattern, 0);
				}
			}
			return recording;
		}
		return false;
	}
	
	public void stopRecording(){
		if(voiceLogger != null){
			voiceLogger.stopRecording();
			if(vibrator != null)
				vibrator.cancel();
			notifyRecordingChanged(false);
		}
	}
	
	public boolean isRecording(){
		if(voiceLogger != null){
			return voiceLogger.isRecording();
		}
		return false;
	}
	
	private List<RecordingChangedListener> recordingListeners = new ArrayList<RecordingChangedListener>();
	public void registerRecordingListener(RecordingChangedListener l){
		recordingListeners.add(l);
	}
	
	public void removeRecordingListener(RecordingChangedListener l){
		recordingListeners.remove(l);
	}
	
	private void notifyRecordingChanged(boolean recording){
		for(RecordingChangedListener l : recordingListeners)
			l.notifyRecordingChanged(recording);
	}
	
	public interface RecordingChangedListener {
		public void notifyRecordingChanged(boolean recording);
	}
	
	public void stopSampling() {
		//unregisterReceiver(receiver);
		Log.d("BUTTON", "STOPPING");
		Log.d(LOG_TAG, "Sampling stopped");
		if (isSampling) {
			if (gpsLogger != null) gpsLogger.stop();
			if (wifiLogger != null) wifiLogger.stop();
			if (gsmLogger != null) gsmLogger.stop();
			if (accelerometerLogger != null) accelerometerLogger.stop();
			if (gyroscopeLogger != null) gyroscopeLogger.stop();
			if (magnetometerLogger != null) magnetometerLogger.stop();
			if (orientationLogger != null) orientationLogger.stop();
			if (userTimestampLogger != null) userTimestampLogger.stop();
			if (buttonEventLogger != null) buttonEventLogger.stop();
			if (networkLocationLogger != null) networkLocationLogger.stop();
			if (mapGroundTruthLogger != null) mapGroundTruthLogger.stop();
			if (voiceLogger != null){
				if(isRecording())
					stopRecording();
				voiceLogger.stop();
				HandsetButtonReceiver.removeListener(buttonListener);
			}
			if(activityLogger != null) activityLogger.stop();
			if(deviceInfoLogger != null) deviceInfoLogger.stop();
			Log.d("WAG", "Gyroscope logging stopped7");
			Toaster.showToast("Sampling stopped");
			isSampling = false;
		}		
	}
	
	public boolean isSampling() {
		return isSampling;
	}
	
	private List<SamplingStartedListener> startedListeners = new ArrayList<SamplerService.SamplingStartedListener>();
	
	public void addSamplingStartedListener(SamplingStartedListener l){
		startedListeners.add(l);
	}
	
	public void removeSamplingStartedListener(SamplingStartedListener l){
		startedListeners.remove(l);
	}
	
	public interface SamplingStartedListener {
		public void notifySamplingStarted();
	}
}