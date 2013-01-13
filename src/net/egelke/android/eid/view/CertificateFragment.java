package net.egelke.android.eid.view;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.security.cert.CertPath;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertStore;
import java.security.cert.CertificateFactory;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.x500.X500Principal;

import android.app.Fragment;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class CertificateFragment extends Fragment {
	
	private static class X509CertificateItem {
		private static Pattern snExtract = Pattern.compile(".*CN=([^,]*).*");
		
		private X509Certificate value;
		
		X509CertificateItem(X509Certificate value) {
			this.value = value;
		}
		
		public X509Certificate getValue() {
			return value;
		}
		
		@Override
		public String toString() {
			String dn = value.getSubjectX500Principal().getName();
		 	Log.d("net.egelke.android.eid", "Retrieved DN from certificate: " + dn);
		 	Matcher matcher = snExtract.matcher(dn);
		 	String cn;
		 	if (matcher.matches())
		 		cn = matcher.group(1);
		 	else
		 		cn = dn;
		 	return cn;
		}
	}
	
	private class Check extends AsyncTask<Void, Void, Boolean> {
	
		
		@Override
		protected Boolean doInBackground(Void... params) {
			try {
				CertificateFactory cf = CertificateFactory.getInstance("X.509");
				CertPath path = cf.generateCertPath(Collections.singletonList(current));
				
				CertStore store = CertStore.getInstance("Collection", new CollectionCertStoreParameters(((MainActivity) getActivity()).certs));
				
				CertPathValidator validator = CertPathValidator.getInstance("PKIX");
				Set<TrustAnchor> roots = new HashSet<TrustAnchor>();
				roots.add(new TrustAnchor((X509Certificate) cf.generateCertificate(getResources().getAssets().open("root.crt")), null));
				roots.add(new TrustAnchor((X509Certificate) cf.generateCertificate(getResources().getAssets().open("root2.crt")), null));
				PKIXParameters validateParams = new PKIXParameters(roots);
				validateParams.setCertStores(Collections.singletonList(store));
				validateParams.setRevocationEnabled(true);
				
				validator.validate(path, validateParams);
				return true;
			} catch (CertPathValidatorException e) {
				Log.i("net.egelke.android.eid", "certificate invalid", e);
				return false;
			} catch (Exception e) {
				Log.e("net.egelke.android.eid", "failed to check the certificate", e);
				return false;
			}
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
			
		}
	}
	
	private ArrayAdapter<X509CertificateItem> certsAdapter;
	
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
	
	private Button check;
	
	private X509Certificate current;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.certificates, container, false);
		
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			LinearLayout layout = (LinearLayout) v.findViewById(R.id.certLayout);
			layout.setOrientation(LinearLayout.VERTICAL);
		}
		
		certsAdapter = new ArrayAdapter<X509CertificateItem>(getActivity(), R.layout.certificates_item, R.id.certificateItem);
		if (((MainActivity) getActivity()).certs != null) {
			for(X509Certificate cert : ((MainActivity) getActivity()).certs) {
				certsAdapter.add(new X509CertificateItem(cert));
			}
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
				current = (X509Certificate) parent.getItemAtPosition(position);
				
				DateFormat df = DateFormat.getDateTimeInstance();
				String subjectValue = current.getSubjectX500Principal().getName(X500Principal.RFC2253);
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
				from.setText(df.format(current.getNotBefore()));
				to.setText(df.format(current.getNotAfter()));
				
				usage_digitalSignature.setChecked(current.getKeyUsage()[0]);
				usage_nonRepudiation.setChecked(current.getKeyUsage()[1]);
				usage_keyEncipherment.setChecked(current.getKeyUsage()[2]);
				usage_dataEncipherment.setChecked(current.getKeyUsage()[3]);
				usage_keyAgreement.setChecked(current.getKeyUsage()[4]);
				usage_keyCertSign.setChecked(current.getKeyUsage()[5]);
				usage_cRLSign.setChecked(current.getKeyUsage()[6]);
				usage_encipherOnly.setChecked(current.getKeyUsage()[7]);
				usage_decipherOnly.setChecked(current.getKeyUsage()[8]);
				
			}
		});
		
		/*
		check = (Button) v.findViewById(R.id.verifyOcsp);
		check.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				new Check().execute();
			}
		});
		*/
		
		return v;
	}
	
	public void clearCertificates() {
		certsAdapter.clear();
		if (!this.isDetached()) {
			certsAdapter.notifyDataSetChanged();
		}
	}
	
	public void addCertificates(Collection<X509Certificate> certs) {
		for(X509Certificate cert : certs) {
			certsAdapter.add(new X509CertificateItem(cert));
		}
		if (!this.isDetached()) {
			certsAdapter.notifyDataSetChanged();
		}
	}

}
