package net.egelke.android.eid;

import java.text.DateFormat;

import net.egelke.android.eid.model.Identity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class CardFragment extends Fragment {
	
	private TextView cardNr;
	
	private TextView issuePlace;
	
	private TextView chipNr;
	
	private TextView validFrom;
	
	private TextView validTo;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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
		updateId();
	}
	
	public void updateId() {
		if (((MainActivity) getActivity()).id != null) {
			DateFormat df = DateFormat.getDateInstance();
			Identity result = ((MainActivity) getActivity()).id;
			
			cardNr.setText(result.getCardNumber());
			issuePlace.setText(result.getCardDeliveryMunicipality());
			chipNr.setText(result.getChipNumber());
			validFrom.setText(df.format(result.getCardValidity().getBegin().getTime()));
			validTo.setText(df.format(result.getCardValidity().getEnd().getTime()));
		}
	}

}
