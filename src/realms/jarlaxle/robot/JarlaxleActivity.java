package realms.jarlaxle.robot;

import realms.entreri.movingcircle.MovingCircleFragment;
import android.app.Activity;
import android.os.Bundle;

public class JarlaxleActivity extends Activity {
	
	/** The moving circle fragment that the user sees*/
	protected MovingCircleFragment mCircleFragment = new MovingCircleFragment();
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }
}