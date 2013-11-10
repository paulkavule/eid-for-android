package net.egelke.android.eid.view;

import android.app.Fragment;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public class PhotoFragment extends Fragment {

	private Drawable photo;

	private ImageView image;
	
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		this.setRetainInstance(true);
		
		View v = inflater.inflate(R.layout.photo, container, false);
		
		image = (ImageView) v.findViewById(R.id.photo);
		
		return v;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		update();
		
	}
	
	public void setPhoto(Drawable photo) {
		this.photo = photo;
		if (!isDetached()) update();
	}
	
	public void update() {
		if (this.photo != null) {
			image.setImageDrawable(this.photo);
		} else {
			image.setImageDrawable(null);
		}
	}
	
}
