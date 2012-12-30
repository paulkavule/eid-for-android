package net.egelke.android.eid.model;

import java.util.EnumSet;

import javax.xml.datatype.XMLGregorianCalendar;

//TODO: implement Parcelable
public class Identity {

	String cardNumber;

	public String getCardNumber() {
		return cardNumber;
	}

	String chipNumber;

	public String getChipNumber() {
		return chipNumber;
	}

	Period cardValidity;

	public Period getCardValidity() {
		return cardValidity;
	}

	String cardDeliveryMunicipality;

	public String getCardDeliveryMunicipality() {
		return cardDeliveryMunicipality;
	}

	String nationalNumber;

	public String getNationalNumber() {
		return nationalNumber;
	}

	String familyName;

	public String getFamilyName() {
		return familyName;
	}

	String firstName;

	public String getFirstName() {
		return firstName;
	}

	String middleNames;

	public String getMiddleNames() {
		return middleNames;
	}

	String nationality;

	public String getNationality() {
		return nationality;
	}

	String placeOfBirth;

	public String getPlaceOfbirth() {
		return placeOfBirth;
	}

	XMLGregorianCalendar dateOfBirth;

	public XMLGregorianCalendar getDateOfBirth() {
		return dateOfBirth;
	}

	Gender gender;

	public Gender getGender() {
		return gender;
	}

	String nobleTitle;

	public String getNobleTitle() {
		return nobleTitle;
	}

	DocumentType documentType;

	public DocumentType getDocumentType() {
		return documentType;
	}

	EnumSet<SpecialStatus> specialStatus;

	public EnumSet<SpecialStatus> getSpecialStatus() {
		return specialStatus;
	}

	//byte[] photoDigest;

	//public byte[] getPhotoDigest() {
	//	return photoDigest;
	//}

	String duplicate;

	public String getDuplicate() {
		return duplicate;
	}

	SpecialOrganisation specialOrganisation;

	public SpecialOrganisation getSpecialOrganisation() {
		return specialOrganisation;
	}

	boolean memberOfFamily;

	public boolean isMemberOfFamily() {
		return memberOfFamily;
	}

	

}
