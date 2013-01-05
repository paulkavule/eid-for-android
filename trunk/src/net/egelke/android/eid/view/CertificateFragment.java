package net.egelke.android.eid.view;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.x500.X500Principal;

import org.apache.http.message.BufferedHeader;

import android.app.Fragment;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class CertificateFragment extends Fragment {
	
	private static class CertArrayAdapter extends ArrayAdapter<X509Certificate> {
		
		private static Pattern snExtract = Pattern.compile(".*CN=([^,]*).*");

		private int layoutResource;
		private int textViewResource;
		
		public CertArrayAdapter(Context ctx, int layoutResource, int textViewResource) {
			super(ctx, layoutResource, textViewResource);
			
			this.layoutResource = layoutResource;
			this.textViewResource = textViewResource;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			X509Certificate cert = getItem(position);
			
			View item;
			if (convertView == null) {
                LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                item = vi.inflate(layoutResource, null);
            } else {
            	item = convertView;
            }
			TextView itemValue = (TextView) item.findViewById(textViewResource);
			
			
		 	String dn = cert.getSubjectX500Principal().getName();
		 	Log.d("net.egelke.android.eid", "Retrieved DN from certificate: " + dn);
		 	Matcher matcher = snExtract.matcher(dn);
		 	String cn;
		 	if (matcher.matches())
		 		cn = matcher.group(1);
		 	else
		 		cn = dn;
		 	
		 	itemValue.setText(cn);
			return item;
		}
	}
	
	private CertArrayAdapter certsAdapter;
	
	private ListView certs;
	
	private TextView subject;
	
	private TextView from;
	
	private TextView to;
	
	private CheckBox usage_digitalSignature;
	private CheckBox usage_nonRepudiation;
	private CheckBox usage_keyEncipherment;
	private CheckBox usage_dataEncipherment;
	private CheckBox usage_keyAgreement;
	private CheckBox usage_keyCertSign;
	private CheckBox usage_cRLSign;
	private CheckBox usage_encipherOnly;
	private CheckBox usage_decipherOnly;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.certificates, container, false);
		
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			LinearLayout layout = (LinearLayout) v.findViewById(R.id.certLayout);
			layout.setOrientation(LinearLayout.VERTICAL);
		}
		
		certsAdapter = new CertArrayAdapter(getActivity(), R.layout.certificates_item, R.id.certificateItem);
		if (((MainActivity) getActivity()).certs != null) {
			certsAdapter.addAll(((MainActivity) getActivity()).certs);
		}
		
		subject = (TextView) v.findViewById(R.id.certSubject);
		from = (TextView) v.findViewById(R.id.certValidFrom);
		to = (TextView) v.findViewById(R.id.certValidTo);
		usage_digitalSignature = (CheckBox) v.findViewById(R.id.usage_digitalSignature);
		usage_nonRepudiation = (CheckBox) v.findViewById(R.id.usage_nonRepudiation);
		usage_keyEncipherment = (CheckBox) v.findViewById(R.id.usage_keyEncipherment);
		usage_dataEncipherment = (CheckBox) v.findViewById(R.id.usage_dataEncipherment);
		usage_keyAgreement = (CheckBox) v.findViewById(R.id.usage_keyAgreement);
		usage_keyCertSign = (CheckBox) v.findViewById(R.id.usage_keyCertSign);
		usage_cRLSign = (CheckBox) v.findViewById(R.id.usage_cRLSign);
		usage_encipherOnly = (CheckBox) v.findViewById(R.id.usage_encipherOnly);
		usage_decipherOnly = (CheckBox) v.findViewById(R.id.usage_decipherOnly);
		certs = (ListView) v.findViewById(R.id.certificates);
		certs.setAdapter(certsAdapter);
		certs.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
				X509Certificate cert = (X509Certificate) parent.getItemAtPosition(position);
				
				DateFormat df = DateFormat.getDateTimeInstance();
				String subjectValue = cert.getSubjectX500Principal().getName(X500Principal.RFC2253);
				subjectValue = subjectValue.replace(",", "\r\n"); //write line by line
				StringBuffer subjectWriter = new StringBuffer();
				try {
					//We need to convert some RFC2253 stuff
					String subjectLine;
					BufferedReader subjectReader = new BufferedReader(new StringReader(subjectValue));
					while ((subjectLine = subjectReader.readLine()) != null) {
						String[] lineParts = subjectLine.split("=");
						if ("2.5.4.5".equals(lineParts[0])) {
							lineParts[0] = "SERIALNUMBER";
						} else if ("2.5.4.4".equals(lineParts[0])) {
							lineParts[0] = "SURNAME";
						} else if ("2.5.4.42".equals(lineParts[0])) {
							lineParts[0] = "GIVENNAME";
						}
						subjectWriter.append(lineParts[0]);
						subjectWriter.append('=');
						
						if (lineParts[1].startsWith("#13")) { //we should decode...
							int i = 5;
							while ((i+2) <= lineParts[1].length()) {
								subjectWriter.append(new String(new byte[] { (byte) Integer.parseInt(lineParts[1].substring(i, i+2), 16)}));
								i += 2;
							}
						} else {
							subjectWriter.append(lineParts[1]);
						}
						subjectWriter.append("\r\n");
					}
				} catch (IOException e) {
					Log.w("net.egelke.android.eid", "Failed to convert  the subject name", e);
				}
				
				subject.setText(subjectWriter.toString());
				from.setText(df.format(cert.getNotBefore()));
				to.setText(df.format(cert.getNotAfter()));
				
				usage_digitalSignature.setChecked(cert.getKeyUsage()[0]);
				usage_nonRepudiation.setChecked(cert.getKeyUsage()[1]);
				usage_keyEncipherment.setChecked(cert.getKeyUsage()[2]);
				usage_dataEncipherment.setChecked(cert.getKeyUsage()[3]);
				usage_keyAgreement.setChecked(cert.getKeyUsage()[4]);
				usage_keyCertSign.setChecked(cert.getKeyUsage()[5]);
				usage_cRLSign.setChecked(cert.getKeyUsage()[6]);
				usage_encipherOnly.setChecked(cert.getKeyUsage()[7]);
				usage_decipherOnly.setChecked(cert.getKeyUsage()[8]);
				
				
			}
		});
		return v;
	}
	
	public void clearCertificates() {
		certsAdapter.clear();
		if (!this.isDetached()) {
			certsAdapter.notifyDataSetChanged();
		}
	}
	
	public void addCertificates(Collection<X509Certificate> certs) {
		certsAdapter.addAll(certs);
		if (!this.isDetached()) {
			certsAdapter.notifyDataSetChanged();
		}
	}

}
