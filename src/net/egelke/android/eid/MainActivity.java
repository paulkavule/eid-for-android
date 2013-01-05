package net.egelke.android.eid;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.security.cert.X509Certificate;
import java.util.List;

import net.egelke.android.eid.model.Address;
import net.egelke.android.eid.model.Identity;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
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

	private EidHandler handler;

	private UsbDevice usbDevice;

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
	
	private class ReadCerts extends AsyncTask<Integer, Void, List<X509Certificate>> {
		
		@Override
		protected List<X509Certificate> doInBackground(Integer... params) {
			try {
				return reader.readFileCerts(params[0]);
			} catch (Exception e) {
				Log.w("net.egelke.android.eid", "Reading the photo file failed", e);
				return null;
			}
		}

		@Override
		protected void onPostExecute(List<X509Certificate> result) {
			certs = result;
			
			CertificateFragment certFrag = (CertificateFragment) getFragmentManager().findFragmentByTag("certificate");
			if (certFrag != null && !certFrag.isDetached()) {
				certFrag.updateCertificates();
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
		
		//In case 
		if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(intent.getAction())) {
			handler = new EidHandler(this);
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
	
	private void attach() {
		Toast.makeText(this.getApplicationContext(), "Sorry, can't select an USB device manually (yet)", Toast.LENGTH_LONG).show();
	}
	
	private void connect() {
		if (usbDevice != null) {
			Log.d("net.egelke.android.eid", "USB device (still present)");
			try {
				reader = new EidReader(this, usbDevice);
				reader.setStateNotifier(handler);
				if (usbMenuItem != null) {
					usbMenuItem.setIcon(R.drawable.ic_usb_connected);
					usbMenuItem.setEnabled(true);
				}
			} catch (Exception e) {
				Log.w("net.egelke.android.eid", "Could not user USB device as eID reader", e);
				if (usbMenuItem != null) {
					usbMenuItem.setIcon(R.drawable.ic_usb_detached);
				}
				usbDevice = null;
			}
		}
	}
	
	private void diconnect() {
		try {
			if (reader != null) {
				reader.close();
				reader = null;
			}
			if (usbMenuItem != null) {
				usbMenuItem.setIcon(R.drawable.ic_usb_attached);
			} else {
				usbMenuItem.setIcon(R.drawable.ic_usb_detached);
			}
		} catch (IOException e) {
			Log.w("net.egelke.android.eid", "could not close the eID reader", e);
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
