package org.mobilesynergies.android.epic.service.administration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;


import org.mobilesynergies.android.epic.service.R;
import org.mobilesynergies.android.epic.service.interfaces.EpicCommandInfoImpl;
import org.mobilesynergies.android.epic.service.interfaces.IPresenceStatusChangeCallback;
import org.mobilesynergies.android.epic.service.interfaces.NetworkNodeImpl;
import org.mobilesynergies.android.epic.service.remoteui.RemoteUserInterfaceActivity;
import org.mobilesynergies.epic.client.NetworkNode;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AbsListView;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;


/**
 * 
 * Activity to display the Epic network of the user and the availability of the network nodes.
 * 
 * @author Peter
 */
public class ExploreEpicNetwork extends ServiceAdministrationActivity implements
OnCreateContextMenuListener,
ExpandableListView.OnChildClickListener, ExpandableListView.OnGroupCollapseListener,
ExpandableListView.OnGroupExpandListener {

	private static String CLASS_TAG = ExploreEpicNetwork.class.getSimpleName(); 


	/**
	 * The network nodes currently registered in the users epic network 
	 */
	private Map<String, NetworkNode> mMapNetworkNodes = new HashMap<String, NetworkNode>();

	/**
	 * The adapter used in combination with the ListView 
	 */
	ExpandableListAdapter mAdapter;

	/**
	 * The view listing the network nodes
	 */
	ExpandableListView mList;

	boolean mFinishedStart = false;




	/**
	 * A handler that makes changes to the UI when a change to the network nodes happens. 
	 */
	private Handler mListChangeHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			if(mMapNetworkNodes==null){
				return;
			}
			
			((BaseExpandableListAdapter) mAdapter).notifyDataSetChanged();
			
		}
	};


	private IPresenceStatusChangeCallback mPresenceCallback = new IPresenceStatusChangeCallback(){

		@Override
		public void onPresenceStatusChanged(NetworkNodeImpl node)
		throws RemoteException {

			mMapNetworkNodes.remove(node.mAddress.getFullAddressString());

			if(mMapNetworkNodes.containsKey(node.mAddress.getBareAddressString())) {
				mMapNetworkNodes.remove(node.mAddress.getBareAddressString());
			}

			mMapNetworkNodes.put(node.mAddress.getFullAddressString(), node);
			
			MyExpandableListAdapter adapter = (MyExpandableListAdapter)mAdapter;
			
			Collection<NetworkNode> nodes = mMapNetworkNodes.values();
			Iterator<NetworkNode> nodesIterator = nodes.iterator();
			while (nodesIterator.hasNext()) {
				NetworkNode currNode = nodesIterator.next();
				String addr = currNode.mAddress.getFullAddressString();
				boolean availability = currNode.mAvailability.equalsIgnoreCase(NetworkNode.AVAILABILITY_AVAILABLE);
				adapter.changeAvailability(addr, availability);
				adapter.addNode(addr);
			}
			

			mListChangeHandler.sendEmptyMessage(0);
		}

		@Override
		public IBinder asBinder() {
			// TODO Auto-generated method stub
			return null;
		}

	};

	public static final String INTENTACTION = "org.mobilesynergies.android.epic.service.explore";

	/** 
	 * Called when the activity is first created. 
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.expandable_list_layout);

		// Set up our adapter
		MyExpandableListAdapter adapter = new MyExpandableListAdapter();
		mAdapter = adapter;
		setListAdapter(mAdapter);

		//registerForContextMenu(getExpandableListView());
	}

	protected void onConnected(){
	}

	protected void onDisconnected(){
	}


	@Override
	protected void onConnectedToEpicNetwork() {
		try {
			mEpicService.registerPresenceStatusChangeCallback(mPresenceCallback );
			NetworkNode[] nodes = mEpicService.getNetworkNodes();
			if(nodes==null){
				return;
			}
			for(int i=0; i<nodes.length; i++){
				if(nodes[i].mAddress !=null){
					String address = nodes[i].mAddress.getFullAddressString();
					if((address!=null)&&(address.length()>0)){
						mMapNetworkNodes.put(nodes[i].mAddress.getFullAddressString(), nodes[i]);
					}
				}
			}
			mListChangeHandler.sendEmptyMessage(0);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		menu.setHeaderTitle("Sample menu");
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		return false;
	}

	/**
	 * Override this for receiving callbacks when a child has been clicked.
	 * <p>
	 * {@inheritDoc}
	 */
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
			int childPosition, long id) {

		EpicCommandInfoImpl info = ((MyExpandableListAdapter) mAdapter).getChildEpicCommandInfo(groupPosition, childPosition);
		
		Intent intent = new Intent(ExploreEpicNetwork.this, RemoteUserInterfaceActivity.class);
		intent.putExtra(RemoteUserInterfaceActivity.EXTRAS_NODE_FULLADDRESS, info.getEpicNodeId());
		intent.putExtra(RemoteUserInterfaceActivity.EXTRAS_COMMAND_ID, info.getEpicCommandId());
		intent.putExtra(RemoteUserInterfaceActivity.EXTRAS_COMMAND_HUMANREADABLENAME, info.getHumanReadableName());
		startActivity(intent);
		return true;
	}

	/**
	 * Override this for receiving callbacks when a group has been collapsed.
	 */
	public void onGroupCollapse(int groupPosition) {
	}

	/**
	 * Override this for receiving callbacks when a group has been expanded.
	 */
	public void onGroupExpand(int groupPosition) {
		String node = (String) mAdapter.getGroup(groupPosition);
		try {
			EpicCommandInfoImpl[] commands = mEpicService.getRemoteCommands(node);
			if(commands==null){
				return;
			}
			for(int i=0; i<commands.length; i++){
				((MyExpandableListAdapter) mAdapter).addCommand(node, commands[i]);
			}
			mListChangeHandler.sendEmptyMessage(0);		
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Ensures the expandable list view has been created before Activity restores all
	 * of the view states.
	 * 
	 *@see Activity#onRestoreInstanceState(Bundle)
	 */
	@Override
	protected void onRestoreInstanceState(Bundle state) {
		ensureList();
		super.onRestoreInstanceState(state);
	}

	/**
	 * Updates the screen state (current list and other views) when the
	 * content changes.
	 * 
	 * @see Activity#onContentChanged()
	 */
	@Override
	public void onContentChanged() {
		super.onContentChanged();
		View emptyView = findViewById(android.R.id.empty);
		mList = (ExpandableListView)findViewById(android.R.id.list);
		if (mList == null) {
			throw new RuntimeException(
					"Your content must have a ExpandableListView whose id attribute is " +
			"'android.R.id.list'");
		}
		if (emptyView != null) {
			mList.setEmptyView(emptyView);
		}
		mList.setOnChildClickListener(this);
		mList.setOnGroupExpandListener(this);
		mList.setOnGroupCollapseListener(this);

		if (mFinishedStart) {
			setListAdapter(mAdapter);
		}
		mFinishedStart = true;
	}

	/**
	 * Provide the adapter for the expandable list.
	 */
	public void setListAdapter(ExpandableListAdapter adapter) {
		synchronized (this) {
			ensureList();
			mAdapter = adapter;
			mList.setAdapter(adapter);
		}
	}

	/**
	 * Get the activity's expandable list view widget.  This can be used to get the selection,
	 * set the selection, and many other useful functions.
	 * 
	 * @see ExpandableListView
	 */
	public ExpandableListView getExpandableListView() {
		ensureList();
		return mList;
	}

	/**
	 * Get the ExpandableListAdapter associated with this activity's
	 * ExpandableListView.
	 */
	public ExpandableListAdapter getExpandableListAdapter() {
		return mAdapter;
	}

	private void ensureList() {
		if (mList != null) {
			return;
		}
		//setContentView(android.R.layout.expandable_list_content);
		setContentView(R.layout.expandable_list_layout);
	}

	/**
	 * Gets the ID of the currently selected group or child.
	 * 
	 * @return The ID of the currently selected group or child.
	 */
	public long getSelectedId() {
		return mList.getSelectedId();
	}

	/**
	 * Gets the position (in packed position representation) of the currently
	 * selected group or child. Use
	 * {@link ExpandableListView#getPackedPositionType},
	 * {@link ExpandableListView#getPackedPositionGroup}, and
	 * {@link ExpandableListView#getPackedPositionChild} to unpack the returned
	 * packed position.
	 * 
	 * @return A packed position representation containing the currently
	 *         selected group or child's position and type.
	 */
	public long getSelectedPosition() {
		return mList.getSelectedPosition();
	}

	/**
	 * Sets the selection to the specified child. If the child is in a collapsed
	 * group, the group will only be expanded and child subsequently selected if
	 * shouldExpandGroup is set to true, otherwise the method will return false.
	 * 
	 * @param groupPosition The position of the group that contains the child.
	 * @param childPosition The position of the child within the group.
	 * @param shouldExpandGroup Whether the child's group should be expanded if
	 *            it is collapsed.
	 * @return Whether the selection was successfully set on the child.
	 */
	public boolean setSelectedChild(int groupPosition, int childPosition, boolean shouldExpandGroup) {
		return mList.setSelectedChild(groupPosition, childPosition, shouldExpandGroup);
	}

	/**
	 * Sets the selection to the specified group.
	 * @param groupPosition The position of the group that should be selected.
	 */
	public void setSelectedGroup(int groupPosition) {
		mList.setSelectedGroup(groupPosition);
	}

	/**
	 * A simple adapter which maintains an ArrayList of photo resource Ids. 
	 * Each photo is displayed as an image. This adapter supports clearing the
	 * list of photos and adding a new photo.
	 *
	 */
	public class MyExpandableListAdapter extends BaseExpandableListAdapter {
		// Sample data set.  children[i] contains the children (String[]) for groups[i].
		HashMap<String, SortedMap<String, EpicCommandInfoImpl>> mMapCommands = new HashMap<String, SortedMap<String, EpicCommandInfoImpl>>();
		ArrayList<String> mNodes = new ArrayList<String>();
		HashMap<String, Boolean>  mMapAvailability = new HashMap<String, Boolean>(); 

		public void addNode(String addr) {
			addCommand(addr, null);
		}

		public EpicCommandInfoImpl getChildEpicCommandInfo(int groupPosition, int childPosition) {
			String key = mNodes.get(groupPosition);
			SortedMap<String, EpicCommandInfoImpl> list = mMapCommands.get(key);
			if (list==null){
				return null;
			}
			Collection<EpicCommandInfoImpl> values = list.values();
			Iterator<EpicCommandInfoImpl> iterCommands = values.iterator();
			EpicCommandInfoImpl command = null;
			if(childPosition < values.size()){
				for(int i=0; i<childPosition+1; i++){
					command = iterCommands.next();
				}
			}
			return command;
		}

		public void changeAvailability(String node, boolean available) {
			mMapAvailability.put(node, available);
		}

		
		public void addCommand(String node, EpicCommandInfoImpl command) {
			if(node==null){
				return;
			}
			
			if(!mNodes.contains(node)) {
				mNodes.add(node);
			}

			SortedMap<String, EpicCommandInfoImpl> list = mMapCommands.get(node);
			if(list == null) {
				list = new TreeMap<String, EpicCommandInfoImpl>();
				mMapCommands.put(node, list);
			}

			if(command==null) {
				return;
			}
				
			if(!list.containsKey(command)){
				list.put(command.getEpicCommandId(),command);
			}
		}

		public Object getChild(int groupPosition, int childPosition) {
			SortedMap<String, EpicCommandInfoImpl> list = mMapCommands.get(mNodes.get(groupPosition));
			Collection<EpicCommandInfoImpl> values = list.values();
			EpicCommandInfoImpl info = null;
			if(childPosition < values.size()){
				Iterator<EpicCommandInfoImpl> iterValues = values.iterator();
				for(int i=0; i<childPosition+1; i++){
					info = iterValues.next();
				}
			}
			if(info == null) {
				return null;
			}
			return info.getHumanReadableName();
		}

		public long getChildId(int groupPosition, int childPosition) {
			return childPosition;
		}

		
		public int getChildrenCount(int groupPosition) {
			SortedMap<String, EpicCommandInfoImpl> list = mMapCommands.get(mNodes.get(groupPosition));
			if(list==null){
				return 0;
			}
			return list.size();
		}

		public TextView getGenericView() {
			// Layout parameters for the ExpandableListView
			AbsListView.LayoutParams lp = new AbsListView.LayoutParams(
					ViewGroup.LayoutParams.FILL_PARENT, 64);

			TextView textView = new TextView(ExploreEpicNetwork.this);
			textView.setLayoutParams(lp);
			// Center the text vertically
			textView.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
			// Set the text starting position
			textView.setPadding(64, 0, 0, 0);
			return textView;
		}

		public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
				View convertView, ViewGroup parent) {
			TextView textView = getGenericView();
			textView.setText(getChild(groupPosition, childPosition).toString());
			return textView;
		}

		public Object getGroup(int groupPosition) {
			return mNodes.get(groupPosition);
		}

		
		public int getGroupCount() {
			return mNodes.size();
		}

		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
				ViewGroup parent) {
			TextView textView = getGenericView();
			textView.setText(getGroup(groupPosition).toString());
			boolean availability = false;
			String node = mNodes.get(groupPosition);
			if(mMapAvailability.containsKey(node)) {
				availability = mMapAvailability.get(node);
			}
			if(availability){
				textView.setTextColor(Color.argb(255, 20, 200, 20));
			} else {
				textView.setTextColor(Color.argb(255, 100, 100, 100));
			}

			return textView;
		}

		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return true;
		}

		public boolean hasStableIds() {
			return true;
		}


	}


}
