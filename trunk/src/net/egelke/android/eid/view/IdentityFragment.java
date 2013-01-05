package net.egelke.android.eid.view;

import java.io.ByteArrayInputStream;
import java.text.DateFormat;

import javax.xml.datatype.DatatypeConstants;

import net.egelke.android.eid.model.Address;
import net.egelke.android.eid.model.Identity;
import net.egelke.android.eid.model.SpecialStatus;
import android.app.Fragment;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

public class IdentityFragment extends Fragment {
	
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
	
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.identity, container, false);
		
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
		street = (TextView) v.findViewById(R.id.street);
		zip = (TextView) v.findViewById(R.id.zip);
		municipality = (TextView) v.findViewById(R.id.municipality);
		photo = (ImageView) v.findViewById(R.id.photo);
		
		return v;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		updateId();
		updateAddress();
		updatePhoto();
		
	}
	
	public void updateId() {
		if (((MainActivity) getActivity()).id != null) {
			Identity result = ((MainActivity) getActivity()).id;
			
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
			status_whiteCane.setChecked(false);
			status_yellowCane.setChecked(false);
			status_extMinority.setChecked(false);
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
	
	public void updateAddress() {
		if (((MainActivity) getActivity()).address != null) {
			Address result = ((MainActivity) getActivity()).address;
			
			street.setText(result.getStreetAndNumber());
			zip.setText(result.getZip());
			municipality.setText(result.getMunicipality());
		}
	}
	
	public void updatePhoto() {
		if (((MainActivity) getActivity()).photo != null) {
			
			photo.setImageDrawable(Drawable.createFromStream(new ByteArrayInputStream(((MainActivity) getActivity()).photo), "idPic"));
		}
	}
	
}
