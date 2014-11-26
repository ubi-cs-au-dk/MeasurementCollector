package dk.au.measurementcollector;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import dk.au.measurementcollector.utils.MapLoader;
import dk.au.measurementcollector.utils.PointS;
import dk.au.measurementcollector.views.DrawTouchImageView;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;

/**
 * Activity for mapping ground truth on a map.
 */
public class MapGroundTruthActivity extends Activity implements OnSharedPreferenceChangeListener {
	
	public static final int MAP_DIALOG = 1;
	public static final int CLEAR_POINTS_DIALOG = 2;
	public static final int CLEAR_MARKED_POINTS_DIALOG = 3;
	public static final int PICK_CONFIG_FILE_RESULT_CODE = 10001;
	public static final int PICK_MAP_FILE_RESULT_CODE = 10002;
	public static final int PICK_SAVE_POINTS_FILE_RESULT_CODE = 10003;
	public static final String PREVIOUS_MAP = "previous_map";
	public static final String MAP_LIST_FILE = "map_list_file";
	
	private CollectorApplication app;
	private File map;
	private DrawTouchImageView iv;	
	private ArrayAdapter<File> mapListAdapter;	
	private ProgressDialog prog;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    //Initialize
        app =(CollectorApplication)getApplication();        
        iv = new DrawTouchImageView(this);
	    setContentView(iv);
	    iv.setOnClickListener(click);
	    getPreferences(MODE_PRIVATE).registerOnSharedPreferenceChangeListener(this);
	    //Load previous map, or the map selection dialog
	    File f = new File(getPreferences(MODE_PRIVATE).getString(PREVIOUS_MAP,""));
	    if(!f.exists()){
	    	showDialog(MAP_DIALOG);
	    } else {
	    	showMap(f);
	    }	    
	}
	
	/**
	 * Called when the bitmap in the imageView is clicked.
	 */
	private OnClickListener click = new OnClickListener() {		
		@Override
		public void onClick(View v) {
			PointS pf = iv.getLastClickedImageCoords();
			GroundTruth.set(pf.x, pf.y, map);			
			if(!app.getService().isSampling()){
				startSampling();
			} else {
				app.getService().logMapGroundTruth();
			}
		}
	};
		
	/**
	 * Load the serialized list of clicked points, to be drawn
	 * on top of the image.
	 */
	private void loadPoints(){
		try{ 
        	ObjectInputStream oIn = new ObjectInputStream(openFileInput(map.getName() + ".pts"));
        	@SuppressWarnings("unchecked")
			List<List<PointS>> points = (List<List<PointS>>) oIn.readObject();
        	iv.setPoints(points);
        	oIn.close();
        } catch (Exception e){
        	iv.clearPoints();
        	Log.d("Logger", "Failed to load points, exception: " + e.getMessage());
        }
	}
	
	/**
	 * Serialize list of clicked points to file. A file of points is stored per image. 
	 */
	private void storePoints(){
		try{
			FileOutputStream out = openFileOutput(map.getName() + ".pts", MODE_PRIVATE);
	    	ObjectOutputStream oOut = new ObjectOutputStream(out);
	    	oOut.writeObject(iv.getPoints());
	    	oOut.close();
		} catch (Exception e){
			Log.e("Logger", "Failed to store points, exception: " + e.getMessage());
		}
	}
	
	private void startSampling(){
		prog = new ProgressDialog(MapGroundTruthActivity.this);
		prog.setProgressNumberFormat("%1d s");
		prog.setProgressPercentFormat(null);
		prog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);		
		prog.setMessage("Sampling");
		prog.setCancelable(true);
		
		final long sampletime = new Long(PreferenceManager.getDefaultSharedPreferences(MapGroundTruthActivity.this).getString(CollectorPreferencesActivity.SAMPLETIME_MAP, "30"));
		prog.setMax((int)sampletime);
		
		final CountDownTimer timer = new CountDownTimer(sampletime*1000, 30) {
			@Override
			public void onTick(long millisUntilFinished) {
				prog.setProgress((int)(sampletime - millisUntilFinished/1000));
			}
			@Override
			public void onFinish() {
				prog.cancel();
				app.stopCollection();
			}
		};		
		prog.setOnCancelListener(new DialogInterface.OnCancelListener() {						
			@Override
			public void onCancel(DialogInterface dial) {
				timer.cancel();
        		app.stopCollection();
        	}			
		});
		app.startCollection();
		timer.start();	
       	prog.show();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.mapmenu, menu);
	    return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		Object obj = menu.findItem(R.id.clearGroundTruth);	
		MenuItem clearItem = (MenuItem) obj;		
	    clearItem.setEnabled(GroundTruth.isSet());
		return super.onPrepareOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()){		
			case R.id.clearGroundTruth:
				GroundTruth.clear();
				break;
			case R.id.clearPoints:
				showDialog(CLEAR_POINTS_DIALOG);
				break;
			case R.id.clearMarkedPoints:
				showDialog(CLEAR_MARKED_POINTS_DIALOG);
				break;
			case R.id.selectMap:
				showDialog(MAP_DIALOG);
				break;
			case R.id.saveMarkedPoints:
				selectFile(PICK_SAVE_POINTS_FILE_RESULT_CODE, "Save points");
				break;
		}
        return true;
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch(id) {	
			case MAP_DIALOG:{
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle("Pick map");
				mapListAdapter = new ArrayAdapter<File>(this, android.R.layout.simple_spinner_item);    	
				mapListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
				mapListAdapter.addAll(MapLoader.getMapFiles(new File(getPreferences(MODE_PRIVATE).getString(MAP_LIST_FILE,""))));
		    	builder.setNeutralButton("Pick from file", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						selectFile(PICK_MAP_FILE_RESULT_CODE, "Select map");			
						dialog.cancel();
					}
				});	  
		    	builder.setNegativeButton("Pick map list", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						selectFile(PICK_CONFIG_FILE_RESULT_CODE, "Select map list");			
						dialog.cancel();
					}
				});	
				builder.setAdapter(mapListAdapter, new DialogInterface.OnClickListener() {			
					@Override
					public void onClick(DialogInterface dialog, int which) {
						showMap(mapListAdapter.getItem(which));
					}
				});		
				return builder.create();
			}
			case CLEAR_POINTS_DIALOG:{
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage("Clear Sampled Points?")
				       .setCancelable(false)
				       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				           public void onClick(DialogInterface dialog, int id) {
				                iv.clearClickedPoints();
				           }
				       })
				       .setNegativeButton("No", new DialogInterface.OnClickListener() {
				           public void onClick(DialogInterface dialog, int id) {
				                dialog.cancel();
				           }
				       });
				return builder.create();
			}
			case CLEAR_MARKED_POINTS_DIALOG:{
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage("Clear Marked Points?")
				       .setCancelable(false)
				       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				           public void onClick(DialogInterface dialog, int id) {
				                iv.clearLongClickedPoints();
				           }
				       })
				       .setNegativeButton("No", new DialogInterface.OnClickListener() {
				           public void onClick(DialogInterface dialog, int id) {
				                dialog.cancel();
				           }
				       });
				return builder.create();
			}
		}		
		return super.onCreateDialog(id);
	}	
	
	private void selectFile(int resultCode, String title){
		Intent intent = new Intent("org.openintents.action.PICK_FILE");
	    intent.setData(Uri.fromFile(Environment.getExternalStorageDirectory()));
	    intent.putExtra("org.openintents.extra.TITLE", title);
	    try {
	        startActivityForResult(intent,resultCode);
	    } catch (ActivityNotFoundException e) {
	    	Toaster.showToast("Please install a file browser app, such as 'OI File Manager'");
	        e.printStackTrace();
	    }
	}
	
	/**
	 * Switch to showing the map from file f
	 * Will store clicked points of old map, and load clicked points of f
	 * @param f
	 */
	private void showMap(File f){
		if(map != null)
			storePoints();
		iv.setImageBitmap(null);
		Bitmap bm = MapLoader.loadMap(f);
		if(bm != null){
			map = f;
			loadPoints();      
	        iv.setImageBitmap(bm);
	        setTitle(getString(R.string.app_name) + " - " + map.getName());
	        iv.setVisibility(View.VISIBLE);
		} else {
			iv.clearPoints();
			map = null;
			iv.setVisibility(View.INVISIBLE);
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode==RESULT_OK && data!=null && data.getData()!=null) {
			switch (requestCode) {
		        case PICK_CONFIG_FILE_RESULT_CODE:		            
	            	getPreferences(MODE_PRIVATE).edit().putString(MAP_LIST_FILE, data.getData().getPath()).commit();
	                showDialog(MAP_DIALOG);
	                break;
		        case PICK_MAP_FILE_RESULT_CODE:		      
	                showMap(new File(data.getData().getPath()));
	                break;
		        case PICK_SAVE_POINTS_FILE_RESULT_CODE:
		        	storeMarkedPoints(new File(data.getData().getPath()));
		        	break;
			}
	    }	
	}
			
	private void storeMarkedPoints(File file) {
		try{
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));
			List<PointS> markedPoints = iv.getLongClickedPoints();
			for(PointS s : markedPoints){
				writer.write(Float.toString(s.x) + "," + Float.toString(s.y) + "," + map.getName());
				writer.newLine();
			}
			writer.close();
			Toaster.showToast("Points saved");
		} catch (IOException e){
			Toaster.showToast("Error occured while writing points.");
		}
	}

	@Override
	protected void onDestroy() {
		if(prog != null && prog.isShowing()){ //If sampling, stop.
			prog.cancel();
		}
		if(map != null){
			getPreferences(MODE_PRIVATE).edit().putString(PREVIOUS_MAP, map.toString()).commit();
			storePoints();
		}		
		getPreferences(MODE_PRIVATE).unregisterOnSharedPreferenceChangeListener(this);		
		super.onDestroy();		
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if(key.equals(MAP_LIST_FILE) && mapListAdapter != null){
			mapListAdapter.clear();
			mapListAdapter.addAll(MapLoader.getMapFiles(new File(getPreferences(MODE_PRIVATE).getString(MAP_LIST_FILE,""))));
		}		
	}
}
