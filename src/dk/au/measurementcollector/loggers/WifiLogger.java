package dk.au.measurementcollector.loggers;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;
import dk.au.measurementcollector.Toaster;
import dk.au.measurementcollector.WiFiTask;
import dk.au.measurementcollector.writers.LogWriter;
import dk.au.perpos.core.Consumer;

public class WifiLogger extends BaseLogger {

	public static final String LOG_TAG_WIFI = "WIFI";
	
    private WiFiTask wifitask;
    private WifiManager wm;
	private Thread sampler;
    
	private String lastScan;
	
	public WifiLogger(Context context, LogWriter writer) {
		super(context, writer);
		wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        this.wifitask = new WiFiTask(this.context,this.wifilistener);
	}
	
	@Override
	public void start() {
		super.start();
		if (wm.isWifiEnabled()) {

			// Periodic sampler thread
			if (sampleRate > 0) {
				sampler = new Thread(new Runnable() {
					public void run() {
						boolean run = true;
						while(run) {
							try {
								Thread.sleep(sampleRate*1000);
								writer.writeSample(LOG_TAG_WIFI, lastScan);
							} catch (InterruptedException e) {
								run = false;
							}
						}
					}
				});
				
				sampler.start();
			}
			
			this.wifitask.startTask();
			isSampling = true;
			Log.d("Logger", "Wifi logging enabled");
			
		} else {
			Toaster.showToast("Wifi is not enabled");
		}
	}

	@Override
	public void stop() {
		if (isSampling) {
			if (sampleRate > 0) sampler.interrupt();
			wifitask.stopTask();
			isSampling = false;
			Log.d("Logger", "Wifi logging stopped");
		}
		super.stop();		
	}

	private Consumer<String> wifilistener = new Consumer<String>()  {
		public void consume(String str) {
			lastScan = "t=" + (System.currentTimeMillis() + timeOffset) + str;

			if (sampleRate == 0 && isSampling) {
				writer.writeSample(LOG_TAG_WIFI, lastScan);
			}
		}
	};
}
