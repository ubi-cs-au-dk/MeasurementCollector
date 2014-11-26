package dk.au.measurementcollector.views;

import java.util.ArrayList;
import java.util.List;

import dk.au.measurementcollector.utils.PointS;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.view.HapticFeedbackConstants;

/**
 * Extends TouchImageView to draw dots on clicked coordinates, and to only fire onClick events 
 * when the image itself it clicked.
 */
public class DrawTouchImageView extends TouchImageView {
	
	public static final int CLICKED =0;
	public static final int LONG_CLICKED =1;
	
	public static final float DOT_SIZE_DP = 3.0f;	
	
	private int dot_size = (int) (getResources().getDisplayMetrics().density*DOT_SIZE_DP+0.5f);

	List<List<PointS>> points = new ArrayList<List<PointS>>(2);	
	List<Paint> paints = new ArrayList<Paint>();	
	
	public DrawTouchImageView(Context context) {
		super(context);
		points.add(new ArrayList<PointS>());
		points.add(new ArrayList<PointS>());
		addPaint(Color.CYAN);
		addPaint(Color.RED);		
	}
	
	private void addPaint(int color){
		Paint p1 = new Paint();
		p1.setAntiAlias(true);
		p1.setColor(color);
		paints.add(p1);
	}
	
	public List<PointS> getClickedPoints(){
		return points.get(CLICKED);
	}

	public void setClickedPoints(List<PointS> pts){
		points.set(CLICKED, pts);
		invalidate();
	}
	
	public void clearClickedPoints(){
		points.get(CLICKED).clear();
		invalidate();
	}
	
	public List<PointS> getLongClickedPoints(){
		return points.get(LONG_CLICKED);
	}

	public void setLongClickedPoints(List<PointS> pts){
		points.set(LONG_CLICKED, pts);
		invalidate();
	}
	
	public void clearLongClickedPoints(){
		points.get(LONG_CLICKED).clear();
		invalidate();
	}
	
	public List<List<PointS>> getPoints(){
		return points;
	}
	
	public void setPoints(List<List<PointS>> p){
		points = p;
		invalidate();
	}
	
	public void clearPoints(){
		points.get(CLICKED).clear();
		points.get(LONG_CLICKED).clear();
		invalidate();
	}

	@Override
	public boolean performClick() {
		PointS clicked = getLastClickedImageCoords();
    	if(clicked.x >= 0 && clicked.y >= 0 && clicked.x <= bmWidth && clicked.y <= bmHeight){
    		points.get(CLICKED).add(getLastClickedImageCoords());
    		return super.performClick();
    	}
    	return true;
	}
	
	@Override
	public boolean performLongClick() {
		PointS clicked = getLastClickedImageCoords();
    	if(clicked.x >= 0 && clicked.y >= 0 && clicked.x <= bmWidth && clicked.y <= bmHeight){
    		points.get(LONG_CLICKED).add(getLastClickedImageCoords());
    		performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
    		return super.performLongClick();
    	}
    	return true;
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		for(int i=0; i<points.size(); i++){
			for(PointS p : points.get(i)){
				float[] cp = {p.x, p.y};
				matrix.mapPoints(cp);
				canvas.drawCircle(cp[0], cp[1], dot_size, paints.get(i));			
			}		
		}
	}
	
	public PointS getLastClickedImageCoords(){
		Matrix inv = new Matrix();
		matrix.invert(inv);
		float[] cs = {last.x, last.y};
		inv.mapPoints(cs);
		return new PointS(cs[0], cs[1]);
	}
}
