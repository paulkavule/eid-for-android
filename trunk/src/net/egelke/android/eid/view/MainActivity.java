package net.egelke.android.eid.view;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.egelke.android.eid.EidReader;
import net.egelke.android.eid.model.Address;
import net.egelke.android.eid.model.Identity;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.acs.smartcard.Reader;

public class MainActivity extends Activity {
	
	private static final String ACTION_USB_PERMISSION = "net.egelke.android.eid.USB_PERMISSION";

	private static class EidHandler extends Handler {
		private final WeakReference<MainActivity> selfRef;

		EidHandler(MainActivity self) {
			this.selfRef = new WeakReference<MainActivity>(self);
		}

		@Override
		public void handleMessage(Message msg) {
			MainActivity self = selfRef.get();
			if (self != null) {
				self.handleMessage(msg);
			} else {
				Log.w("net.egelke.android.eid", "Not reference to the Main Activity");
			}
		}
	}
	
	private UsbDeviceIntentReceiver observer;

	private EidHandler handler;

	UsbDevice usbDevice;

	private EidReader reader;
	
	private MenuItem usbMenuItem;
	
	private boolean userConnected;
	
	Identity id;
	
	Address address;
	
	byte[] photo;
	
	List<X509Certificate> certs;

	public void handleMessage(Message msg) {
		switch (msg.what) {
		case Reader.CARD_PRESENT:
			Toast.makeText(this.getApplicationContext(), "eID card inserted", Toast.LENGTH_SHORT).show();
			//TODO:rework to cursor loader that used content provider of eID (http://developer.android.com/guide/components/loaders.html)
			new ReadIdentity().execute(msg.arg1);
			new ReadAddress().execute(msg.arg1);
			new ReadPhoto().execute(msg.arg1);
			new ReadCerts().execute(msg.arg1);
			break;
		case Reader.CARD_ABSENT:
			Toast.makeText(this.getApplicationContext(), "eID card removed", Toast.LENGTH_SHORT).show();
			break;
		default:
			Toast.makeText(this.getApplicationContext(), "Unknown status change", Toast.LENGTH_SHORT).show();
			break;
		}
	}
	
	private static class State {
		Identity id;
		
		Address address;
		
		byte[] photo;
		
		List<X509Certificate> certs;
		
	}
	
	private class UsbDeviceIntentReceiver extends BroadcastReceiver {

	    public UsbDeviceIntentReceiver() {	    	
	        IntentFilter usbFilter = new IntentFilter();
	        usbFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
	        usbFilter.addAction(ACTION_USB_PERMISSION);
	        registerReceiver(this, usbFilter);
	    }

	    @Override public void onReceive(Context context, Intent intent) {
	    	if (ACTION_USB_PERMISSION.equals(intent.getAction()) && intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
	    		usbDevice = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
	    		if (usbMenuItem != null) {
					usbMenuItem.setIcon(R.drawable.ic_usb_attached);
				}
	    		connect();
	    	}
	    	else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(intent.getAction())) {
	    		UsbDevice usbDeviceReceived = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
	    		if (usbDevice != null && usbDeviceReceived != null && usbDevice.getDeviceId() == usbDeviceReceived.getDeviceId()) {
	    			detach();
	    		}
	    	}
	    }
	}
	
	private class ReadIdentity extends AsyncTask<Integer, Void, Identity> {

		@Override
		protected Identity doInBackground(Integer... params) {
			try {
				return reader.readFileIdentity(params[0]);
			} catch (Exception e) {
				Log.w("net.egelke.android.eid", "Reading the identify file failed", e);
				return null;
			}
		}

		@Override
		protected void onPostExecute(Identity result) {
			id = result;
			
			IdentityFragment idFrag = (IdentityFragment) getFragmentManager().findFragmentByTag("identity");
			if (idFrag != null && !idFrag.isDetached()) {
				idFrag.updateId();
			}
			CardFragment cardFrag = (CardFragment) getFragmentManager().findFragmentByTag("card");
			if (cardFrag != null && !cardFrag.isDetached()) {
				cardFrag.updateId();
			}
		}
	}

	private class ReadAddress extends AsyncTask<Integer, Void, Address> {

		@Override
		protected Address doInBackground(Integer... params) {
			try {
				return reader.readFileAddress(params[0]);
			} catch (Exception e) {
				Log.w("net.egelke.android.eid", "Reading the identify file failed", e);
				return null;
			}
		}

		@Override
		protected void onPostExecute(Address result) {
			address = result;
			
			IdentityFragment idFrag = (IdentityFragment) getFragmentManager().findFragmentByTag("identity");
			if (idFrag != null && !idFrag.isDetached()) {
				idFrag.updateAddress();
			}
		}
	}

	private class ReadPhoto extends AsyncTask<Integer, Void, byte[]> {

		@Override
		protected byte[] doInBackground(Integer... params) {
			try {
				return reader.readFileRaw(params[0], EidReader.File.PHOTO);
			} catch (Exception e) {
				Log.w("net.egelke.android.eid", "Reading the photo file failed", e);
				return null;
			}
		}

		@Override
		protected void onPostExecute(byte[] result) {
			photo = result;
			
			IdentityFragment idFrag = (IdentityFragment) getFragmentManager().findFragmentByTag("identity");
			if (idFrag != null && !idFrag.isDetached()) {
				idFrag.updatePhoto();
			}
		}
	}
	
	private class ReadCerts extends AsyncTask<Integer, X509Certificate, Void> {
		
		@Override
		protected Void doInBackground(Integer... params) {
			try {
				for(X509Certificate cert : reader.readFileCerts(params[0])) {
					this.publishProgress(cert); //we display it immediately
				}
				return null;
			} catch (Exception e) {
				Log.w("net.egelke.android.eid", "Reading the photo file failed", e);
				return null;
			}
		}
		
		@Override
		protected void onPreExecute() {
			certs = new LinkedList<X509Certificate>();
			CertificateFragment certFrag = (CertificateFragment) getFragmentManager().findFragmentByTag("certificate");
			if (certFrag != null ) {
				certFrag.clearCertificates();
			}
		}
		
		@Override
		protected void onProgressUpdate(X509Certificate... values) {
			certs.add(values[0]); //we keep it as "cache"
			CertificateFragment certFrag = (CertificateFragment) getFragmentManager().findFragmentByTag("certificate");
			if (certFrag != null ) {
				certFrag.addCertificates(Arrays.asList(values));
			}
		}
	}

	public static class TabListener implements ActionBar.TabListener {
		private final Activity mActivity;
        private final String mTag;
        private final Class<? extends Fragment> mClass;
        private Fragment mFragment;


        public TabListener(Activity activity, String tag, Class<? extends Fragment> clz) {
            mActivity = activity;
            mTag = tag;
            mClass = clz;

            // Check to see if we already have a fragment for this tab, probably
            // from a previously saved state.  If so, deactivate it, because our
            // initial state is that a tab isn't shown.
            mFragment = mActivity.getFragmentManager().findFragmentByTag(mTag);
            if (mFragment != null && !mFragment.isDetached()) {
                FragmentTransaction ft = mActivity.getFragmentManager().beginTransaction();
                ft.detach(mFragment);
                ft.commit();
            }
        }

        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            if (mFragment == null) {
                mFragment = Fragment.instantiate(mActivity, mClass.getName(), null);
                ft.add(android.R.id.content, mFragment, mTag);
            } else {
                ft.attach(mFragment);
            }
        }

        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
            if (mFragment != null) {
                ft.detach(mFragment);
            }
        }

        public void onTabReselected(Tab tab, FragmentTransaction ft) {

        }
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Intent intent = getIntent();
		Log.d("net.egelke.android.eid", "creating a new main activity:" + this.hashCode() + ", with intend " + intent.getAction());

		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build());
		StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build());
		
		//create the observer
		if (observer == null) {
			observer = new UsbDeviceIntentReceiver();
		}

		//Create the action bar
		ActionBar bar = getActionBar();
		bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		bar.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE);

		bar.addTab(bar.newTab().setText(R.string.identity).setTabListener(new TabListener(this, "identity", IdentityFragment.class)));
		bar.addTab(bar.newTab().setText(R.string.card).setTabListener(new TabListener(this, "card", CardFragment.class)));
		bar.addTab(bar.newTab().setText(R.string.certificates).setTabListener(new TabListener(this, "certificate", CertificateFragment.class)));
		
		//In case of a config change, restore the state
		State state = (State) getLastNonConfigurationInstance();
	    if (state != null) {
	        this.id = state.id;
	        this.address = state.address;
	        this.photo = state.photo;
	        this.certs = state.certs;
	    }
		
	    //Restore the state in other cases
		if (savedInstanceState != null) {
            bar.setSelectedNavigationItem(savedInstanceState.getInt("tab", 0));
            userConnected = savedInstanceState.getBoolean("connected", true);
        } else {
        	userConnected = true;
        }
		
		handler = new EidHandler(this);
		if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(intent.getAction())) {
			usbDevice = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		
		Log.d("net.egelke.android.eid", "creating optoins menu:" + this.hashCode());
		this.usbMenuItem = menu.findItem(R.id.menu_usb);
		
		if (usbMenuItem != null) {
			if (this.reader != null) {
				usbMenuItem.setIcon(R.drawable.ic_usb_connected);
			} else if (this.usbDevice != null) {
				usbMenuItem.setIcon(R.drawable.ic_usb_attached);
			} else {
				usbMenuItem.setIcon(R.drawable.ic_usb_detached);
			}
		}
		
		return true;
	}
	
	@Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("tab", getActionBar().getSelectedNavigationIndex());
        outState.putBoolean("connected", userConnected);
    }
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		State state = new State();
		state.id = this.id;
		state.address = this.address;
		state.photo = this.photo;
		state.certs = this.certs;
		
		return state;
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		Log.d("net.egelke.android.eid", "resuming main activity:" + this.hashCode());
		if (userConnected) {
			connect();
		}
	}
	
	@Override
	protected void onPause() {
		Log.d("net.egelke.android.eid", "pausing main activity:" + this.hashCode());
		
		diconnect();	
		super.onPause();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		if (observer != null) {
			this.unregisterReceiver(observer);
			observer = null;
		}
	}
	
	void attach() {
		if (usbMenuItem != null) {
			usbMenuItem.setIcon(R.drawable.ic_usb_detached);
		}
		UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
		
		boolean found = false;
		Map<String, UsbDevice> deviceList = manager.getDeviceList();
		Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
		while(deviceIterator.hasNext()){
		    UsbDevice device = deviceIterator.next();
		    if (device.getVendorId() == 1839) {
		    	//we request permission for all, see what it gets ;-)
		    	manager.requestPermission(device, PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0));
		    	found = true;
		    }
		}
		
		if (!found)
			Toast.makeText(this.getApplicationContext(), "No compatible reader found", Toast.LENGTH_SHORT).show();
	}
	
	void detach() {
		diconnect();
		usbDevice = null;
		if (usbMenuItem != null) {
			usbMenuItem.setIcon(R.drawable.ic_usb_detached);
		}
		
	}
	
	void connect() {
		if (usbDevice != null) {
			if (usbMenuItem != null) {
				usbMenuItem.setIcon(R.drawable.ic_usb_attached);
			}
			Log.d("net.egelke.android.eid", "USB device (still present)");
			try {
				reader = new EidReader(this, usbDevice);
				reader.setStateNotifier(handler);
				if (usbMenuItem != null) {
					usbMenuItem.setIcon(R.drawable.ic_usb_connected);
				}
			} catch (Exception e) {
				Log.w("net.egelke.android.eid", "Could not user USB device as eID reader", e);
				Toast.makeText(this.getApplicationContext(), "Could not connect to reader", Toast.LENGTH_SHORT).show();
				usbDevice = null;
				if (usbMenuItem != null) {
					usbMenuItem.setIcon(R.drawable.ic_usb_detached);
				}
			}
		} else {
			if (usbMenuItem != null) {
				usbMenuItem.setIcon(R.drawable.ic_usb_detached);
			}
		}
	}
	
	void diconnect() {
		try {
			if (reader != null) {
				reader.close();
				reader = null;
			}
			if (usbMenuItem != null) {
				usbMenuItem.setIcon(R.drawable.ic_usb_attached);
			}
		} catch (IOException e) {
			Log.w("net.egelke.android.eid", "could not close the eID reader", e);
			usbDevice = null;
			if (usbMenuItem != null) {
				usbMenuItem.setIcon(R.drawable.ic_usb_detached);
			}
		}
	}

	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_usb:
			Log.d("net.egelke.android.eid", "toggle USB");
			if (reader != null) {
				userConnected = false;
				diconnect();
			} else if (usbDevice != null) {
				userConnected = true;
				connect();
			} else {
				attach();
			}
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}
