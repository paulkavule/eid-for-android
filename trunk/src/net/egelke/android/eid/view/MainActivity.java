package net.egelke.android.eid.view;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.Map;

import net.egelke.android.eid.EidReader;
import net.egelke.android.eid.model.Address;
import net.egelke.android.eid.model.Identity;
import net.egelke.android.eid.model.ObjectFactory;
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
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.drawable.Drawable;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class MainActivity extends Activity
	implements OnSharedPreferenceChangeListener {
	
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

	public void handleMessage(Message msg) {
		switch (msg.what) {
		case EidReader.MSG_CARD_INSERTED:
			Toast.makeText(this.getApplicationContext(), "eID card inserted", Toast.LENGTH_SHORT).show();
			new ReadEid(this).execute(msg.arg1);
			break;
		case EidReader.MSG_CARD_REMOVED:
			Toast.makeText(this.getApplicationContext(), "eID card removed", Toast.LENGTH_SHORT).show();
			break;
		default:
			Toast.makeText(this.getApplicationContext(), "Unknown status change", Toast.LENGTH_SHORT).show();
			break;
		}
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
	
	private class ReadEid extends AsyncTask<Integer, Object, Void> {
		
		private final Context context;
		private boolean demo;
		
		
		public ReadEid(Context context)
		{
			this.context = context;
		}
		
		@Override
		protected void onPreExecute() {
			SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
			demo = sharedPref.getBoolean("pref_demo", false);
			
			
			CertificateFragment certFrag = (CertificateFragment) getFragmentManager().findFragmentByTag("certificate");
			if (certFrag != null ) {
				certFrag.clearCertificates();
			}
		}
		
		@Override
		protected Void doInBackground(Integer... params) {
			try {
				if (demo) {
					Log.w("net.egelke.android.eid", "Getting the demo eID info");
					ObjectFactory factory = new ObjectFactory();
					
					Map<Integer, byte[]> identifyMap = factory.createTvMap(getAssets().open("Alice_Identity.tlv"));
					this.publishProgress(factory.createIdentity(identifyMap));
					
					Map<Integer, byte[]> addressMap = factory.createTvMap(getAssets().open("Alice_Address.tlv"));
					this.publishProgress(factory.createAddress(addressMap));
					
					InputStream photoStream = getAssets().open("Alice_Photo.jpg");
					this.publishProgress(Drawable.createFromStream(photoStream, "idPic"));
					
					InputStream certStream;
					CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
					
					certStream = getAssets().open("Alice_Root.crt");
					this.publishProgress(certFactory.generateCertificate(certStream));
					
					certStream = getAssets().open("Alice_RRN.crt");
					this.publishProgress(certFactory.generateCertificate(certStream));
					
					certStream = getAssets().open("Alice_CA.crt");
					this.publishProgress(certFactory.generateCertificate(certStream));
					
					certStream = getAssets().open("Alice_Authentification.crt");
					this.publishProgress(certFactory.generateCertificate(certStream));
					
					certStream = getAssets().open("Alice_Signing.crt");
					this.publishProgress(certFactory.generateCertificate(certStream));
				}
				else 
				{
					Log.i("net.egelke.android.eid", "Getting the eID info");
					this.publishProgress(reader.readFileIdentity(params[0]));
					this.publishProgress(reader.readFileAddress(params[0]));
					this.publishProgress(reader.readFilePhoto(params[0]));
					for(X509Certificate cert : reader.readFileCerts(params[0])) {
						this.publishProgress(cert); //we display it immediately
					}
				}
				return null;
			} catch (Exception e) {
				Log.e("net.egelke.android.eid", "Reading the identify file failed", e);
				return null;
			}
		}
		

		
		@Override
		protected void onProgressUpdate(Object... values) {
			Log.d("net.egelke.android.eid", String.format("Displaying %s", values[0].getClass().toString()));
			if (values[0] instanceof Identity) {
				PersonFragment personFrag = (PersonFragment) getFragmentManager().findFragmentByTag("person");
				if (personFrag != null) {
					personFrag.setId((Identity) values[0]);
				}
				CardFragment cardFrag = (CardFragment) getFragmentManager().findFragmentByTag("card");
				if (cardFrag != null) {
					cardFrag.setId((Identity) values[0]);
				}
			} 
			else if (values[0] instanceof Address) {
				AddressFragment addressFrag = (AddressFragment) getFragmentManager().findFragmentByTag("address");
				if (addressFrag != null) {
					addressFrag.setAddress((Address) values[0]);
				}
			}
			else if (values[0] instanceof Drawable) {
				PhotoFragment photoFragment = (PhotoFragment) getFragmentManager().findFragmentByTag("photo");
				if (photoFragment != null) {
					photoFragment.setPhoto((Drawable) values[0]);
				}
			}
			else if (values[0] instanceof X509Certificate)
			{
				CertificateFragment certFrag = (CertificateFragment) getFragmentManager().findFragmentByTag("certificate");
				if (certFrag != null) {
					certFrag.addCertificate((X509Certificate) values[0]);
				}
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
            
            //eager load the tab, detached.
            FragmentTransaction ft = mActivity.getFragmentManager().beginTransaction();
            mFragment = mActivity.getFragmentManager().findFragmentByTag(mTag);
            if (mFragment == null) {
            	mFragment = Fragment.instantiate(mActivity, mClass.getName(), null);
                ft.add(android.R.id.content, mFragment, mTag);
            }
            if (!mFragment.isDetached()) ft.detach(mFragment);
            ft.commit();
        }

        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            ft.attach(mFragment);
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

		bar.addTab(bar.newTab().setText(R.string.person).setTabListener(new TabListener(this, "person", PersonFragment.class)));
		bar.addTab(bar.newTab().setText(R.string.photo).setTabListener(new TabListener(this, "photo", PhotoFragment.class)));
		bar.addTab(bar.newTab().setText(R.string.address).setTabListener(new TabListener(this, "address", AddressFragment.class)));
		bar.addTab(bar.newTab().setText(R.string.card).setTabListener(new TabListener(this, "card", CardFragment.class)));
		bar.addTab(bar.newTab().setText(R.string.certificates).setTabListener(new TabListener(this, "certificate", CertificateFragment.class)));
		
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
	protected void onResume() {
		super.onResume();
		Log.d("net.egelke.android.eid", "resuming main activity:" + this.hashCode());
		
		this.getPreferences(MODE_PRIVATE).registerOnSharedPreferenceChangeListener(this);
		
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		if (sharedPref.getBoolean("pref_demo", false)) {
			new ReadEid(this).execute(-1);
		} else {
			if (userConnected) {
				connect();
			}
		}
	}
	
	@Override
	protected void onPause() {
		Log.d("net.egelke.android.eid", "pausing main activity:" + this.hashCode());
		this.getPreferences(MODE_PRIVATE).unregisterOnSharedPreferenceChangeListener(this);
		
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
		    if (device.getDeviceClass() == UsbConstants.USB_CLASS_CSCID) {
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
				reader.open();
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
				if (usbDevice != null) {
					usbMenuItem.setIcon(R.drawable.ic_usb_attached);
				} else {
					usbMenuItem.setIcon(R.drawable.ic_usb_detached);
				}
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
		case R.id.menu_settings:
			Log.d("net.egelke.android.eid", "show menu");
			Intent intent = new Intent();
			intent.setClass(this, SettingsActivity.class);
			startActivity(intent);
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if (key.equals("pref_demo")) {
			new ReadEid(this).execute(-1);
		}
	}
}
