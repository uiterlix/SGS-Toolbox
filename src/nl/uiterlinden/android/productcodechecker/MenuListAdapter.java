package nl.uiterlinden.android.productcodechecker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class MenuListAdapter extends BaseAdapter {

	LayoutInflater mInflater = null;

	List<MenuItem> items = new ArrayList<MenuItem>();
	Map<String, MenuItem> itemMap = new HashMap<String, MenuItem>();
	boolean hasRoot = false;
	
	public MenuListAdapter(LayoutInflater inflater, boolean hasRoot) {
		mInflater = inflater;
		this.hasRoot = hasRoot;
	}
	
	public void setRoot(boolean root) {
		this.hasRoot = root;
	}
	
	public void addItem(String name, boolean needsRoot) {
		MenuItem item = new MenuItem(name, needsRoot);
		items.add(item);
		itemMap.put(name, item);
	}
	
	public void removeItem(String name) {
		MenuItem item = itemMap.get(name);
		items.remove(item);
		itemMap.remove(name);
	}
	
	@Override
	public int getCount() {
		return items.size();
	}

	@Override
	public Object getItem(int position) {
		return items.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}
	
	@Override
	public boolean isEnabled(int position) {
		MenuItem item = items.get(position);
		return (!item.needsRoot() || (item.needsRoot() && hasRoot));
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = null;
		MenuItem item = items.get(position);
		if (!item.needsRoot() || (item.needsRoot() && hasRoot)) {
			v = mInflater.inflate(R.layout.listitem, null);
		} else {
			v = mInflater.inflate(R.layout.listitem_disabled, null);
		}
		((TextView)v).setText(item.getName());
		return v;
	}
	
	class MenuItem {
		
		String name;
		boolean needsRoot;
		
		public MenuItem(String name, boolean needsRoot) {
			this.name = name;
			this.needsRoot = needsRoot;
		}

		public String getName() {
			return name;
		}

		public boolean needsRoot() {
			return needsRoot;
		}
		
	}

	public boolean containsItem(String string) {
//		Log.d("ProductCodeCheck", "list contains: " + string);
		for (String key : itemMap.keySet()) {
//			Log.d("ProductCodeCheck", "key: '" + key + "'");
			if (key.equals(string)) {
				return true;
			}
		}
		return false;
	}

}
