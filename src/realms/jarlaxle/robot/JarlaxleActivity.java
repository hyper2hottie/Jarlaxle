package realms.jarlaxle.robot;

import realms.jarlaxle.movingcircle.MovingCircleListener;
import realms.jarlaxle.bluetoothutility.BluetoothClass;
import realms.jarlaxle.movingcircle.MovingCircleFragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class JarlaxleActivity extends Activity  implements MovingCircleListener{
	
	/** The moving circle fragment that the user sees*/
	protected MovingCircleFragment mCircleFragment = new MovingCircleFragment();
	
	/** The bluetooth class that will be used to communicate over bluetooth */
	private BluetoothClass mBluetooth;
	
	/** Dialog for user to know a connection is being made. */
	private ProgressDialog mDialog;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mBluetooth = new BluetoothClass(this, mHandler);
        if(!mBluetooth.isBluetoothAvailable())
		{
			Toast.makeText(this, "Bluetooth is not available.", Toast.LENGTH_LONG).show();
			finish();
		}
    }
    
	
	@Override
	public void onStart()
	{
		super.onStart();
		if(mBluetooth.isConnected())
		{
			mBluetooth.setDeviceName("bToothTest");
			mBluetooth.autoConnect();
		}
	}
	
	/**
	 * Activity is becoming visible.
	 */
	public void onResume()
	{
		super.onResume();
		
		//Register this as a listener to the MovingCircleView
    	MovingCircleFragment fragment = (MovingCircleFragment)getFragmentManager().findFragmentById(R.id.movingCircleFragment);
    	fragment.registerListener(this);
	}
	
	/**
	 * Activity is no longer visible.
	 */
	public void onPaue()
	{
		super.onPause();
		
		//unregister this as a listener to the MovingCircleView
    	MovingCircleFragment fragment = (MovingCircleFragment)getFragmentManager().findFragmentById(R.id.movingCircleFragment);
    	fragment.unregisterListener(this);
	}
	
	
	
	/** Activity is done.  Cancel threads. */
	public void onDestroy()
	{
		super.onDestroy();
		if(mBluetooth != null)
		{
			mBluetooth.cancel();
			mBluetooth = null;
		}
		
		if(mDialog != null)
		{
			mDialog.cancel();
			mDialog = null;
		}
	}
	
  //-------------------------------------Dialogs-------------------------------------
	
  	/**
  	 * Display a dialog indicating that a connection is being made.
  	 */
  	private void displayConnectingDialog()
  	{
  		mDialog = new ProgressDialog(this);
  		mDialog.setMessage("Connecting Bluetooth");
  		mDialog.setCanceledOnTouchOutside(true);
  		mDialog.setOnCancelListener(cancelConnectingListener);
  		mDialog.setButton("Cancel", dialogButtonListener);
  		mDialog.show();
  	}
  	  	
  	/**
  	 * Listener for buttons in dialog indicating bluetooth progress.
  	 */
  	private final OnClickListener dialogButtonListener = new OnClickListener()
  	{
  		public void onClick(DialogInterface dialog, int which)
  		{
  			switch(which)
  			{
  			case -1 :
  				//Cancel connecting
  				mBluetooth.cancel();
  				if(mDialog != null)
  					mDialog.dismiss();
  				mDialog = null;
  				break;
  			}
  			
  		}
  		
  	};
  	
  	/**
  	 * Listener for dialog cancel, cancels making a connection.
  	 */
  	private final OnCancelListener cancelConnectingListener = new OnCancelListener()
  	{
  		public void onCancel(DialogInterface dialog) 
  		{
  			//Cancel connecting
  			mBluetooth.cancel();
  			if(mDialog != null)
  				mDialog.dismiss();
  			mDialog = null;
  			
  		}
  	};
  	
    
  //--------------------------Handlers-----------------------------------------------
  	
  	/**
  	 * This is called by a button in the main layout.  It will
  	 * connect automatically to a default device.
  	 * @param view - The calling button.
  	 */
  	public void autoConnect(View view)
  	{
  		displayConnectingDialog();
  		mBluetooth.setDeviceName("robot-AAC2");
  		mBluetooth.autoConnect();
  	}
  	
  	/**
  	 * This is called by a button in the main layout.  It will
  	 * display a dialog for that allows the user to select a device to connect to.
  	 * @param view - The calling button.
  	 */
  	public void manualConnect(View view)
  	{
  		final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.select_dialog_singlechoice);
  		AlertDialog.Builder builder = new AlertDialog.Builder(this);
  		builder.setTitle("Select a Device to Connect");
  		builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
			
			public void onClick(DialogInterface dialog, int which) {
				String temp = adapter.getItem(which);
				String macAddress = temp.split("\n")[1];
				mBluetooth.connect(macAddress);
				displayConnectingDialog();
			}
		});
  		AlertDialog alert = builder.create();
  		alert.show();
  		mBluetooth.manuallConnect(adapter);
  	}
  	
  	/**
  	 * This class will handle any messages that come from
  	 * the bluetooth class.
  	 */
  	private final Handler mHandler = new Handler()
  	{
  		@Override
  		public void handleMessage(Message msg)
  		{
  				switch(msg.what)
  				{
  				case BluetoothClass.MESSAGE_CONNECTION_COMPLETE:
  					if(mDialog != null)
  					{
  						mDialog.dismiss();
  						mDialog = null;
  					}
  					break;
  				case BluetoothClass.MESSAGE_DEVICE_NAME:
  					String deviceName = msg.getData().getString(BluetoothClass.BUNDLE_DEVICE_NAME); 
  					((TextView)findViewById(R.id.device)).setText(deviceName);
  					break;
  				case BluetoothClass.MESSAGE_CONNECTION_LOST:
  					Toast.makeText(getApplicationContext(), "Connection Lost", Toast.LENGTH_LONG).show();
  					((TextView)findViewById(R.id.device)).setText("No Connection");
  					break;
  				case BluetoothClass.MESSAGE_ERROR_CONNECTING:
  					Toast.makeText(getApplicationContext(), "No device found", Toast.LENGTH_LONG).show();
  					mBluetooth.cancel();
  					if(mDialog != null)
  						mDialog.dismiss();
  					mDialog = null;
  					break;
  				}
  						
  		}
  	};
  	
  	/**
     * When the circle moves then we want to change
     * the value in the text box.
     */
    public void onCircleMoved(final int X, final int Y) {
    	runOnUiThread(new Runnable()
		{
			public void run() {
				sendRightLeft(X, Y);
			}
			
		});
	}
  	
  	//---------------------------------------------------------------------------------
  	
  //--------------------------Bluetooth Management-----------------------------------
	
  	/**
  	 * Sends data over bluetooth.
  	 * @param data - The data to write to the bluetooth stream
  	 */
  	protected void sendData(char data)
  	{
  		if(mBluetooth != null && mBluetooth.isConnected())
  		{
  			mBluetooth.write(data);
  		}
  	}
  	
  	//---------------------------------------------------------------------------------
  	
  	//-----------------------Robot Conversion------------------------------------------
  	
  	/**
  	 * Converts X and Y values into values that can be used by the robot.
  	 * @param X - The x coordinate for conversion.
  	 * @param Y - The y coordinate for conversion.
  	 */
  	protected void sendRightLeft(int X, int Y)
  	{
  		int xSign = 1;
  		if(X < 0)
  		{
  			xSign = -1;
  		}
  		double magnitude = Math.abs(Y);
  		int right = 0;
  		int left = 0;
  		
  		if(Y >=0)
  		{
  			right = (int) Math.sqrt(Math.abs(magnitude*magnitude - X*X*xSign));
  	  		left = (int) Math.sqrt(Math.abs(magnitude*magnitude + X*X*xSign));
  			
  	  		if((magnitude*magnitude - X*X*xSign) > 0)
  			{
  				right += 128;
  			}

  			if((magnitude*magnitude + X*X*xSign) > 0)
  			{
  				left += 128;
  			}
  		}
  		else if(Y < 0)
  		{
  			right = (int) Math.sqrt(Math.abs(-1*magnitude*magnitude + X*X*xSign));
  	  		left = (int) Math.sqrt(Math.abs(-1*magnitude*magnitude - X*X*xSign));
  	  		
  			if(-1*magnitude*magnitude + X*X*xSign > 0)
  			{
  				right += 128;
  			}
  			
  			if(-1*magnitude*magnitude - X*X*xSign > 0)
  			{
  				left += 128;
  			}  		
  		}
  		
  		char rightSend = (char)right;
  		char leftSend = (char)left;

  		((TextView)findViewById(R.id.position)).setText(X + ", " + Y + ", " + right + ", " + left);
  		sendData(rightSend);
  		sendData(leftSend);
  		
  	}
  	
  	//---------------------------------------------------------------------------------

}