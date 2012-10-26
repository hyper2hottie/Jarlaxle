package realms.jarlaxle.movingcircle;

import realms.entreri.bluetoothutility.R;
import realms.jarlaxle.movingcircle.MovingCircleView.MovingCircleThread;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class MovingCircleFragment extends Fragment {
	
	/** Used for debug messages */
	public static final boolean d = true;
	
	/** A handle to the thread that runs the circle. */
    private MovingCircleThread circleThread;
    
    /** A handle to the view where the circle is running. */
    private MovingCircleView circleView;
    
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);        
        
    }
    
    //---------------------ACTIVITY LIFECYCLE----------------------------------
    
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {
		if(d)
			Log.d("MovingCircleFragment", "Entered onCreateView");
		//Inflate the view to return
		View toReturn = inflater.inflate(R.layout.circle_fragment, container, false);	
		
		//Get the circle view and thread
        circleView = (MovingCircleView) toReturn.findViewById(R.id.movingCircle);
        circleThread = circleView.getThread();
        
		return toReturn;
	}
 
    /**
     * Invoked when Activity loses user focus.
     */
    @Override
	public void onPause()
    {
    	if(d)
			Log.d("MovingCircleFragment", "Entered onPause");
    	super.onPause();
    	
    	
    }
    
    /**
     * Invoked when Activity regains focus
     */
    @Override
	public void onResume()
    {
    	if(d)
			Log.d("MovingCircleFragment", "Entered onResume");
    	super.onResume();
    	circleView.getThread().unpause();
    		
    }   
    
    /**
     * Invoked when fragment is destroyed.
     */
    @Override
    public void onDestroy()
    {
    	super.onDestroy();
    	//Release the thread lock so it can quit
    	circleView.getThread().setState(MovingCircleThread.STATE_STOPPING);
    	synchronized (circleView.getThread()) {
    		circleView.getThread().notifyAll();
		}
    	boolean retry = true;
        circleView.getThread().setRunning(false);
        while (retry) {
            try {
            	circleView.getThread().join();
                retry = false;
            } catch (InterruptedException e) {
            }
        }		
    }
    
    //------------------------------------------------------------------------------
    
    
    
    //------------------------CIRCLE MANAGEMENT-------------------------------------
    
    /**
	 * Get circle offset.  Used to get the offset from center of the screen
	 * Note: Towards the right of the screen is a positive x offset and towards 
	 * the top is a positive y offset.
	 * @return offset of drawn circle in format [x,y] from center of screen
	 */
    public Pair<Integer, Integer> getCircleOffset()
    {
    	if(circleThread != null)
    		return circleThread.getCircleOffset();
    	else
    		return null;
    }
    
    /**
	 * This function is used for adding a listener.  All listeners will
	 * be called whenever the circles location changes
	 * @param listener - the object that wants calls from this circle
	 */
	public void registerListener(MovingCircleListener listener)
	{
		circleView.registerListener(listener);
	}
	
	/**
	 * This function removes a listener.
	 * @param listener - the object that no longer wants to recieve updates
	 */
	public void unregisterListener(MovingCircleListener listener)
	{
		circleView.unregisterListener(listener);
	}
    
    //------------------------------------------------------------------------------
}
