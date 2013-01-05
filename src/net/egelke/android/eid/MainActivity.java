package net.egelke.android.eid;

import java.io.IOException;
import java.lang.ref.WeakReference;

import net.egelke.android.eid.model.Address;
import net.egelke.android.eid.model.Identity;
import android.R.menu;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.drawable.Drawable;
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
	
	Identity id;
	
	Address address;
	
	Drawable photo;

	public void handleMessage(Message msg) {
		switch (msg.what) {
		case Reader.CARD_PRESENT:
			Toast.makeText(this.getApplicationContext(), "eID card inserted", Toast.LENGTH_SHORT).show();
			new ReadIdentity().execute(msg.arg1);
			new ReadAddress().execute(msg.arg1);
			new ReadPhoto().execute(msg.arg1);
			break;
		case Reader.CARD_ABSENT:
			Toast.makeText(this.getApplicationContext(), "eID card removed", Toast.LENGTH_SHORT).show();
			break;
		default:
			Toast.makeText(this.getApplicationContext(), "Unknown status change", Toast.LENGTH_SHORT).show();
			break;
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

	private class ReadPhoto extends AsyncTask<Integer, Void, Drawable> {

		@Override
		protected Drawable doInBackground(Integer... params) {
			try {
				return reader.readFilePhoto(params[0]);
			} catch (Exception e) {
				Log.w("net.egelke.android.eid", "Reading the photo file failed", e);
				return null;
			}
		}

		@Override
		protected void onPostExecute(Drawable result) {
			photo = result;
			
			IdentityFragment idFrag = (IdentityFragment) getFragmentManager().findFragmentByTag("identity");
			if (idFrag != null && !idFrag.isDetached()) {
				idFrag.updatePhoto();
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

		ActionBar bar = getActionBar();
		bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		bar.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE);

		bar.addTab(bar.newTab().setText(R.string.identity).setTabListener(new TabListener(this, "identity", IdentityFragment.class)));
		bar.addTab(bar.newTab().setText(R.string.certificates).setTabListener(new TabListener(this, "certificate", CertificateFragment.class)));

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
	protected void onStart() {
		super.onStart();
		
		Log.d("net.egelke.android.eid", "staring main activity:" + this.hashCode());
	}
	
	@Override
	protected void onRestart() {
		super.onRestart();
		
		Log.d("net.egelke.android.eid", "restaring main activity:" + this.hashCode());
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		Log.d("net.egelke.android.eid", "resuming main activity:" + this.hashCode());
		connect();
	}
	
	@Override
	protected void onPause() {
		Log.d("net.egelke.android.eid", "pausing main activity:" + this.hashCode());
		
		diconnect();	
		super.onPause();
	}
	
	@Override
	protected void onStop() {
		Log.d("net.egelke.android.eid", "stopping main activity:" + this.hashCode());
		
		super.onStop();
	}
	
	@Override
	protected void onDestroy() {
		Log.d("net.egelke.android.eid", "destroying main activity:" + this.hashCode());
		
		super.onDestroy();
	}
	
	private void attach() {
		
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
				diconnect();
			} else if (usbDevice != null) {
				connect();
			} else {
				Toast.makeText(this.getApplicationContext(), "Sorry, can't select an USB device manually (yet)", Toast.LENGTH_LONG).show();
			}
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}
