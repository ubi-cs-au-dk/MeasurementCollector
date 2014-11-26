package dk.au.measurementcollector.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import dk.au.measurementcollector.Toaster;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

public class MapLoader {
	
	/**
	 * Loads and decodes a bitmap from a file.
	 * If the file cannot be loaded, a toast is printed with the reason
	 * 
	 * @param f File to load
	 * @return On success, bitmap from File f, otherwise null
	 */
	public static Bitmap loadMap(File f){
		if(f.exists() && f.canRead()){
			Bitmap b = BitmapFactory.decodeFile(f.getAbsolutePath());
			if(b != null)
				return b;
			else 
				Toaster.showToast("Could not decode file: " + f.toString());
		} else {
			if(!f.exists())
				Toaster.showToast("File: " + f.toString() + " does not exist!");
			else
				Toaster.showToast("Application does not have read access to file: " + f.toString());
		}
		return null;
	}
	
	/**
	 * Loads a list of map files from a File configFile.
	 * If configFile is unaccessible or non-existing, the list will be empty
	 * 
	 * @param configFile File containing a newline separated list of image files
	 * @return List of files from configFile, or empty if unaccessible.
	 */
	public static List<File> getMapFiles(File configFile){
		List<File> files = new ArrayList<File>();
		if(configFile.exists() && configFile.canRead()){
			try{
				BufferedReader in = new BufferedReader(new FileReader(configFile));
				String line = null;
				while((line = in.readLine()) != null){
					files.add(new File(line));
				}
				in.close();
			} catch (IOException e){
				Toaster.showToast("An error occured while reading map list file.");
				Log.e("Logger", "Error while reading map list: " + e.getMessage());
			} 
		} else {
			Log.d("Logger", "Could not read map list file: " + configFile.toString());
		}
		return files;
	}
}
