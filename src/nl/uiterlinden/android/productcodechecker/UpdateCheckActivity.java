/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nl.uiterlinden.android.productcodechecker;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

/**
 * This class provides a basic demonstration of how to write an Android
 * activity. Inside of its window, it places a single view: an EditText that
 * displays and edits some internal text.
 */
public class UpdateCheckActivity extends Activity {

	static final private int BACK_ID = Menu.FIRST;

	private TextView mTextView;
	final Handler mHandler = new Handler();
	Process ps;
	ProgressDialog dialog;

	public UpdateCheckActivity() {
	}

	/** Called with the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Inflate our UI from its XML layout description.
		setContentView(R.layout.productcodechecker);

		// Find the text editor view inside the layout, because we
		// want to do various programmatic things with it.
		mTextView = (TextView) findViewById(R.id.TextView);

		// Hook up button presses to the appropriate event handler.
		((Button) findViewById(R.id.back)).setOnClickListener(mBackListener);
		dialog = ProgressDialog.show(UpdateCheckActivity.this, "", 
                "Checking. Please wait...", true);
		new Thread(new VersionChecker()).start();
	}

	private void updateResultsInUi(String message) {
		mTextView.append(message);
	}
	/**
	 * Called when the activity is about to start interacting with the user.
	 */
	@Override
	protected void onResume() {
		super.onResume();
	}

	/**
	 * Called when your activity's options menu needs to be created.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		// We are going to create two menus. Note that we assign them
		// unique integer IDs, labels from our string resources, and
		// given them shortcuts.
		menu.add(0, BACK_ID, 0, R.string.back).setShortcut('0', 'b');

		return true;
	}

	/**
	 * Called right before your activity's option menu is displayed.
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);

		return true;
	}

	/**
	 * Called when a menu item is selected.
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case BACK_ID:
			finish();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	/**
	 * A call-back for when the user presses the back button.
	 */
	OnClickListener mBackListener = new OnClickListener() {
		public void onClick(View v) {
			Log.d("ProductCodeCheck", "Finish");
			finish();
		}
	};

	/**
	 * A call-back for when the user presses the clear button.
	 */
	OnClickListener mClearListener = new OnClickListener() {
		public void onClick(View v) {
			mTextView.setText("");
		}
	};
	
	private void updateStatus(String status) {
		mHandler.post(new ResultUpdater(status + "\n"));
	}
	
    class ResultUpdater implements Runnable {
    	
    	private String message;
    	
    	public ResultUpdater(String message) {
    		this.message = message;
    	}
    	
        public void run() {
            updateResultsInUi(message);
        }

    }
	
	class VersionChecker implements Runnable {

		@Override
		public void run() {

			updateStatus("Checking latest firmware version with Samsung.\n\n");
			
			String[] latestVersion;
			try {
				latestVersion = new KiesCheck().check();
				updateStatus("Latest firmware version info. \n");
				updateStatus("PDA: " + latestVersion[0]);
				updateStatus("PHONE: " + latestVersion[1]);
				updateStatus("CSC: " + latestVersion[2]);
			} catch (Exception e) {
				updateStatus("Could not fetch latest version, reason: " + e.getMessage());
			}
			
			dialog.dismiss();
		}
		
	}
}
