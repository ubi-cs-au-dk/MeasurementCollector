package dk.au.measurementcollector;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

/**
 * Uniform access to displaying toasts from the application.
 * 
 * @author Christian Melchior
 *
 */
public class Toaster {

	private static Context baseContext;
	
	public static void setContext(Context context) {
		baseContext = context;
	}
	
	public static void showToast(final String msg) {
		//TODO What about services?
		if (baseContext instanceof Activity) {
			Activity activity = (Activity) baseContext;
			activity.runOnUiThread(new Runnable() {
				public void run() {
					Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show();
				}
			});
		}
	}
}

