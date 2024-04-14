package com.statsmind.scrcpy.client;

import android.app.Activity;
import android.os.Bundle;

public class PhoneActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone);

        String serverURL = savedInstanceState.getString("SERVER_URL");

    }
}