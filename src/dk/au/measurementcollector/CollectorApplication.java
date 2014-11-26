package dk.au.measurementcollector;

import dk.au.measurementcollector.SamplerService.ServiceBinder;
import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

/**
 * Handles connection to the SamplerService, in order to make it
 * accessible to all child activities.
 */
public class CollectorApplication extends Application {
	private ServiceConnection conn;
	private SamplerService core;	// Null if not connected to the service
	private ServiceConnection listener;
	private static final String LOG_TAG = "Application";

	/**
	 * Bind to the SamplerService
	 * @param list A listener which will be notified when the service is connected
	 */
	public void BindToService(ServiceConnection list) {	   
		this.listener = list;
		ComponentName comp = new ComponentName(getPackageName(), SamplerService.class.getName());
		Intent service = new Intent().setComponent(comp);
    	Log.d(LOG_TAG, "Creating ServiceConnection");
        conn = new ServiceConnection() {
			public void onServiceDisconnected(ComponentName name) {
				core = null;
				if(listener != null)
					listener.onServiceDisconnected(name);
		        Log.d(LOG_TAG, "SamplerService disconnected");
			}
			
			public void onServiceConnected(ComponentName name, IBinder service) {
				core = (SamplerService) ((ServiceBinder) service).getService();
				if(listener != null)
					listener.onServiceConnected(name, service);
				Log.d(LOG_TAG, "Connected to SamplerService");
			}
		};
		
		if (!bindService(service, conn, BIND_AUTO_CREATE)) {
        	Log.e(LOG_TAG, "Could not bind to SamplerService");
        } 
    }
		
	/**
	 * Called by Controller class to inform that it is being destroyed
	 * This allows the application to unbind the SamplerService.
	 * The service cannot be unbound by onDestroy in this class, since it 
	 * will not be called while the service is still connected.
	 */
	public void informDestroyed(){
		// Destroy service if not sampling
		if (!core.isSampling()) {
			//Stop service
			ComponentName comp = new ComponentName(this.getPackageName(), SamplerService.class.getName());
	    	Intent i = new Intent().setComponent(comp);
	    	stopService(i);
		}
		unbindService(conn);
		Log.d(LOG_TAG, "Disconnected from SamplerService");
	}	
	
	
	/**
	 * Start logging metrics based on settings.
	 * 
	 * Note: All EditTextPrferences are stored as Strings even if they are 
	 * limited to integer input.
	 */
	public void startCollection() {
		// Start service
		ComponentName comp = new ComponentName(this.getPackageName(), SamplerService.class.getName());
    	Intent i = new Intent().setComponent(comp);
    	startService(i);
	}
	
	public void stopCollection() {
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				core.stopSampling();	
				Log.d("SAD", "Gyroscope logging stopped66");
			}
		}).run();
		
	}
	
	public SamplerService getService(){
		return core;
	}
	
}
