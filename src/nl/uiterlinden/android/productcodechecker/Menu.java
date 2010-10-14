package nl.uiterlinden.android.productcodechecker;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class Menu extends ListActivity {

	private static final String MOUNT_SYSTEM_AS_R_O = "Mount /system as ro (current: rw)";

	private static final String MOUNT_SYSTEM_AS_R_W = "Mount /system as rw (current: ro)";

	private static final String PRODUCT_CODE_CHECK = "ProductCodeCheck";

	@Override
	protected void onResume() {
		super.onResume();
		hasRoot = checkRoot();
		if (adapter != null) {
			adapter.setRoot(hasRoot);
		}
		checkSystemStatus();
		updateStatusInMenu();
	}

	static final String[] ITEMS = new String[] { "Check product code", 
		"Phone & battery info", 
		"Firmware info", 
		"GPS test menu", 
		"Show IMEI", 
		"CSC Selection", 
		"Check latest kies update",
		"Reboot", 
		"Reboot into recovery" };
	
	private String status = null;
	private String dev = null;

	private MenuListAdapter adapter;

	private ListView listView;

	private boolean hasRoot;
	
	private int errorcode = 0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
	  super.onCreate(savedInstanceState);

	  hasRoot = checkRoot();
	  checkSystemStatus();
	  adapter = new MenuListAdapter((LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE), hasRoot);
	  for (String item : ITEMS) {
		  boolean needsRoot = false;
		  if (item.equals("Check product code") || item.equals("Reboot") || item.equals("Reboot into recovery")) {
			  needsRoot = true;
		  }
		  adapter.addItem(item, needsRoot);
	  }
	  setListAdapter(adapter);

	  listView = getListView();
	  listView.setTextFilterEnabled(true);
	  
	  updateStatusInMenu();
	  
	  if (status.equals("unsupported")) {
		  Toast.makeText(getApplicationContext(), "System remount not supported on this device. Error code: " + errorcode,
		          Toast.LENGTH_LONG).show();
	  }

	  listView.setOnItemClickListener(new OnItemClickListener() {
	    public void onItemClick(AdapterView<?> parent, View view,
	        int position, long id) {
	      // When clicked, show a toast with the TextView text
	      Toast.makeText(getApplicationContext(), ((TextView) view).getText(),
	          Toast.LENGTH_SHORT).show();
	      
	      if (((TextView)view).getText().equals("Check product code")) {
	    	  // start activity
	    	  Intent myIntent = new Intent(Menu.this, ProductCodeCheckerActivity.class);
	    	  Menu.this.startActivity(myIntent);	    	  
	      }
	      if (((TextView)view).getText().equals("Check latest kies update")) {
	    	  // start activity
	    	  Intent myIntent = new Intent(Menu.this, UpdateCheckActivity.class);
	    	  Menu.this.startActivity(myIntent);	    	  
	      }
	      if (((TextView)view).getText().equals("Firmware info")) {
	    	  // start activity
	    	  String encodedHash = Uri.encode("#");
	    	  String ussd = "*" + encodedHash + "1234" + encodedHash;
	    	  startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + ussd)));
	      }
	      if (((TextView)view).getText().equals("GPS test menu")) {
	    	  // start activity
	    	  String encodedHash = Uri.encode("#");
	    	  String ussd = "*" + encodedHash + "*" + encodedHash + "1472365" + encodedHash + "*" + encodedHash + "*";
	    	  startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + ussd)));
	      }
	      if (((TextView)view).getText().equals("Show IMEI")) {
	    	  // start activity
	    	  String encodedHash = Uri.encode("#");
	    	  String ussd = "*" + encodedHash + "06" + encodedHash;
	    	  startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + ussd)));
	      }	 	      
	      if (((TextView)view).getText().equals("Phone & battery info")) {
	    	  // start activity
	    	  String encodedHash = Uri.encode("#");
	    	  String ussd = "*" + encodedHash + "*" + encodedHash + "4636" + encodedHash + "*" + encodedHash + "*";
	    	  startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + ussd)));
	      }	 	
	      if (((TextView)view).getText().equals("CSC Selection")) {
	    	  // *#272*????#
	    	  String encodedHash = Uri.encode("#");
	    	  DateFormat dateFormat = new SimpleDateFormat("HHmm");
	    	  String currentTime = dateFormat.format(new Date());
	    	  String ussd = "*" + encodedHash + "272" + "*" + currentTime + encodedHash;
	    	  startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + ussd)));
	      }
	      if (((TextView)view).getText().equals("Reboot")) {
	    	  reboot();
	      }
	      if (((TextView)view).getText().equals("Reboot into recovery")) {
	    	  rebootRecovery();
	      }
	      if (((TextView)view).getText().equals(MOUNT_SYSTEM_AS_R_O)) {
	    	  remount("ro");
	    	  updateStatusInMenu();
	    	  if (status.equals("ro")) {
	    		  Toast.makeText(getApplicationContext(), "remounted /system as r/o",
	    		          Toast.LENGTH_SHORT).show();
	    	  }
	      }
	      if (((TextView)view).getText().equals(MOUNT_SYSTEM_AS_R_W)) {
	    	  remount("rw");
	    	  updateStatusInMenu();
	    	  if (status.equals("rw")) {
	    		  Toast.makeText(getApplicationContext(), "remounted /system as r/w",
	    		          Toast.LENGTH_SHORT).show();
	    	  }
	      }	      
	    }
	  });
	}
	
	private void updateStatusInMenu() {
		Log.d(PRODUCT_CODE_CHECK, "current status: " + status);
		if (status.equals("ro")) {
			  adapter.removeItem(MOUNT_SYSTEM_AS_R_O);
			  Log.d(PRODUCT_CODE_CHECK, "Removed r/o item");
			  if (!adapter.containsItem(MOUNT_SYSTEM_AS_R_W)) {
				  Log.d(PRODUCT_CODE_CHECK, "Adding r/w item");
				  adapter.addItem(MOUNT_SYSTEM_AS_R_W, true);
			  }
		  } else if (status.equals("rw")) {
			  adapter.removeItem(MOUNT_SYSTEM_AS_R_W);
			  Log.d(PRODUCT_CODE_CHECK, "Removed r/w item");
			  if (!adapter.containsItem(MOUNT_SYSTEM_AS_R_O)) {
				  Log.d(PRODUCT_CODE_CHECK, "Adding r/o item");
				  adapter.addItem(MOUNT_SYSTEM_AS_R_O, true);
			  }
		  }
		setListAdapter(adapter);
		
	}
	
	private void reboot() {
		showDialog(1);
	}
	
	private void doReboot() {
		try {
			Process ps = Runtime.getRuntime().exec("su");

	        DataOutputStream os = new DataOutputStream(ps.getOutputStream());

	        os.writeBytes("reboot\n");
	        os.writeBytes("exit\n");
	        os.flush();
	        os.close();
	        ps.waitFor();
	        
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	
	private void rebootRecovery() {
		showDialog(2);
	}
	
	private void doRebootRecovery() {
		try {
			Process ps = Runtime.getRuntime().exec("su");

	        DataOutputStream os = new DataOutputStream(ps.getOutputStream());
	        os.writeBytes("reboot recovery\n");
	        os.writeBytes("exit\n");
	        os.flush();
	        os.close();
	        ps.waitFor();
	        
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		
		if (id == 1) {
		builder.setMessage("Really reboot ?")
		       .setCancelable(false)
		       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		                doReboot();
		           }
		       })
		       .setNegativeButton("No", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   dialog.cancel();
		           }
		       });
		} else if (id == 2) {
			builder.setMessage("Really reboot into recovery ?")
		       .setCancelable(false)
		       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		                doRebootRecovery();
		           }
		       })
		       .setNegativeButton("No", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   dialog.cancel();
		           }
		       });			
		}
		AlertDialog alert = builder.create();
		return alert;
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
		Log.d(PRODUCT_CODE_CHECK,"Hopefully we have root now.");
		return root;
	}
	
	private void remount(String newStatus) {
		try {
			Process ps = Runtime.getRuntime().exec("su");

	        DataOutputStream os = new DataOutputStream(ps.getOutputStream());
	        
//	        os.writeBytes("busybox mount -o remount," + newStatus + " -t rfs " + dev + " /system\n");
	        os.writeBytes("busybox mount -o remount," + newStatus + " /system\n");
	        os.writeBytes("exit\n");
	        os.flush();
	        os.close();
	        ps.waitFor();
	        
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		checkSystemStatus();
	}
	
	private void checkSystemStatus() {
		Log.d(PRODUCT_CODE_CHECK, "checkSystemStatus");
		String systemStatus = null;
		try {
			Log.d(PRODUCT_CODE_CHECK, "shell");
			Process ps = Runtime.getRuntime().exec("busybox sh");
			Log.d(PRODUCT_CODE_CHECK, "create streams");
	        DataOutputStream os = new DataOutputStream(ps.getOutputStream());
	        DataInputStream is = new DataInputStream(ps.getInputStream());
	        Log.d(PRODUCT_CODE_CHECK, "grep /system mount");
	        os.writeBytes("busybox mount |busybox grep /system\n");
	        os.writeBytes("exit\n");
	        os.flush();
	        
	        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
	        String line = null;
	        StringBuilder result = new StringBuilder();
	        Log.d(PRODUCT_CODE_CHECK, "read result");
	        while ((line = reader.readLine()) != null) {
	        	result.append(line);
	        }
	        Log.d(PRODUCT_CODE_CHECK, "result: " + result.toString());
	        if (result == null || result.length() == 0) {
	        	errorcode = 105;
	        	systemStatus = "unsupported";
	        } else {
	        	int fb = result.indexOf("(");
	        	int rb = result.indexOf(")");
	        	if (fb > 0 && rb > 0 && (rb > fb)) {
			        String[] flags = result.substring(result.indexOf("(") + 1, result.indexOf(")")).split(",");
			        List<String> lFlags = Arrays.asList(flags);
			        if (lFlags.contains("ro")) {
			        	systemStatus = "ro";
			        } else if (lFlags.contains("rw")) {
			        	systemStatus = "rw";
			        } else {
			        	Log.d(PRODUCT_CODE_CHECK, "Unknown system status: " + result.toString());
			        	errorcode = 106;
			        	systemStatus = "unsupported";
			        }
			        if (!systemStatus.equals("unsupported")) {
				        dev = result.toString().split(" ")[0];
				        Log.d(PRODUCT_CODE_CHECK, "Device for system folder: '" + dev + "'");
			        }
	        	} else {
	        		Log.d(PRODUCT_CODE_CHECK, "Unparseable mount status");
	        		errorcode = 107;
	        		systemStatus = "unsupported";
	        	}
	        }
	        
	        os.close();
	        ps.waitFor();
	        
		} catch (InterruptedException e) {
			Log.d(PRODUCT_CODE_CHECK, "InterrupedException: " + e.getMessage());
			errorcode = 108;
			systemStatus = "unsupported";
		} catch (IOException e) {
			Log.d(PRODUCT_CODE_CHECK, "IOException: " + e.getMessage());
			errorcode = 109;
			systemStatus = "unsupported";
		} catch (Exception e) {
			Log.d(PRODUCT_CODE_CHECK, "Exception: " + e.getMessage());
			errorcode = 110;
			systemStatus = "unsupported";
		}
		this.status = systemStatus;
	}
}
