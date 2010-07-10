package com.sofurry.favorites;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MainMenu extends Activity {

	Button buttonBrowser;
	Button buttonNext;
	Button buttonSave;

	public static int RESULT_BROWSER = 1; 
	public static int RESULT_NEXT = 2; 
	public static int RESULT_SAVE = 3; 

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.menu);
		buttonBrowser = (Button) findViewById(R.id.ButtonBrowser);
		buttonNext = (Button) findViewById(R.id.ButtonNext);
		buttonSave = (Button) findViewById(R.id.ButtonSave);
		
		buttonBrowser.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View arg0) {
				LiveWallpaperPainting.launchBrowser();
				finish();
			}
		});

		buttonNext.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View arg0) {
				LiveWallpaperPainting.launchNextImage();
				finish();
			}
		});

		buttonSave.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View arg0) {
				LiveWallpaperPainting.launchSaveImage();
				finish();
			}
		});

	}

	
}
