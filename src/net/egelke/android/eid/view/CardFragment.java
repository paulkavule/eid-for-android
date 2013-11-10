package net.egelke.android.eid.view;

import java.text.DateFormat;

import net.egelke.android.eid.model.Identity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class CardFragment extends Fragment {
	
	private Identity id;
	
	private TextView cardNr;
	
	private TextView issuePlace;
	
	private TextView chipNr;
	
	private TextView validFrom;
	
	private TextView validTo;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		this.setRetainInstance(true);
		
		View v = inflater.inflate(R.layout.card, container, false);
		
		cardNr = (TextView) v.findViewById(R.id.cardnr);
		issuePlace = (TextView) v.findViewById(R.id.issuePlace);
		chipNr = (TextView) v.findViewById(R.id.chipnr);
		validFrom = (TextView) v.findViewById(R.id.validFrom);
		validTo = (TextView) v.findViewById(R.id.validTo);
		
		return v;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		update();
	}
	
	public void setId(Identity id) {
		this.id = id;
		if (!isDetached()) update();
	}
	
	private void update() {
		if (this.id != null) {
			DateFormat df = DateFormat.getDateInstance();
			
			cardNr.setText(id.getCardNumber());
			issuePlace.setText(id.getCardDeliveryMunicipality());
			chipNr.setText(id.getChipNumber());
			validFrom.setText(df.format(id.getCardValidity().getBegin().getTime()));
			validTo.setText(df.format(id.getCardValidity().getEnd().getTime()));
		} else {
			cardNr.setText("");
			issuePlace.setText("");
			chipNr.setText("");
			validFrom.setText("");
			validTo.setText("");
		}
	}

}
