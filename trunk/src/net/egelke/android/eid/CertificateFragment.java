package net.egelke.android.eid;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class CertificateFragment extends eidFragment {
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.certificates, container, false);
	}

	@Override
	public void update() {
		
		
	}

}
