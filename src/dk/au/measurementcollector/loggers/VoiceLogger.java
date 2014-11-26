package dk.au.measurementcollector.loggers;

import java.io.File;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.Environment;
import dk.au.measurementcollector.Toaster;
import dk.au.measurementcollector.writers.LogWriter;

public class VoiceLogger extends BaseLogger {
	public static final String LOG_TAG_VOICE_RECORDING = "VOICE_RECORDING";
	private long lastTimestamp;
	private File folder;
	private String prefix;
	private MediaRecorder recorder = new MediaRecorder();
	
	public VoiceLogger(Context context, LogWriter writer, File folder, String prefix) {
		super(context, writer);
		this.folder = folder;
		this.prefix = prefix;
	}

	public Object getLastScan() {
		return lastTimestamp;
	}

	public boolean startRecording() {
		lastTimestamp = System.currentTimeMillis() + timeOffset;
		writer.writeSample(LOG_TAG_VOICE_RECORDING, lastTimestamp + " Recording started");
		recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setOutputFile(folder.toString() + "/" + prefix + "_" + lastTimestamp + ".3gp");
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        try {
			recorder.prepare();
		} catch (Exception e) {
			Toaster.showToast("Could not start audio: " + e.getMessage());
			return false;
		}
        recorder.start();
        recording = true;
        return true;
	}
	
	public void stopRecording(){
		recorder.stop();
		lastTimestamp = System.currentTimeMillis() + timeOffset;
		writer.writeSample(LOG_TAG_VOICE_RECORDING, lastTimestamp + " Recording stopped");
		recording = false;
	}
	
	boolean recording = false;
	
	public boolean isRecording(){
		return recording;
	}
	
	@Override
	public void stop() {
		if(isRecording())
			stopRecording();
		super.stop();
	}

}
