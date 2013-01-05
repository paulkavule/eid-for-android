package net.egelke.android.eid;

import android.app.Fragment;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public class CertificateFragment extends Fragment {
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.certificates, container, false);
		
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			LinearLayout layout = (LinearLayout) v.findViewById(R.id.certLayout);
			layout.setOrientation(LinearLayout.VERTICAL);
		}
		
		return v;
	}

	public void updateCertificates() {
		
		
	}

}
