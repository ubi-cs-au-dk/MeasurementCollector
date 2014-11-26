package dk.au.measurementcollector.utils;

import java.util.ArrayList;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

public class HandsetButtonReceiver extends BroadcastReceiver {
	
	private static List<HandsetButtonListener> listeners = new ArrayList<HandsetButtonListener>();
	
	public static void addListener(HandsetButtonListener l){
		listeners.add(l);
	}
	
	public static void removeListener(HandsetButtonListener l){
		listeners.remove(l);
	}

	public HandsetButtonReceiver() {
        super();
    }
	
	@Override
	public void onReceive(Context context, Intent intent) {
		String intentAction = intent.getAction();
	    if (!intentAction.equals(Intent.ACTION_MEDIA_BUTTON)){
	        return;
	    }
	    KeyEvent event = (KeyEvent)intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
	    if (event == null) {
	        return;
	    }
	    int action = event.getAction();
	    if (action == KeyEvent.ACTION_DOWN) {
	        Log.d("BUTTON", "BUTTON PRESSED DOWN");
	        for(HandsetButtonListener l : listeners)
	        	l.notifyHandsetButtonPressed();
	    }
	    abortBroadcast();
	}
	
	public interface HandsetButtonListener {
		public void notifyHandsetButtonPressed();
	}

}
