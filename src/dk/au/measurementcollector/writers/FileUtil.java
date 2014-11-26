package dk.au.measurementcollector.writers;
/**
 * Wrapper for interacting with log files
 * 
 * This wrapper depends on External storage being available. If testing through
 * the emulator, remember to create a usable SDCard. See: 
 * http://developer.android.com/guide/developing/tools/othertools.html#mksdcard
 * 
 * @author Christian Melchior
 */
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import dk.au.measurementcollector.Toaster;

import android.os.Environment;
import android.util.Log;

public class FileUtil {

	/**
	 * Create a new log file. Usual naming scheme is <name>_<type>.log, but 
	 * name can be defined in LogWriters.
	 * 
	 * @param fileName		Name of log file.
	 * @return File writer
	 */
	public static BufferedWriter open(String fileName, File folder) {
		BufferedWriter writer = null;
		try {
            if (folder != null && folder.exists() && folder.canWrite()){
            	writer = new BufferedWriter(new FileWriter(new File(folder, fileName)));
           } else {
        	   Log.e("Logger", "Output directory not available");
        	   Toaster.showToast("Output directory not available. No logging possible.");
           }
        	   
        } catch (IOException e) {
        	Log.e("Logger", "Could not create file " + e.getMessage());
        	Toaster.showToast("Could not create log file " + e.getMessage());
        }       
        
        return writer;
	}
	
	/**
	 * Write sample to log file
	 * 
	 * @param logData
	 * @param file
	 */
	public static void writeToLog(String logData, BufferedWriter file) {
		if (file == null) return;
		synchronized (file) {
			try {
				file.write(logData);
				//file.flush();
			} catch (IOException e) {
				Toaster.showToast("Could not write to file " + e.getMessage());
			}
		}
	}

	/**
	 * Close log file
	 * @param file
	 */
	public static void close(BufferedWriter file) {
		if (file == null) return;
		synchronized(file) {
			try {
				file.close();
			} catch (IOException e) {
				Toaster.showToast("Could not close file " + e.getMessage());
			}
		}
	}
}
