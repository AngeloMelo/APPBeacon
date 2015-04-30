package com.example.das.ufsc.beacon;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;


public class CommunicationService
{
	public static final int MSG_TYPE_MESSAGE_READ = 0;
	public static final int MSG_TYPE_REFRESH_SLAVELIST = 1;
	public static final int MSG_TYPE_STOP_DISCOVERY = 2;
	public static final int MSG_TYPE_CONNECTION_ACCEPTED = 3;
	
	public static final String NAME = "beacon";
	public static final UUID MY_UUID = UUID.fromString("061d3751-78df-472c-8957-07df79497e71");

	// Constants that indicate the current connection state
	public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device
	
	private ReadWriteThread mReadWriteThread;
	private AcceptThread mAcceptThread;
	private BluetoothAdapter mAdapter;
	private final Handler mHandler;
    
	
	private Map<String, ReadWriteThread> commThreads = new HashMap(7);


	
	public CommunicationService(Handler handler) 
	{
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mHandler = handler;
    }
	
	
	public synchronized void start() 
	{
        // Cancel any thread currently running a connection
        if (mReadWriteThread != null) {mReadWriteThread.cancel(); mReadWriteThread = null;}

        // Start the thread to listen on a BluetoothServerSocket
        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread(BluetoothAdapter.getDefaultAdapter());
            mAcceptThread.start();
        }
    }
	

	public synchronized void restartAcceptThread()
	{
		// Start the thread to listen on a BluetoothServerSocket
		if (mAcceptThread != null) {mAcceptThread.cancel(); mAcceptThread = null;}
		
        mAcceptThread = new AcceptThread(BluetoothAdapter.getDefaultAdapter());
        mAcceptThread.start();
	}
	
	
	public synchronized void stop()
	{
		if (mAcceptThread != null) {mAcceptThread.cancel(); mAcceptThread = null;}
        if (mReadWriteThread != null) {mReadWriteThread.cancel(); mReadWriteThread = null;}
	}
	
	
	/**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     * @param device  The BluetoothDevice that has been connected
     */
	public synchronized void startTransmission(BluetoothSocket socket) 
    {
        
		BluetoothDevice remoteDevice = socket.getRemoteDevice();
        
		//TODO Colocar thread em uma lista
		// Cancel any thread currently running a connection
        if (mReadWriteThread != null) {mReadWriteThread.cancel(); mReadWriteThread = null;}
        
        // Start the thread to manage the connection and perform transmissions
        mReadWriteThread = new ReadWriteThread(socket);
        mReadWriteThread.start();
        
        mHandler.obtainMessage(MSG_TYPE_CONNECTION_ACCEPTED, remoteDevice.getAddress()).sendToTarget();

        mAcceptThread.cancel(); 
        mAcceptThread = new AcceptThread(BluetoothAdapter.getDefaultAdapter());
        mAcceptThread.start();
    }    
	
	public void stopComunicationThread() 
	{
		mReadWriteThread.cancel(); 
		mReadWriteThread = null;
	}

    

	public synchronized void sendMessage(String msg) throws IOException
    {
    	mReadWriteThread.write(msg.getBytes());
    }
    
	
	public void stopConnectionThreads() 
	{
        // Cancel the accept thread because we only want to connect to one device
        if (mAcceptThread != null) {mAcceptThread.cancel(); mAcceptThread = null;}
	}

	
	
	private class AcceptThread extends Thread
	{
		private final BluetoothServerSocket mmServerSocket;
		
		public AcceptThread(BluetoothAdapter mBluetoothAdapter)
		{
	        // Use a temporary object that is later assigned to mmServerSocket, because mmServerSocket is final
	        BluetoothServerSocket tmp = null;
	        try 
	        {
	            // MY_UUID is the app's UUID string, also used by the client code
	            tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(NAME, MY_UUID);
	        } 
	        catch (IOException e) 
	        {
	        	e.printStackTrace();
	        }
	        mmServerSocket = tmp;
	    }
		
		
		public void run() 
		{
	        BluetoothSocket socket = null;
	        
	        //TODO
            while (true) 
	        {
	            try 
	            {
	                socket = mmServerSocket.accept();
	            } 
	            catch (IOException e) 
	            {
	                break;
	            }
	            // If a connection was accepted
	            if (socket != null) 
	            {
                    synchronized (CommunicationService.this) 
                    {
                    	// Situation normal. Start the connected thread.
                    	startTransmission(socket);
                    	
                    	try 
                    	{
							mmServerSocket.close();
						} 
                    	catch (IOException e) 
                    	{
							e.printStackTrace();
						}
                        break;
                    }
                }
	        }
	    }
		
		
		 /** Will cancel the listening socket, and cause the thread to finish */
	    public void cancel() 
	    {
	        try 
	        {
	            mmServerSocket.close();
	        } 
	        catch (IOException e) 
	        {
	        	
	        }
	    }
	}
	
	
	private class ReadWriteThread extends Thread 
	{
		private final BluetoothSocket mmSocket;
	    private final InputStream mmInStream;
	    private final OutputStream mmOutStream;
	 
	    public ReadWriteThread(BluetoothSocket socket) 
	    {
	        mmSocket = socket;
	        InputStream tmpIn = null;
	        OutputStream tmpOut = null;
	 
	        // Get the input and output streams, using temp objects because
	        // member streams are final
	        try {
	            tmpIn = socket.getInputStream();
	            tmpOut = socket.getOutputStream();
	        } catch (IOException e) { }
	 
	        mmInStream = tmpIn;
	        mmOutStream = tmpOut;
	    }
	 
	    public void run() 
	    {
	    	// buffer store for the stream
	        byte[] buffer = new byte[1024];  

	        // bytes returned from read()
	        int bytes; 
	 
	        // Keep listening to the InputStream until an exception occurs
	        while (true) 
	        {
	            try 
	            {
	                // Read from the InputStream
	                bytes = mmInStream.read(buffer);
	                
	                // Send the obtained bytes to the UI activity
	                mHandler.obtainMessage(MSG_TYPE_MESSAGE_READ, bytes, -1, buffer).sendToTarget();
	            } 
	            catch (IOException e) 
	            {
	                break;
	            }
	        }
	    }
	 
	    /* Call this from the main activity to send data to the remote device */
	    public void write(byte[] bytes) throws IOException 
	    {
            mmOutStream.write(bytes);
	    }
	 
	    /* Call this from the main activity to shutdown the connection */
	    public void cancel() 
	    {
	        try 
	        {
	            mmSocket.close();
	        } 
	        catch (IOException e) { }
	    }
	}
}
