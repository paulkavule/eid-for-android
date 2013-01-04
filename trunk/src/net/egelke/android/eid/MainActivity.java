package net.egelke.android.eid;

import java.io.IOException;
import java.lang.ref.WeakReference;

import net.egelke.android.eid.model.Address;
import net.egelke.android.eid.model.Identity;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
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
	
	Identity id;
	
	Address address;
	
	Drawable photo;

	private BroadcastReceiver usbBcReceiver;

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
				Log.e("net.egelke.android.eid", "Reading the identify file failed", e);
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
				Log.e("net.egelke.android.eid", "Reading the identify file failed", e);
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
				Log.e("net.egelke.android.eid", "Reading the photo file failed", e);
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

		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build());
		StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build());

		ActionBar bar = getActionBar();
		bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		bar.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE);

		bar.addTab(bar.newTab().setText(R.string.identity).setTabListener(new TabListener(this, "identity", IdentityFragment.class)));
		bar.addTab(bar.newTab().setText(R.string.certificates).setTabListener(new TabListener(this, "certificate", CertificateFragment.class)));

		Intent intent = getIntent();

		handler = new EidHandler(this);
		usbDevice = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
		if (usbDevice != null) {
			usbBcReceiver = new BroadcastReceiver() {
				public void onReceive(Context context, Intent intent) {
					String action = intent.getAction();

					if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
						Toast.makeText(MainActivity.this.getApplicationContext(), "eID reader removed", Toast.LENGTH_SHORT).show();
						//TODO:check if same device
						 UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
						if (reader != null && device != null && device.getDeviceId() == reader.getDeviceId()) {
							try {
								reader.close();
							} catch (IOException e) {
								Log.w("net.egelke.android.eid", "Failed to close the reader", e);
							}
						}
					}
				}
			};
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		if (usbDevice != null) {
			reader = new EidReader(this, usbDevice);
			reader.setStateNotifier(handler);
		}
	}
	
	@Override
	protected void onPause() {
		try {
			if (reader != null) {
				reader.close();
				reader = null;
			}
		} catch (IOException e) {
			Log.w("net.egelke.android.eid", "could not close the eID reader", e);
		}
		
		super.onPause();
	}

}
