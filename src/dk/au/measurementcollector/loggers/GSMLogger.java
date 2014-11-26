package dk.au.measurementcollector.loggers;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.telephony.NeighboringCellInfo;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import dk.au.measurementcollector.Controller;
import dk.au.measurementcollector.Toaster;
import dk.au.measurementcollector.writers.LogWriter;
import dk.au.perpos.sensing.measurements.gsm.GSMSignalStrength;

public class GSMLogger extends BaseLogger {

	public static final String LOG_TAG_GSM = "GSM";

	private TelephonyManager tm;
	private Thread sampler;

	private List<GSMSignalStrength> lastScan = new ArrayList<GSMSignalStrength>();

	public GSMLogger(Context context, LogWriter writer) {
		super(context,writer);
		tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
	}

	// GSM is always on, no need to query antenna status
	@Override
	public void start() {
		super.start();
		// Periodic sampler thread
		if (sampleRate > 0) {
			sampler = new Thread(new Runnable() {
				public void run() {
					boolean run = true;
					while(run) {
						try {
							Thread.sleep(sampleRate*1000);

							GsmCellLocation gsmCell = (GsmCellLocation)(tm.getCellLocation());					
							if(gsmCell != null) {
								lastScan.clear();

								GSMSignalStrength gsmsignalstrength = new GSMSignalStrength(System.currentTimeMillis(),gsmCell.getCid(),gsmCell.getLac(),-1,tm.getNetworkType(),signalStrength.getGsmSignalStrength(),signalStrength.getGsmBitErrorRate());					
								lastScan.add(gsmsignalstrength);

								List<NeighboringCellInfo> naboer = tm.getNeighboringCellInfo();
								for (NeighboringCellInfo info : naboer) {
									gsmsignalstrength = new GSMSignalStrength(System.currentTimeMillis() + timeOffset,info.getCid(),info.getLac(),info.getPsc(),info.getNetworkType(),info.getRssi(),-1);
									lastScan.add(gsmsignalstrength);
								}

							}
							writer.writeSample(LOG_TAG_GSM, lastScan);
						} catch (InterruptedException e) {
							run = false;
						}
					}
				}
			});
			sampler.start();
		}

		// Register for signal strength notifications.
		tm.listen(gsmListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
		Log.d("Logger", "GSM logging enabled");
	}

	@Override
	public void stop() {
		super.stop();
		if (sampleRate > 0) sampler.interrupt();
		tm.listen(gsmListener, PhoneStateListener.LISTEN_NONE);
		Log.d("Logger", "GSM logging stopped");
	}

	public static SignalStrength signalStrength;
	private PhoneStateListener gsmListener = new PhoneStateListener() {

		@Override
		public void onSignalStrengthsChanged(SignalStrength signalStrength) {
			GSMLogger.signalStrength = signalStrength; //To be used in start()
			
			synchronized (lastScan) {
				lastScan.clear();

				GsmCellLocation gsmCell = (GsmCellLocation)(tm.getCellLocation());					
				if(gsmCell != null) {
					GSMSignalStrength gsmsignalstrength = new GSMSignalStrength(System.currentTimeMillis() + timeOffset,gsmCell.getCid(),gsmCell.getLac(),-1,tm.getNetworkType(),signalStrength.getGsmSignalStrength(),signalStrength.getGsmBitErrorRate());					
					lastScan.add(gsmsignalstrength);

					List<NeighboringCellInfo> naboer = tm.getNeighboringCellInfo();
					for (NeighboringCellInfo info : naboer) {
						gsmsignalstrength = new GSMSignalStrength(System.currentTimeMillis() + timeOffset,info.getCid(),info.getLac(),info.getPsc(),info.getNetworkType(),info.getRssi(),-1);
						lastScan.add(gsmsignalstrength);

					}

					if (sampleRate == 0) {
						writer.writeSample(LOG_TAG_GSM, lastScan);
					}

				} else {
					Toaster.showToast("No GSM Cell info");
				}
			}
		}		
	};
}