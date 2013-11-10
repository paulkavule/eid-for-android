package net.egelke.android.eid.view;

import net.egelke.android.eid.model.Address;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class AddressFragment extends Fragment {
	
	private Address address;

	private TextView street;

	private TextView zip;

	private TextView municipality;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		this.setRetainInstance(true);
		
		View v = inflater.inflate(R.layout.address, container, false);
		
		street = (TextView) v.findViewById(R.id.street);
		zip = (TextView) v.findViewById(R.id.zip);
		municipality = (TextView) v.findViewById(R.id.municipality);
		
		return v;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		update();
	}
	
	public void setAddress(Address address) {
		this.address = address;
		if (!isDetached()) update();
	}
	
	public void update() {
		if (this.address != null) {
			street.setText(address.getStreetAndNumber());
			zip.setText(address.getZip());
			municipality.setText(address.getMunicipality());
		} else {
			street.setText("");
			zip.setText("");
			municipality.setText("");
		}
	}
	
}
