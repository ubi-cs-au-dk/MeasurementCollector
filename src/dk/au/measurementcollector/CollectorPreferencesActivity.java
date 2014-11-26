package dk.au.measurementcollector;
/**
 * Activity that controls various properties about the data collector.
 * Note that the SamplingService needs to be restarted before changes can take
 * effect.
 * 
 * @author Christian Melchior
 */
import java.io.File;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceManager.OnActivityResultListener;

public class CollectorPreferencesActivity extends PreferenceActivity {

	// Preference keys
	public static final String SYNCHRONIZED = "synchronized";
	public static final String COMBINED = "combined";
	
	public static final String LOGGING_GPS = "gps";
	public static final String LOGGING_SNR = "snr";
	public static final String LOGGING_NMEA = "nmea";
	public static final String LOGGING_NETWORK_LOCATION = "networkLocation";
	public static final String LOGGING_WIFI = "wifi";
	public static final String LOGGING_GSM = "gsm";
	public static final String LOGGING_VOICE = "voice";
	public static final String LOGGING_BUTTON_EVENT = "buttonEvent";
	public static final String LOGGING_MAGNETOMETER = "magnetometer";
	public static final String LOGGING_ACCELEROMETER = "accelerometer";
	public static final String LOGGING_GYROSCOPE = "gyroscope";
	public static final String LOGGING_ORIENTATION = "orientation";
	public static final String LOGGING_USER_TIMESTAMP = "userTimestamp";
	public static final String LOGGING_MAP_GROUND_TRUTH = "mapGroundTruth";
	public static final String LOGGING_DEVICE_INFO = "deviceInfo";
	public static final String OUTPUT_FOLDER = "output_folder";
	
	public static final String SAMPLERATE_SYNCHED = "shared_interval";
	public static final String SAMPLERATE_LOCATION = "samplerate_location";
	public static final String SAMPLERATE_WIFI = "samplerate_wifi";
	public static final String SAMPLERATE_GSM = "samplerate_gsm";
	public static final String SAMPLERATE_SENSOR = "samplerate_sensor";
	public static final String SAMPLETIME_MAP = "sampletime";
	public static final String VIBRATOR = "vibrator";
	
	public static final String BUTTON_EVENT_LIST = "buttonEventList";
	
	public static final String NTP_ENABLED = "ntp";
	
	public static final int SELECT_OUTPUT_FOLDER_RETURN_CODE = 5000;
	
	EditTextPreference wifiSampleRate;
	EditTextPreference locationSampleRate;
	EditTextPreference gsmSampleRate;
	EditTextPreference sensorSampleRate;
	EditTextPreference mapSampleTime;
	Preference outputFolder;
	
	private CheckBoxPreference synchronizedSamples;
	private CheckBoxPreference gpsLogger;
	private CheckBoxPreference snrLogger;
	private CheckBoxPreference nmeaLogger;
	private CheckBoxPreference userTimestampLogger;
	private CheckBoxPreference mapGroundTruth;
	private CheckBoxPreference ntpEnabled;
	private CheckBoxPreference buttonEvent;
	private CheckBoxPreference voice;
	private CheckBoxPreference deviceInfo;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        wifiSampleRate = (EditTextPreference) getPreferenceScreen().findPreference("samplerate_wifi");
        locationSampleRate = (EditTextPreference) getPreferenceScreen().findPreference("samplerate_location");
        gsmSampleRate = (EditTextPreference) getPreferenceScreen().findPreference("samplerate_gsm");
        sensorSampleRate = (EditTextPreference) getPreferenceScreen().findPreference("samplerate_sensor");
        mapSampleTime = (EditTextPreference) getPreferenceScreen().findPreference(SAMPLETIME_MAP);        

        gpsLogger = (CheckBoxPreference) getPreferenceScreen().findPreference(LOGGING_GPS);
        snrLogger = (CheckBoxPreference) getPreferenceScreen().findPreference(LOGGING_SNR);
        nmeaLogger = (CheckBoxPreference) getPreferenceScreen().findPreference(LOGGING_NMEA);
        
        userTimestampLogger = (CheckBoxPreference) getPreferenceScreen().findPreference(LOGGING_USER_TIMESTAMP);
        voice = (CheckBoxPreference) getPreferenceScreen().findPreference(LOGGING_VOICE);
        deviceInfo = (CheckBoxPreference) getPreferenceScreen().findPreference(LOGGING_DEVICE_INFO);
        buttonEvent = (CheckBoxPreference) getPreferenceScreen().findPreference(LOGGING_BUTTON_EVENT);
        mapGroundTruth = (CheckBoxPreference) getPreferenceScreen().findPreference(LOGGING_MAP_GROUND_TRUTH);        
        mapGroundTruth.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {			
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				if(!(Boolean)newValue)
					GroundTruth.clear();
				return true;
			}
		});
        
        ntpEnabled = (CheckBoxPreference) getPreferenceScreen().findPreference(NTP_ENABLED);
             
        synchronizedSamples = (CheckBoxPreference) getPreferenceScreen().findPreference("synchronized");
        synchronizedSamples.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				
				// Disable/Enable all sample rates depending on this state
				wifiSampleRate.setEnabled(!(Boolean)newValue);
				locationSampleRate.setEnabled(!(Boolean)newValue);
				gsmSampleRate.setEnabled(!(Boolean)newValue);
				sensorSampleRate.setEnabled(!(Boolean)newValue);
				
				return true;
			}
		});
        
        outputFolder = (Preference)getPreferenceScreen().findPreference(OUTPUT_FOLDER);
        outputFolder.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				Intent intent = new Intent("org.openintents.action.PICK_DIRECTORY");
				File outFolderFile = null;
				if(outputFolder.getSharedPreferences().contains(OUTPUT_FOLDER))
					outFolderFile = new File(outputFolder.getSharedPreferences().getString(OUTPUT_FOLDER, ""));
				if(outFolderFile == null || !outFolderFile.exists())
					outFolderFile = Environment.getExternalStorageDirectory();				
			    intent.setData(Uri.fromFile(outFolderFile));				
			    intent.putExtra("org.openintents.extra.TITLE", "Select output Folder");
			    try {
			        startActivityForResult(intent,SELECT_OUTPUT_FOLDER_RETURN_CODE);
			    } catch (ActivityNotFoundException e) {
			    	Toaster.showToast("Please install a file browser app, such as 'OI File Manager'");
			        e.printStackTrace();
			    }
				return true;
			}
		});
        
        
	}	
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {		
		switch (requestCode) {
		    case SELECT_OUTPUT_FOLDER_RETURN_CODE:
		    	if (resultCode==RESULT_OK && data!=null && data.getData()!=null) {
		    		outputFolder.getEditor().putString(OUTPUT_FOLDER, data.getData().getPath()).commit();		            			                
		    	}
                break;
	    }
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	

}