package dk.au.measurementcollector;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.PorterDuff.Mode;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.LinearLayout.LayoutParams;
import dk.au.measurementcollector.SamplerService.RecordingChangedListener;
import dk.au.measurementcollector.SamplerService.SamplingStartedListener;

public class Controller extends Activity implements ServiceConnection {

	private static final String LOG_TAG = "Controller";
	private CollectorApplication app;

	private Button startButton;
	private View contentView;
	private ProgressDialog prog;
	private SamplerService samplerService;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(LOG_TAG, "Creating logging controller");

		// Bind to sampler service
		app = (CollectorApplication) getApplication();
		app.BindToService(this);

		// Initialize preferences if needed
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

		// Setup view
		setContentView(R.layout.main);

		// Setup Toast
		Toaster.setContext(this);

		// Register button listeners
		startButton = (Button) findViewById(R.id.startButton);
		startButton.setOnClickListener(startButtonListener);

		Button mapButton = (Button) findViewById(R.id.MapButton);
		mapButton.setOnClickListener(mapListener);

		Button voiceButton = (Button) findViewById(R.id.VoiceButton);
		voiceButton.setOnClickListener(voiceButtonListener);

		Button userTimestamp = (Button) findViewById(R.id.UserTimestampButton);
		userTimestamp.setOnClickListener(timestampListener);

		Button sendData = (Button) findViewById(R.id.SendButton);
		sendData.setOnClickListener(sendListener);
		
		contentView = findViewById(R.id.contentlayout);		
	}

	private View.OnClickListener timestampListener = new OnClickListener() {
		public void onClick(View v) {
			samplerService.logTimestamp();
		}
	};
	
	private View.OnClickListener sendListener = new OnClickListener() {
		public void onClick(View v) {
			String folderPref = PreferenceManager.getDefaultSharedPreferences(Controller.this).getString(CollectorPreferencesActivity.OUTPUT_FOLDER, "");
			File outputFolder = folderPref.equals("") ? Environment.getExternalStorageDirectory() : new File(folderPref);
			File[] files = outputFolder.listFiles();
			if(files.length == 0){
				Toaster.showToast("Found no data");
			} else {
				File newest = Collections.max(Arrays.asList(files), new Comparator<File>(){
					@Override
					public int compare(File arg0, File arg1) {
						return (int)(arg0.lastModified() - arg1.lastModified());
					}});
				File zipped = new File(newest.getPath() + ".zip");
				{
					int BUFFER_SIZE = 1024;
					BufferedInputStream origin = null;
				    try {
				    	ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipped)));
					    byte data[] = new byte[BUFFER_SIZE];
	
				            FileInputStream fi = new FileInputStream(newest);    
				            origin = new BufferedInputStream(fi, BUFFER_SIZE);
				            try {
				                ZipEntry entry = new ZipEntry(newest.toString().substring(newest.toString().lastIndexOf("/") + 1));
				                out.putNextEntry(entry);
				                int count;
				                while ((count = origin.read(data, 0, BUFFER_SIZE)) != -1) {
				                    out.write(data, 0, count);
				                }
				            }
				            finally {
				                origin.close();
				                out.close();
				            }
				    } catch(Exception e){
				    
				    }
				}

				Intent intent = new Intent(Intent.ACTION_SENDTO,Uri.fromParts(
			            "mailto","blunck@cs.au.dk", null));
				//intent.setType("text/plain");
				//intent.putExtra(Intent.EXTRA_EMAIL, new String[] {"blunck@cs.au.dk"});
				intent.putExtra(Intent.EXTRA_SUBJECT, "Data");
				intent.putExtra(Intent.EXTRA_TEXT, "Attached");			
				Uri uri = Uri.fromFile(zipped);
				intent.putExtra(Intent.EXTRA_STREAM, uri);
				startActivity(Intent.createChooser(intent, "Send email..."));
			}
		}
	};

	private View.OnClickListener voiceButtonListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			if (samplerService.isRecording()) {
				samplerService.stopRecording();
			} else {
				samplerService.startRecording();
			}
			setVoiceButtonState(samplerService.isRecording());
		}
	};

	private View.OnClickListener mapListener = new OnClickListener() {
		public void onClick(View v) {
			Intent intent = new Intent(Controller.this,
					MapGroundTruthActivity.class);
			startActivity(intent);
		}
	};

	RecordingChangedListener voiceListener = new RecordingChangedListener() {
		@Override
		public void notifyRecordingChanged(boolean recording) {
			setVoiceButtonState(recording);
		}
	};

	View.OnClickListener buttonEventListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			Button b = (Button) v;
			String event = b.getText().toString();
			samplerService.logButtonEvent(event);
		}
	};

	@Override
	protected void onResume() {
		super.onResume();
		updateVisibility(CollectorPreferencesActivity.LOGGING_USER_TIMESTAMP,
				R.id.UserTimestampButtonView);
		updateVisibility(CollectorPreferencesActivity.LOGGING_MAP_GROUND_TRUTH,
				R.id.MapButtonView);
		updateVisibility(CollectorPreferencesActivity.LOGGING_VOICE,
				R.id.VoiceButtonView);
		boolean isEventButtonsEnabled = updateVisibility(
				CollectorPreferencesActivity.LOGGING_BUTTON_EVENT,
				R.id.flowLayoutView);
		if (isEventButtonsEnabled)
			generateEventButtons();
	}

	private boolean updateVisibility(String preference, int viewId) {
		Boolean enabled = PreferenceManager.getDefaultSharedPreferences(this)
				.getBoolean(preference, false);
		View view = findViewById(viewId);
		view.setVisibility(enabled ? View.VISIBLE : View.GONE);
		return enabled;
	}

	private void generateEventButtons() {
		LinearLayout layoutView = (LinearLayout) findViewById(R.id.flowLayoutView);
		layoutView.removeAllViews();
		String eventList = PreferenceManager.getDefaultSharedPreferences(this)
				.getString(CollectorPreferencesActivity.BUTTON_EVENT_LIST, "");
		String[] lineArray = eventList.split(";;");
		layoutView.setLayoutParams(new LinearLayout.LayoutParams(
				LayoutParams.FILL_PARENT, 0, lineArray.length));
		for (String ls : lineArray) {
			LinearLayout lineLayout = new LinearLayout(this);
			lineLayout.setOrientation(LinearLayout.HORIZONTAL);
			lineLayout.setLayoutParams(new LinearLayout.LayoutParams(
					LayoutParams.FILL_PARENT, 0, 1.0f));
			lineLayout.setGravity(Gravity.CENTER_VERTICAL);
			String[] eventArray = ls.split(";");
			for (String s : eventArray) {
				Button b = new Button(this);
				b.setLayoutParams(new LinearLayout.LayoutParams(0,
						LayoutParams.WRAP_CONTENT, 1.0f));
				b.setText(s);
				b.setOnClickListener(buttonEventListener);
				lineLayout.addView(b);
			}
			layoutView.addView(lineLayout);
		}
	}

	/**
	 * Shut down logging gracefully if closing Activity
	 */
	@Override
	protected void onDestroy() {
		if(samplerService != null)
			samplerService.removeRecordingListener(voiceListener);
		app.informDestroyed();
		super.onDestroy();
	}

	/**
	 * Start/Stop data collection button handler
	 */
	private OnClickListener startButtonListener = new OnClickListener() {
		public void onClick(View v) {
			if (!samplerService.isSampling()) {
				samplerService.addSamplingStartedListener(startedListener);
				startButton.setEnabled(false);
				showProgressSpinner();
				app.startCollection();
			} else {
				AlertDialog.Builder builder = new AlertDialog.Builder(
						Controller.this);
				builder.setMessage("Are you sure?")
						.setPositiveButton("Yes", stopClickListener)
						.setNegativeButton("No", stopClickListener).show();
			}
		}
	};
	
	private void showProgressSpinner(){
		prog = new ProgressDialog(Controller.this);
		prog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		prog.setMessage("Starting");
		prog.setCancelable(false);
		prog.show();
	}

	SamplingStartedListener startedListener = new SamplingStartedListener() {
		@Override
		public void notifySamplingStarted() {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					startButton.setEnabled(true);
					setSamplingState(true);
					app.getService().removeSamplingStartedListener(
							startedListener);
					prog.cancel();
				}
			});
		}
	};

	DialogInterface.OnClickListener stopClickListener = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
			case DialogInterface.BUTTON_POSITIVE:
				app.stopCollection();
				setSamplingState(false);
				break;
			case DialogInterface.BUTTON_NEGATIVE:
				break;
			}
		}
	};
	
	private void setSamplingState(boolean sampling){
		setButtonState(startButton, sampling, "Collector is running\nTap to stop", "Collector is offline\nTap to start");
		contentView.setVisibility(sampling ? View.VISIBLE : View.GONE);
	}

	private void setButtonState(Button b, boolean running, String onText,
			String offText) {
		if (running) {
			b.setText(onText);
			b.getBackground().setColorFilter(0xFF00FF00, Mode.MULTIPLY);
		} else {
			b.setText(offText);
			b.getBackground().setColorFilter(0xFFFF0000, Mode.MULTIPLY);
		}
	}

	private void setVoiceButtonState(boolean state) {
		Button voiceButton = (Button) findViewById(R.id.VoiceButton);
		setButtonState(voiceButton, state, "Recording ON \n Tap to stop", "Recording OFF \n Tap to start");
	}

	/**
	 * Called when SamplerService is connected
	 */
	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		app.getService().registerRecordingListener(voiceListener);
		samplerService = app.getService();
		startButton.setEnabled(true);
		setSamplingState(samplerService.isSampling());
		setVoiceButtonState(samplerService.isRecording());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_settings:
			startActivity(new Intent(Controller.this,
					CollectorPreferencesActivity.class));
			break;
		}
		return true;
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
	}

}