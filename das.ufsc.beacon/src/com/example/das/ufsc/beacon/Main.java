package com.example.das.ufsc.beacon;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


public class Main extends Activity 
{
	private Manager manager;
	private Button turnOn;
	private Button disconnect;
	private TextView statusUpdate;
	private TextView msg1;
	private TextView msg2;
	private TextView historic;
	
	private String newMessage;
	private String oldMessage;
	private String historicText = "";
	
	BroadcastReceiver bluetoothState = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent) 
		{
			String stateExtra = BluetoothAdapter.EXTRA_STATE;
			int state = intent.getIntExtra(stateExtra, -1);
			switch(state)
			{
			case(BluetoothAdapter.STATE_TURNING_ON):
			{
				showToast("Bluetooth turning On");
				break;
			}
			case(BluetoothAdapter.STATE_ON):
			{
				showToast("Bluetooth On");
				manager.onBluetoothOn();
				break;
			}
			case(BluetoothAdapter.STATE_TURNING_OFF):
			{
				showToast("Bluetooth turning Off");
				break;
			}
			case(BluetoothAdapter.STATE_OFF):
			{
				showToast("Bluetooth Off");
				hideBluetoothProperties();
				break;
			}
			}
			
		};
	};
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		this.manager = new Manager(this);
		
		setupUI();
	}
	
	
	private void setupUI() 
	{
		statusUpdate = (TextView) findViewById(R.id.result);
		msg1 = (TextView) findViewById(R.id.msg1);
		msg2 = (TextView) findViewById(R.id.msg2);
		historic = (TextView) findViewById(R.id.historic);
		
		turnOn = (Button) findViewById(R.id.turnonBtn);
		disconnect = (Button) findViewById(R.id.disconnectBtn);		
		
		disconnect.setVisibility(View.GONE);
		
		//cria actionListeners para o botao turnOn
		turnOn.setOnClickListener(
			new OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					//registra no monitor de estados a action para ligar o bluetooth
					String actionStateChanged = BluetoothAdapter.ACTION_STATE_CHANGED;
					IntentFilter filter = new IntentFilter(actionStateChanged);
					registerReceiver(bluetoothState, filter);
					
					manager.turnOnBluetooth();
				}
			}
		);


		
		disconnect.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				disconnect.setVisibility(View.GONE);
				turnOn.setVisibility(View.VISIBLE);
				manager.stopBeacon();
			}
		});
	}
	
	
	public void showToast(String msg)
	{
		this.oldMessage = this.newMessage;
		this.newMessage = msg;
		
		msg1.setText(this.newMessage);
		msg2.setText(this.oldMessage);
	}
	
	
	public void addHistoricEntry(String msg)
	{
		this.historicText = this.historicText + "\n" + msg;
		
		historic.setText(this.historicText);
	}

	public void showBluetoothProperties(String statusText)
	{
		statusUpdate.setText(statusText);
		disconnect.setVisibility(View.VISIBLE);
		turnOn.setVisibility(View.GONE);
	}

	
	public void hideBluetoothProperties() 
	{
		statusUpdate.setText("Bluetooth is not on");
		disconnect.setVisibility(View.GONE);
		turnOn.setVisibility(View.VISIBLE);
	}
	

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
