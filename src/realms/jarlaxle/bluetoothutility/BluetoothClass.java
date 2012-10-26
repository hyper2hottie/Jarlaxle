package realms.jarlaxle.bluetoothutility;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.ArrayAdapter;
import android.widget.Toast;

/** This class is used to create a connection over
 * Bluetooth to a device that is set.  It is meant
 * to be a general class that can be used in many situations.
 */
public class BluetoothClass {

	/** This is the UUID for serial port service connections */
	private static final UUID SerialPortServiceClass_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	
	/** The adapter on the phone, allows us to actually use bluetooth */
	private BluetoothAdapter mBluetoothAdapter = null;	
	/** Name of the device to connect to*/
	private String mRemoteDeviceName = "robot";	
	/** Device that is found and connected to */
	private BluetoothDevice mBluetoothDevice = null;
	/** State of auto-connect or manual connect */
	boolean doAutoConnect = true;
	/** Array adapter for manual connect */
	private ArrayAdapter<String> mArrayAdapter = null;
	
	//Codes
	/** Code for enabling bluetooth */
	private static final int REQUEST_ENABLE_BT = 1;
	
	//Message codes
	/** Code for connection complete */
	public static final int MESSAGE_CONNECTION_COMPLETE = 1;
	/** Code for the name of the currently connected device */
	public static final int MESSAGE_DEVICE_NAME = 2;
	/** Code indicating that a connection has been lost */
	public static final int MESSAGE_CONNECTION_LOST = 3;
	/** Code indicating connection failure */
	public static final int MESSAGE_ERROR_CONNECTING = 4;
	
	//Bundle keys
	/** Code for a bundle key, name of the device */
	public static final String BUNDLE_DEVICE_NAME = "deviceName";
	
	//Handles
	/** The activity that contains this class */
	private Activity parentActivity = null;	
	/** Threads for running/creating connections */
	private ConnectThread mConnectThread = null;
	private ConnectedThread mConnectedThread = null;
	/** Message handler for messages to the UI */
	private Handler mHandler = null;
	
	//----------------------------------General--------------------------------------------
	/** 
	 * Constructor.
	 * @param a - The activity that is calling using the class
	 * @param h - A message handler, this will be called when the state of this 
	 * 			  object changes and affects the activity
	 */
	public BluetoothClass(Activity a, Handler h)
	{
		if(a == null)
			throw new IllegalArgumentException("The parent activity can not be null.");
		else
			parentActivity = a;
		
		if(h == null)
			throw new IllegalArgumentException("The handler can not be null.");
		else
			mHandler = h;
		
		doAutoConnect = true;
	}
	
	/**
	 *  This can be used to test if bluetooth is available on the device.
	 */
	public boolean isBluetoothAvailable()
	{
		if(BluetoothAdapter.getDefaultAdapter() == null)
			return false;
		else
			return true;
	}
	
	/**
	 * Cancel all threads.
	 */
	public void cancel()
	{
		//Make sure that discovery is cancelled
		mBluetoothAdapter.cancelDiscovery();
		
		//Cancel the thread creating a connection
		if(mConnectThread != null)
		{
			mConnectThread.cancel();
			mConnectThread = null;
		}
		
		//Cancel any thread currently running a connection
		if(mConnectedThread != null)
		{
			mConnectedThread.cancel();
			mConnectedThread = null;
		}
	}
	
	//-------------------------------------------------------------------------------------
	
	//----------------------------------Configuration--------------------------------------
	
	/**
	 * Set the name of the remove device to connect to.
	 * @param name - The name of the device to connect
	 */
	public void setDeviceName(String name)
	{
		if(name == null)
			throw new IllegalArgumentException("Name can not be null");
		mRemoteDeviceName = name;
	}
	
	/**
	 * Get the name of the remote device we are connecting with.
	 * @return - Name of the current device to connect with
	 */
	public String getDeviceName()
	{
		return mRemoteDeviceName;
	}
	
	
	
	//-------------------------------------------------------------------------------------
	
	//-----------------------------------Creating a Connection-----------------------------
	
	/**
	 * Create a connection manually, by filling an array adapter.
	 * 
	 * @param adapter - An adapter that will be filled with devices to connect to.
	 */
	public void manuallConnect(ArrayAdapter<String> adapter)
	{
		mArrayAdapter = adapter;
		doAutoConnect = false;
		createConnection();
	}
	
	/**
	 * Create a connection automatically
	 */
	public void autoConnect()
	{
		doAutoConnect = true;
		createConnection();
	}
	
	/**
	 * Creates a connection.  This will handle everything, including enabling bluetooth.
	 */
	protected void createConnection()
	{
		//Try get adapter for bluetooth
		if(mBluetoothAdapter == null)
			getBluetoothAdapter();
		
		//If no adapter then we can not connect
		if(mBluetoothAdapter == null)
		{
			Toast.makeText(parentActivity, "Bluetooth not available", Toast.LENGTH_LONG).show();
			return;
		}
		
		if(!mBluetoothAdapter.isEnabled())
			mBluetoothAdapter.enable();
		
		//Register a reciever for bluetooth device discovery
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		parentActivity.registerReceiver(mAutoConnectReciever, filter);
		
		//Start Discovery
		mBluetoothAdapter.startDiscovery();		
	}
	
	/**
	 * Get the bluetooth adapter and store it.
	 */
	private void getBluetoothAdapter()
	{
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	}
	
	/**
	 * This reciever is used for when bluetooth has finished discovery or has discovered
	 * a device.  It will connect to any device that matches the name of the current
	 * device to connect with.
	 */
	public final BroadcastReceiver mAutoConnectReciever = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			String action = intent.getAction();
			
			//Check the action
			if(action.equals(BluetoothDevice.ACTION_FOUND))
			{
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				if(doAutoConnect == true && device.getName().equals(mRemoteDeviceName))
				{
					mBluetoothAdapter.cancelDiscovery();
					connect(device);
					parentActivity.unregisterReceiver(mAutoConnectReciever);
				} else if(doAutoConnect == false)
				{
					//Fill adapter
					mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
				}
			}
			else if(action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED))
			{
				mBluetoothAdapter.cancelDiscovery();
				parentActivity.unregisterReceiver(mAutoConnectReciever);
				cancel();
				
				//Send error message to UI
				Message msg = mHandler.obtainMessage(BluetoothClass.MESSAGE_ERROR_CONNECTING);
				mHandler.sendMessage(msg);
			}
		}
	};
	
	/**
	 * Connect to the input bluetooth device.  This will cancel any
	 * open connection or connection attempts currently running.
	 * 
	 * @param device - The device to connect with.
	 */
	protected void connect(BluetoothDevice device)
	{
		cancel();
		
		mConnectThread = new ConnectThread(device);
		mConnectThread.start();
	}
	
	/**
	 * Connect to the device with the input mac address.
	 * 
	 * @param macAddress - The address of the device to connect to.
	 */
	public void connect(String macAddress)
	{
		connect(mBluetoothAdapter.getRemoteDevice(macAddress));
	}
	
	
	/**
	 * This thread attempts to connect to the input device.  It
	 * will run until a connection succeeds or the thread is cancelled.
	 */
	private class ConnectThread extends Thread{
		private final BluetoothSocket mSocket;
		private final BluetoothDevice mDevice;
		
		public ConnectThread(BluetoothDevice device) 
		{
			mDevice = device;
			BluetoothSocket tmp = null;
			
			//Create a socket to the device
			try
			{
				tmp = device.createInsecureRfcommSocketToServiceRecord(SerialPortServiceClass_UUID);
			}
			catch(IOException e)
			{
				//TODO: Give an error to the main activity
			}
			
			mSocket = tmp;
			
		}
		
		/**
		 * Run method.  This tries to connect the socket until success or
		 * is cancelled.
		 */
		public void run()
		{
			//Make sure discovery is cancelled, if not it can slow a connection
			mBluetoothAdapter.cancelDiscovery();
			
			while(!mSocket.isConnected())
			{
				// Make a connection to the BluetoothSocket
	            try 
	            {
	                // This is a blocking call and will only return on a
	                // successful connection or an exception
	                mSocket.connect();
	            } 
	            catch (IOException e) 
	            {
	            	// Close the socket
	                try
	                {
	                    mSocket.close();
	                }
	                catch (IOException e2)
	                {  }	
	            }
			}
			connected(mSocket, mDevice);
		}
		
		public void cancel() {
			try 
			{
                mSocket.close();
            } 
			catch (IOException e) 
			{ }
			
		}
		
	}

	//-------------------------------------------------------------------------------------
	
	//-----------------------------------Manage a Connection-------------------------------
	/**
    * Start the ConnectedThread to begin managing a Bluetooth connection
    * @param socket  The BluetoothSocket on which the connection was made
    * @param device  The BluetoothDevice that has been connected
    */
   public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) 
	{
	   	//Cancel the thread that completed the connection
	   if(mConnectThread != null)
	   {
		   //Do not call cancel because it closes the socket.
		   //mConnectThread.cancel();
		   mConnectThread = null;
	   }
	   
	   //Cancel any thread currently running a connection
	   if(mConnectedThread != null)
	   {
		   mConnectedThread.cancel();
	   }
	   mConnectedThread = null;
	   
	   //Start the thread to manage the connection
	   mConnectedThread = new ConnectedThread(socket);
	   mConnectedThread.start();
	   
	   //Let the UI know we are connected
	   Message msg = mHandler.obtainMessage(BluetoothClass.MESSAGE_CONNECTION_COMPLETE);
	   mHandler.sendMessage(msg);
	   
	   //Send the name of the connected device
	   Message deviceNameMsg = mHandler.obtainMessage(BluetoothClass.MESSAGE_DEVICE_NAME);
	   Bundle deviceNameBundle = new Bundle();
	   deviceNameBundle.putCharSequence(BluetoothClass.BUNDLE_DEVICE_NAME, device.getName());
	   deviceNameMsg.setData(deviceNameBundle);
	   mHandler.sendMessage(deviceNameMsg);
	}
	
   /**
    * This thread runs during a connection with a remote device.
    * It handles all incoming and outgoing transmissions.
    */
   private class ConnectedThread extends Thread
   {
	   private final BluetoothSocket mSocket;
	   private final InputStream mInStream;
	   private final OutputStream mOutStream;
	   
	   public ConnectedThread(BluetoothSocket socket)
	   {
		   mSocket = socket;
		   InputStream tmpIn = null;
		   OutputStream tmpOut = null;
		   
		   //Get the BluetoothSocket input and output streams
		   try
		   {
			   tmpIn = socket.getInputStream();
			   tmpOut = socket.getOutputStream();
		   }
		   catch(IOException e)
		   {}
		   
		   mInStream = tmpIn;
		   mOutStream = tmpOut;
	   }
	   
	   public void run()
	   {
		   byte[] buffer = new byte[1024];
		   int bytes;
		   
		   //Keep listening while connected
		   while(true)
		   {
			   try
			   {
				   bytes = mInStream.read(buffer);
			   }
			   catch(IOException e)
			   {
				   //Notify UI, cancel thread
				   connectionLost();
			   }
		   }
	   }
	   
	   /**
	    * Write to the connected OutStream
	    * @param buffer - The bytes to write
	    */
	   public void write(char buffer)
	   {
		   try
		   {
			   mOutStream.write(buffer);
		   }
		   catch(IOException e)
		   {  }
	   }
	   
	   public void cancel()
	   {
		   try
		   {
			   mSocket.close();
		   }
		   catch(IOException e)
		   {  }
	   }
   }
   
   /**
    * Write to the bluetooth device non-synchronized
    * @param out - Bytes to write
    */
   public void write(char out)
   {
	   mConnectedThread.write(out);
   }
   
   /**
    * Connection lost.  Sends a message to the UI and closes the running threads.
    */
   private void connectionLost()
   {
	   cancel();
	   Message msg = mHandler.obtainMessage(BluetoothClass.MESSAGE_CONNECTION_LOST);
	   mHandler.sendMessage(msg);
   }
   
   /**
    * Is the device currently running a connection.
    * @return - True if the there is a connection running.
    */
   public boolean isConnected()
   {
	   if(mConnectedThread != null)
		   return true;
	   return false;
   }
   //-------------------------------------------------------------------------------------
}
