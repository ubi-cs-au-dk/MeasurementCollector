package dk.au.measurementcollector;

import java.io.File;

/**
 * Keeps track of the current ground truth as a singleton.
 */
public class GroundTruth {
	private float x, y;
	private File map;
	
	private static GroundTruth groundTruth = null;
	
	private GroundTruth(float x, float y, File map){
		this.x = x;
		this.y = y;
		this.map = map;		
	}
	
	public static GroundTruth get(){		
		return groundTruth;
	}
	
	public static void set(float x, float y, File map){		
		groundTruth = new GroundTruth(x, y, map);
	}
	
	public static void clear(){
		groundTruth = null;
	}
	
	public static boolean isSet(){
		return groundTruth != null;
	}
	
	public float getX(){
		return x;
	}
	
	public float getY(){
		return y;
	}
	
	public File getMap(){
		return map;
	}
	
	@Override
	public String toString() {
		return Double.toString(x) + "," + Double.toString(y) + "," + map.getName();
	}
}
