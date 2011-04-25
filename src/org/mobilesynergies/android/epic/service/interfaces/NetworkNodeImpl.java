package org.mobilesynergies.android.epic.service.interfaces;

import org.mobilesynergies.epic.client.Address;
import org.mobilesynergies.epic.client.NetworkNode;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 
 * @author Peter
 * Class that holds all relevant information about a epic network node. 
 *
 */
public class NetworkNodeImpl extends NetworkNode implements Parcelable {


	/**
	 * A Parcelable.Creator making this class parcelable.
	 */
	public static final Parcelable.Creator<NetworkNodeImpl> CREATOR = new Parcelable.Creator<NetworkNodeImpl>() {
		public NetworkNodeImpl createFromParcel(Parcel in) {
			return new NetworkNodeImpl(in);
		}

		public NetworkNodeImpl[] newArray(int size) {
			return new NetworkNodeImpl[size];
		}
	};

	/**
	 * Constructor required to make this class parcelable
	 * 
	 * @param in The parcel received for initialization
	 */
	private NetworkNodeImpl(Parcel in) {

		readFromParcel(in);
	}



	public NetworkNodeImpl(NetworkNode networkNode) {
		super(networkNode);
	}



	/**
	 * Writing to a parcel
	 */
	public void writeToParcel(Parcel out, int flags){
		out.writeString(mAddress.getName());
		out.writeString(mAddress.getServer());
		out.writeString(mAddress.getResource());
		out.writeString(mAvailability);
		out.writeString(mSubscription);
	}

	/**
	 * Reading from a parcel
	 */
	public void readFromParcel(Parcel in) {
		String name =  in.readString();
		String server =  in.readString();
		String resource =  in.readString();
		mAddress = new Address(name, server, resource);
		mAvailability = in.readString();
		mSubscription = in.readString();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	/**
	 * Converts the contents to a humanreadable string
	 * 
	 * @return A human readable string that contains all relevant information about this node  
	 */
	public String toString() {
		return mAddress.getFullAddressString() + " ("+mSubscription+")"+": " + mAvailability;
	}



	public static NetworkNodeImpl[] asNetworkNodesImplArray(NetworkNode[] nodes) {
		if(nodes==null){
			return null;
		}
		if(nodes.length==0)
		{
			return new NetworkNodeImpl[0];
		}
		NetworkNodeImpl[] array = new NetworkNodeImpl[nodes.length]; 
		for(int i=0; i<nodes.length; i++) {
			array[i] = new NetworkNodeImpl(nodes[i]);
		}
		return array;
	}


}
