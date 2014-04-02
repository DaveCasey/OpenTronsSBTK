package com.opentrons.sbtk;



import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ImageButton;

public class HomeFragment extends Fragment{

	public HomeFragmentListener parent;
	public int count = 0;
	//public boolean swich = true;
	private ImageButton imageBtn;
	
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
		try {
			parent = (HomeFragmentListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement HomeFragmentListener");
		}
	}
	
	
	@Override
	public View onCreateView(	LayoutInflater inflater, 
								ViewGroup container, 
								Bundle savedInstanceState)
	{
		View v = inflater.inflate(R.layout.home, container, false);
		
		imageBtn = (ImageButton) v.findViewById(R.id.imageView1);
		imageBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				if((count++)>3){
					//if(swich){
						//swich = false;
						parent.unlock();
						count = 0;
						
					//}
				}
			}
		});
		
		
		return v;
	}
	
	public interface HomeFragmentListener {
		void unlock();
	}
	
	
}
