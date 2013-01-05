package net.egelke.android.eid;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Fragment;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
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
	
	CertArrayAdapter certsAdapter;
	
	ListView certs;
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.certificates, container, false);
		
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			LinearLayout layout = (LinearLayout) v.findViewById(R.id.certLayout);
			layout.setOrientation(LinearLayout.VERTICAL);
		}
		
		certsAdapter = new CertArrayAdapter(getActivity(), R.layout.certificates_item, R.id.certificateItem);
		
		certs = (ListView) v.findViewById(R.id.certificates);
		certs.setAdapter(certsAdapter);

		return v;
	}

	@Override
	public void onResume() {
		super.onResume();
		updateCertificates();
	}
	
	public void updateCertificates() {
		if (((MainActivity) getActivity()).certs != null) {
			List<X509Certificate> certs = ((MainActivity) getActivity()).certs;
			
			certsAdapter.clear();
			certsAdapter.addAll(certs);
			certsAdapter.notifyDataSetChanged();
		}
	}

}
