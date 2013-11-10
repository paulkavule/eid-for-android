package net.egelke.android.eid.view;

import java.text.DateFormat;

import javax.xml.datatype.DatatypeConstants;

import net.egelke.android.eid.model.Identity;
import net.egelke.android.eid.model.SpecialStatus;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

public class PersonFragment extends Fragment {
	
	private Identity id;
	
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
	
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		this.setRetainInstance(true);
		
		View v = inflater.inflate(R.layout.person, container, false);
		
		type = (TextView) v.findViewById(R.id.idType);
		name = (TextView) v.findViewById(R.id.name);
		gNames = (TextView) v.findViewById(R.id.gNames);
		birthPlace = (TextView) v.findViewById(R.id.birthPlace);
		birthDate = (TextView) v.findViewById(R.id.birthDate);
		sex = (TextView) v.findViewById(R.id.sex);
		natNumber = (TextView) v.findViewById(R.id.natNumber);
		nationality = (TextView) v.findViewById(R.id.nationality);
		title = (TextView) v.findViewById(R.id.title);
		status_whiteCane = (CheckBox) v.findViewById(R.id.status_whiteCane);
		status_yellowCane = (CheckBox) v.findViewById(R.id.status_yellowCane);
		status_extMinority = (CheckBox) v.findViewById(R.id.status_extMinority);
		
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
	
	public void update() {
		if (this.id != null) {
			switch (id.getDocumentType()) {
			case BELGIAN_CITIZEN:
				type.setText(R.string.cardtype_citizen);
				break;
			case KIDS_CARD:
				type.setText(R.string.cardtype_kids);
				break;
			default:
				type.setText(id.getDocumentType().name().replace('_', ' '));
				break;
			}
			name.setText(id.getFamilyName());
			gNames.setText(id.getFirstName() + " " + id.getMiddleNames());
			birthPlace.setText(id.getPlaceOfbirth());
			if (id.getDateOfBirth().getMonth() != DatatypeConstants.FIELD_UNDEFINED) {
				 DateFormat df = DateFormat.getDateInstance();
				birthDate.setText(df.format(id.getDateOfBirth().toGregorianCalendar().getTime()));
			} else {
				birthDate.setText(Integer.toString(id.getDateOfBirth().getYear()));
			}
			switch (id.getGender()) {
			case MALE:
				sex.setText(R.string.sex_male);
				break;
			case FEMALE:
				sex.setText(R.string.sex_female);
				break;
			}
			natNumber.setText(id.getNationalNumber());
			nationality.setText(id.getNationality());
			title.setText(id.getNobleTitle());
			status_whiteCane.setChecked(false);
			status_yellowCane.setChecked(false);
			status_extMinority.setChecked(false);
			for (SpecialStatus status : id.getSpecialStatus()) {
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
		} else {
			type.setText("");
			name.setText("");
			gNames.setText("");
			birthPlace.setText("");
			birthDate.setText("");
			sex.setText("");
			natNumber.setText("");
			nationality.setText("");
			title.setText("");
			status_whiteCane.setChecked(false);
			status_yellowCane.setChecked(false);
			status_extMinority.setChecked(false);
		}
	}
	
}
