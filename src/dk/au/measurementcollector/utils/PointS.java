package dk.au.measurementcollector.utils;

import java.io.Serializable;

/**
 * A serializable point
 */
public class PointS implements Serializable{

	private static final long serialVersionUID = 8623988052281112285L;
	
	public float x, y;
	
	public PointS(float x, float y){
		this.x = x;
		this.y = y;
	}
}
