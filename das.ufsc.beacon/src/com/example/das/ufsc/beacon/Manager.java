package com.example.das.ufsc.beacon;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;


public class Manager 
{
	private BluetoothAdapter btAdapter;
	private CommunicationService comunicationService;
	private Main ui;
	
	private final Handler mHandler = new Handler() 
	{
		@Override
		public void handleMessage(Message msg) 
		{
			switch(msg.what)
			{
			case CommunicationService.MSG_TYPE_MESSAGE_READ:
			{
				byte[] readBuf = (byte[]) msg.obj;
				// construct a string from the valid bytes in the buffer
				String readMessage = new String(readBuf, 0, msg.arg1);
				
				readAck(readMessage);
				//readSetSlave(readMessage);
				break;
			}
			case CommunicationService.MSG_TYPE_STOP_DISCOVERY:
			{
				if (btAdapter.isDiscovering()) 
				{
		            btAdapter.cancelDiscovery();
		        }
				ui.showToast("Conected as slave, stopping discovery...");
				break;
			}
			case CommunicationService.MSG_TYPE_CONNECTION_ACCEPTED:
			{
				//obtem o mac
				String mac = (String)msg.obj;
				
				sendTic(mac);
				
				
				break;
			}

			}
		}

	};
	private ScheduledExecutorService executor;
	

	private void sendTic(String mac) 
	{
		ui.showToast("conexao aceita de " + mac);
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd HH:mm:ss.SSS");
		ui.addHistoricEntry("[" + mac + "] in at " + dateFormat.format(new Date()));

		try 
		{
			int secs = new Random().nextInt(5) + 8;
			String msg = "tic:" + secs;
			this.comunicationService.sendMessage(msg);
		} 
		catch (IOException e) 
		{
			ui.showToast(e.getMessage());
			e.printStackTrace();
		}
		
	}
	
	public Manager(Main uiRef)
	{
		super();
		
		this.ui = uiRef;
		
		//obtem a interface para o hardware bluetooth do dispositivo
		btAdapter = BluetoothAdapter.getDefaultAdapter();
		comunicationService = new CommunicationService(mHandler);
	}


	
	private void sendMAC()
	{
		String mac = btAdapter.getAddress();
		String msg = "remotemac:" + mac;
		//ui.showToast(" antes enviar mac");
		try {
			this.comunicationService.sendMessage(msg);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			ui.showToast(e.getMessage());
			e.printStackTrace();
		}
		//ui.showToast("apos enviar mac");
	}
	
	
	private void readAck(String readMessage) 
	{
		if(readMessage.contains("ack"))
		{
			//TODO criar outro metodo
			comunicationService.stopComunicationThread();
			
			ui.showToast("encerrando conexao com monitor");
		}
	}
	
	
	
	
	
	/*private void startDiscovery() 
	{
		//start discovery
		if(btAdapter.startDiscovery())
		{
			ui.showToast("Executing discovery");
			//ui.prepareDiscoveryResult();
		}
	}*/

	



	public void turnOnBluetooth() 
	{
		//liga bluetooth
		if (btAdapter.isEnabled()) 
		{
			btAdapter.disable(); 
		}
		btAdapter.enable(); 	
	}

	

	public void stopBeacon() 
	{
		//desliga o bluetooth
		btAdapter.disable();
		
		//executor.shutdownNow();
		
		//para o servico de comunicacao
		comunicationService.stop();
	}



	public void onBluetoothOn() 
	{
		//inicia o servico de comunicacao
		comunicationService.start();
		
		Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
		discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
		ui.startActivity(discoverableIntent);
				
		ui.showToast("Discoverable now");
		
		
		//obtem o endereco de hardware do dispositivo
		String address = btAdapter.getAddress();
		String name = btAdapter.getName();
		String statusText = "Device name: " + name + " MAC:" + address;		
		
		ui.showBluetoothProperties(statusText);	
	}	
	
	
}
