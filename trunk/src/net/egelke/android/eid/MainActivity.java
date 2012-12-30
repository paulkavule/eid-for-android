package net.egelke.android.eid;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.DateFormat;

import javax.xml.datatype.DatatypeConstants;

import net.egelke.android.eid.model.Address;
import net.egelke.android.eid.model.Identity;
import net.egelke.android.eid.model.SpecialStatus;
import android.app.Activity;
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
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
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
			if (result == null)
				return;

			switch (result.getDocumentType()) {
			case BELGIAN_CITIZEN:
				type.setText(R.string.cardtype_citizen);
				break;
			case KIDS_CARD:
				type.setText(R.string.cardtype_kids);
				break;
			default:
				type.setText(result.getDocumentType().name().replace('_', ' '));
				break;
			}
			name.setText(result.getFamilyName());
			gNames.setText(result.getFirstName() + " " + result.getMiddleNames());
			birthPlace.setText(result.getPlaceOfbirth());
			if (result.getDateOfBirth().getMonth() != DatatypeConstants.FIELD_UNDEFINED) {
				 DateFormat df = DateFormat.getDateInstance();
				birthDate.setText(df.format(result.getDateOfBirth().toGregorianCalendar().getTime()));
			} else {
				birthDate.setText(Integer.toString(result.getDateOfBirth().getYear()));
			}
			switch (result.getGender()) {
			case MALE:
				sex.setText(R.string.sex_male);
				break;
			case FEMALE:
				sex.setText(R.string.sex_female);
				break;
			}
			natNumber.setText(result.getNationalNumber());
			nationality.setText(result.getNationality());
			title.setText(result.getNobleTitle());
			for (SpecialStatus status : result.getSpecialStatus()) {
				switch (status) {
				case WHITE_CANE:
					status_whiteCane.setChecked(true);
					break;
				case YELLOW_CANE:
					status_yellowCane.setChecked(true);
					break;
				case EXTENDED_MINORITY:
					status_extMinority.setChecked(true);
					break;
				}
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
			street.setText(result.getStreetAndNumber());
			zip.setText(result.getZip());
			municipality.setText(result.getMunicipality());
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
			if (result == null)
				return;

			photo.setImageDrawable(result);
		}
	}

	private UsbDevice usbDevice;

	private EidReader reader;

	private EidHandler handler;
	
	private TextView type;

	private TextView name;

	private TextView gNames;

	private TextView birthPlace;

	private TextView birthDate;

	private TextView sex;

	private TextView natNumber;

	private TextView nationality;

	private TextView title;

	private CheckBox status_whiteCane;

	private CheckBox status_yellowCane;

	private CheckBox status_extMinority;

	private TextView street;

	private TextView zip;

	private TextView municipality;

	private ImageView photo;

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

	@Override
	public void onCreate(Bundle savedInstanceState) {
		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build());
		StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build());

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Intent intent = getIntent();

		handler = new EidHandler(this);
		usbDevice = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
		type = (TextView) this.findViewById(R.id.idType);
		name = (TextView) this.findViewById(R.id.name);
		gNames = (TextView) this.findViewById(R.id.gNames);
		birthPlace = (TextView) this.findViewById(R.id.birthPlace);
		birthDate = (TextView) this.findViewById(R.id.birthDate);
		sex = (TextView) this.findViewById(R.id.sex);
		natNumber = (TextView) this.findViewById(R.id.natNumber);
		nationality = (TextView) this.findViewById(R.id.nationality);
		title = (TextView) this.findViewById(R.id.title);
		status_whiteCane = (CheckBox) this.findViewById(R.id.status_whiteCane);
		status_yellowCane = (CheckBox) this.findViewById(R.id.status_yellowCane);
		status_extMinority = (CheckBox) this.findViewById(R.id.status_extMinority);
		street = (TextView) this.findViewById(R.id.street);
		zip = (TextView) this.findViewById(R.id.zip);
		municipality = (TextView) this.findViewById(R.id.municipality);
		photo = (ImageView) this.findViewById(R.id.photo);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	protected void onStart() {
		super.onStart();

		if (usbDevice != null) {
			reader = new EidReader(this, usbDevice);
			reader.setStateNotifier(handler);
		}
	}

	@Override
	protected void onStop() {
		try {
			if (reader != null) {
				reader.close();
				reader = null;
			}
		} catch (IOException e) {
			Log.w("net.egelke.android.eid", "could not close the eID reader", e);
		}
		super.onStop();
	}

}
