package com.example.other;

import com.example.multidextest.HelperOne;
import com.example.multidextest.R;

import android.app.Activity;
import android.os.Bundle;

public class OtherActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.other_layout);
		
		HelperTwo helperTwo = new HelperTwo();
		helperTwo.haha();
	}

}
