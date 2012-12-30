package net.egelke.android.eid;

import android.app.Activity;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.os.Bundle;

public class UsbNotifyActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Intent intent = getIntent();

		Intent newIntent = new Intent(this, MainActivity.class);
		newIntent.putExtra(UsbManager.EXTRA_DEVICE,  intent.getParcelableExtra(UsbManager.EXTRA_DEVICE));
		startActivity(newIntent);
	}
}
