package net.egelke.android.eid.model;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import android.util.Log;

public class ObjectFactory {
	private static final String[][] MONTHS = new String[][] { new String[] { "JAN" }, new String[] { "FEV", "FEB" }, new String[] { "MARS", "MAAR", "M??R" },
			new String[] { "AVR", "APR" }, new String[] { "MAI", "MEI" }, new String[] { "JUIN", "JUN" }, new String[] { "JUIL", "JUL" },
			new String[] { "AOUT", "AUG" }, new String[] { "SEPT", "SEP" }, new String[] { "OCT", "OKT" }, new String[] { "NOV" },
			new String[] { "DEC", "DEZ" } };

	private static String[] GENDER_MALE = { "M" };
	private static String[] GENDER_FEMALE = { "F", "V", "W" };
	
	public Map<Integer, byte[]> createTvMap(byte[] bytes) throws IOException {
		return createTvMap(new ByteArrayInputStream(bytes));
	}
	
	public Map<Integer, byte[]> createTvMap(InputStream steam) throws IOException {
		int tag;
		Map<Integer, byte[]> values = new TreeMap<Integer, byte[]>();
		while ((tag = steam.read()) != -1) {
			int len = 0;
			int lenByte;
			do {
				lenByte = steam.read();
				len = (len << 7) + (lenByte & 0x7F);
			} while ((lenByte & 0x80) == 0x80);
			
			//In case the file is padded with nulls
			if (tag == 0 && len == 0) break;
			
			byte[] value = new byte[len];
			int read = 0;
			while (read < len) {
				read += steam.read(value, read, len - read);
			}
			Log.d("net.egelke.android.eid", String.format("Added tag %d (len %d)", tag, value.length));
			values.put(tag, value);
		}
		return values;
	}

	public Identity createIdentity(Map<Integer, byte[]> tvMap) {
		Identity id = new Identity();
		id.cardNumber = toString(tvMap.get(1));
		id.chipNumber = toHexString(tvMap.get(2));
		id.cardValidity = new Period();
		id.cardValidity.begin = toCalendar(tvMap.get(3));
		id.cardValidity.end = toCalendar(tvMap.get(4));
		id.cardDeliveryMunicipality = toString(tvMap.get(5));
		id.nationalNumber = toString(tvMap.get(6));
		id.familyName = toString(tvMap.get(7));
		id.firstName = toString(tvMap.get(8));
		id.middleNames = toString(tvMap.get(9));
		id.nationality = toString(tvMap.get(10));
		id.placeOfBirth = toString(tvMap.get(11));
		id.dateOfBirth = toXmlCalendar(tvMap.get(12));
		id.gender = toGender(tvMap.get(13));
		id.nobleTitle = toString(tvMap.get(14));
		id.documentType = toDocType(tvMap.get(15));
		id.specialStatus = toStatusSet(tvMap.get(16));
		//photo digest isn't important here, so we skip it for the time being
		id.duplicate = toString(tvMap.get(18));
		id.specialOrganisation = toSpecialOrganisation(tvMap.get(19));
		//waiting for documentation...
		id.memberOfFamily = true;
		
		return id;
	}
	
	public Address createAddress(Map<Integer, byte[]> tvMap) {
		Address a = new Address();
		a.streetAndNumber = toString(tvMap.get(1));
		a.zip = toString(tvMap.get(2));
		a.municipality = toString(tvMap.get(3));
		
		return a;
	}

	private String toString(byte[] array) {
		if (array == null) return null;
		
		try {
			return new String(array, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			Log.e("net.egelke.android.eid", "Failed to convert to string", e);
			return null;
		}
	}

	private String toHexString(byte[] array) {
		StringBuffer hexString = new StringBuffer();
		for (byte b : array) {
			int intVal = b & 0xff;
			if (intVal < 0x10)
				hexString.append("0");
			hexString.append(Integer.toHexString(intVal));
		}
		return hexString.toString();
	}

	private Gender toGender(final byte[] value) {
		final String genderStr = toString(value);

		if (Arrays.binarySearch(GENDER_MALE, genderStr) >= 0) {
			return Gender.MALE;
		}
		if (Arrays.binarySearch(GENDER_FEMALE, genderStr) >= 0) {
			return Gender.FEMALE;
		}

		throw new RuntimeException("unknown gender: " + genderStr);
	}

	private DocumentType toDocType(final byte[] value) {
		int docTypeCode = Integer.parseInt(toString(value), 10);
		switch (docTypeCode) {
		case 1:
			return DocumentType.BELGIAN_CITIZEN;
		case 6:
			return DocumentType.KIDS_CARD;
		case 7:
			return DocumentType.BOOTSTRAP_CARD;
		case 8:
			return DocumentType.HABILITATION_CARD;
		case 11:
			return DocumentType.FOREIGNER_A;
		case 12:
			return DocumentType.FOREIGNER_B;
		case 13:
			return DocumentType.FOREIGNER_C;
		case 14:
			return DocumentType.FOREIGNER_D;
		case 15:
			return DocumentType.FOREIGNER_E;
		case 16:
			return DocumentType.FOREIGNER_E_PLUS;
		case 17:
			return DocumentType.FOREIGNER_F;
		case 18:
			return DocumentType.FOREIGNER_F_PLUS;
		case 19:
			return DocumentType.EUROPEAN_BLUE_CARD_H;
		default:
			throw new RuntimeException("Unsupported card type: " + toString(value));
		}
	}
	
	private EnumSet<SpecialStatus> toStatusSet(final byte[] value) {
		int statusCode = Integer.parseInt(toString(value), 10);
		switch(statusCode) {
		case 0:
			return EnumSet.noneOf(SpecialStatus.class);
		case 1:
			return EnumSet.of(SpecialStatus.WHITE_CANE);
		case 2:
			return EnumSet.of(SpecialStatus.EXTENDED_MINORITY);
		case 3:
			return EnumSet.of(SpecialStatus.WHITE_CANE, SpecialStatus.EXTENDED_MINORITY);
		case 4:
			return EnumSet.of(SpecialStatus.YELLOW_CANE);
		case 5:
			return EnumSet.of(SpecialStatus.YELLOW_CANE, SpecialStatus.EXTENDED_MINORITY);
		default:
			throw new RuntimeException("Unsupported special status: " + toString(value));
		}
	}
	
	private SpecialOrganisation toSpecialOrganisation(final byte[] value) {
		if (value == null || value.length == 0) return null;
		
		int orgCode = Integer.parseInt(toString(value), 10);
		switch(orgCode) {
		case 1:
			return SpecialOrganisation.SHAPE;
		case 2:
			return SpecialOrganisation.NATO;
		case 4:
			return SpecialOrganisation.FORMER_BLUE_CARD_HOLDER;
		case 5:
			return SpecialOrganisation.RESEARCHER;
		default:
			throw new RuntimeException("Unsupported special organiation");
		}
	}

	private Calendar toCalendar(final byte[] value) {
		final String dateStr = new String(value);
		Log.d("net.egelke.android.eid", String.format("Converting %s to calendar", dateStr));
		
		final int day = Integer.parseInt(dateStr.substring(0, 2));
		final int month = Integer.parseInt(dateStr.substring(3, 5));
		final int year = Integer.parseInt(dateStr.substring(6));
		return new GregorianCalendar(year, month -1, day);
	}

	private XMLGregorianCalendar toXmlCalendar(final byte[] value) {
		DatatypeFactory dfFactory;
		try {
			dfFactory = DatatypeFactory.newInstance();
		} catch (DatatypeConfigurationException e) {
			throw new RuntimeException(e);
		}
		XMLGregorianCalendar xmlCal = dfFactory.newXMLGregorianCalendar();
		String dateOfBirthStr;
		try {
			dateOfBirthStr = new String(value, "UTF-8").trim();
		} catch (final UnsupportedEncodingException uex) {
			return null;
		}
		int spaceIdx = dateOfBirthStr.indexOf('.');
		if (-1 == spaceIdx) {
			spaceIdx = dateOfBirthStr.indexOf(' ');
		}

		if (spaceIdx > 0) {
			final String dayStr = dateOfBirthStr.substring(0, spaceIdx);
			xmlCal.setDay(Integer.parseInt(dayStr));
			String monthStr = dateOfBirthStr.substring(spaceIdx + 1, dateOfBirthStr.length() - 4 - 1);
			if (monthStr.endsWith(".")) {
				monthStr = monthStr.substring(0, monthStr.length() - 1);
			}
			final String yearStr = dateOfBirthStr.substring(dateOfBirthStr.length() - 4);
			xmlCal.setYear(Integer.parseInt(yearStr));
			xmlCal.setMonth(toMonth(monthStr));

			return xmlCal;
		}

		if (dateOfBirthStr.length() == 4) {
			xmlCal.setYear(Integer.parseInt(dateOfBirthStr));
			return xmlCal;
		}

		throw new RuntimeException("Unsupported Birth Date Format [" + dateOfBirthStr + "]");
	}

	private int toMonth(String monthStr) {
		monthStr = monthStr.trim();
		for (int monthIdx = 0; monthIdx < MONTHS.length; monthIdx++) {
			final String[] monthNames = MONTHS[monthIdx];
			for (String monthName : monthNames) {
				if (monthName.equals(monthStr)) {
					return monthIdx + 1;
				}
			}
		}
		throw new RuntimeException("unknown month: " + monthStr);
	}
}
