package dk.au.measurementcollector;
/**
 * Wifi scanner is scanning continuously every 1 sec. This is the case no matter
 * the sampling interval, so results can be fetched if needed
 */
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import dk.au.perpos.core.Consumer;

public class WiFiTask {

	private Consumer<String> consumer;
	private Context context;
	private WifiManager wifi;
	private Timer timer = null;
	private boolean weStartedWiFi = false;

	public WiFiTask(Context service, Consumer<String> consumer) {
		this.context = service;
		this.consumer = consumer;
		this.wifi = (WifiManager) this.context
				.getSystemService(Context.WIFI_SERVICE);
	}

	BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			List<ScanResult> results = wifi.getScanResults();
			WifiInfo info = wifi.getConnectionInfo();
			StringBuffer buffer = new StringBuffer(";pos=");
			if(GroundTruth.isSet())
				buffer.append(GroundTruth.get());
			buffer.append(";id="+ info.getMacAddress());
			if (results != null) {
				for (ScanResult result : results) {
					buffer.append(";" + result.BSSID + "=" + result.level + ","
							+ result.frequency + ",3,0"); // TODO: Removed MBK: The substring is a hack to correct for APs at daimi with multiple MACs.
				}
			}
			consumer.consume(buffer.toString());
		}
	};

	public void startTask() {
		if (timer == null) {
			this.context.registerReceiver(broadcastReceiver, new IntentFilter(
					WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
			timer = new Timer(true);
			timer.schedule(new TimerTask() {

				@Override
				public void run() {
					scanWiFi();
				}
			}, new Date(System.currentTimeMillis()),
					2000);
		}
	}

	private void scanWiFi() {
		if (this.wifi.getWifiState() != WifiManager.WIFI_STATE_ENABLED) {
			weStartedWiFi = true;
			this.wifi.setWifiEnabled(true);
		}
		// Initiate a scan.
		wifi.startScan();
	}

	public void stopTask() {
		if (timer != null) {
			this.context.unregisterReceiver(broadcastReceiver);			
			timer.cancel();
			timer = null;
			if (weStartedWiFi) {
				wifi.setWifiEnabled(false);
			}
		}
	}
}
