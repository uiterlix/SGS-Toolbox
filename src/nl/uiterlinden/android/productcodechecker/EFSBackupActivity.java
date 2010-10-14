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

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
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
public class EFSBackupActivity extends Activity {

	private static final String SDCARD_PRODUCT_CODE_CHECK = "/sdcard/ProductCodeCheck";

	static final private int BACK_ID = Menu.FIRST;

	private TextView mTextView;
	final Handler mHandler = new Handler();
	Process ps;

	public EFSBackupActivity() {
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
		// ((Button) findViewById(R.id.clear)).setOnClickListener(mClearListener);
		init();
		new Thread(new ProductCodeChecker()).start();
	}

	private void init() {

		// copy the nv_data.bin to the workdir
		if (checkRoot()) {
			mTextView.setText("Got root access. Checking the product code...\n");
			File workDir = new File(SDCARD_PRODUCT_CODE_CHECK);
			if (workDir.exists()) {
				Log.d("ProductCodeCheck", "workdir exists.");
			} else {
				// create workdir
				boolean success = workDir.mkdir();
				Log.d("ProductCodeCheck", "created workdir: " + success);
				if (!workDir.exists()) {
					Log.d("ProductCodeCheck", "Oops... still no workdir.");
				}
			}		
		} else {
			mTextView.setText("No root access. Can't check the product code.\n");
		}

	}
	
	private boolean checkRoot() {
		boolean root = false;
		try {
			Process ps = Runtime.getRuntime().exec("su");

	        DataOutputStream os = new DataOutputStream(ps.getOutputStream());

	        os.writeBytes("exit\n");
	        os.flush();
	        os.close();
	        ps.waitFor();
	        
	        root = (ps.exitValue() == 0);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Log.d("ProductCodeCheck","Hopefully we have root now.");
		return root;
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
	
	class ProductCodeChecker implements Runnable {

		@Override
		public void run() {

			updateStatus("Copying nv_data files to backup dir:\n /sdcard/ProductCodeCheck\n\n");
			// copy the nv_data.bin to the workdir
			
			// create backup folder based on current date/time
			DateFormat format = new SimpleDateFormat("yyyyMMdd-HHmmSS");
			String targetFolder = SDCARD_PRODUCT_CODE_CHECK + "/" + format.format(new Date());
			updateStatus("Backing up to: " + targetFolder);
			File target = new File(targetFolder);
			boolean success = false;
			if (!target.exists()) {
				success = target.mkdir();
			}
			if (!success) {
				updateStatus("Could not create backup folder: " + targetFolder);
				return;
			}
			

			try {
				Process ps = Runtime.getRuntime().exec("su");

		        DataOutputStream os = new DataOutputStream(ps.getOutputStream());

		        os.writeBytes("busybox cp /efs/nv_data.bin " + targetFolder + "\n");
		        os.writeBytes("busybox cp /efs/.nv_data.bak " + targetFolder + "\n");
		        os.writeBytes("busybox cp /efs/nv_data.bin.md5 " + targetFolder + "\n");
		        os.writeBytes("busybox cp /efs/.nv_data.bak.md5 " + targetFolder + "\n");
		        os.flush();
		        os.writeBytes("exit\n");
		        os.flush();
		        os.close();
		        ps.waitFor();
		        
		        updateStatus("Copied nv_data files...");
		        
		        String origFileName = targetFolder + "/nv_data.bin";
		        String backupFileName = targetFolder + "/.nv_data.bak";
		        File orig = new File(origFileName);
		        if (!orig.exists()) {
		        	updateStatus("nv_data.bin could not be read!");
		        } else {
		        	updateStatus("Product code in nv_data.bin: " + fetchProductCode(origFileName));
		        }
		        
		        File backup = new File(backupFileName);
		        if (!backup.exists()) {
		        	updateStatus("No backup .nv_data.bak found!");
		        } else {
		        	updateStatus("Product code in backup .nv_data.bak: " + fetchProductCode(backupFileName));
		        }

			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		String fetchProductCode(String filename) throws IOException {
			String code = null;
			
	        FileInputStream is = new FileInputStream(filename);
	        byte[] tmp = new byte[10000];
	        for (int i = 0; i < 160; i++) {
	        	is.read(tmp);
	        }
	        is.read(new byte[5657]);

    		byte[] buf = new byte[13];
    		is.read(buf);
    		code = new String(buf);

    		is.close();
	        
			return code;
		}
		
	}
}
