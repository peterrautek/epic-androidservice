package org.mobilesynergies.android.epic.service.interfaces;

import org.mobilesynergies.epic.client.remoteui.EpicCommandInfo;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Provides information about an EpicCommand 
 * @author Peter
 *
 */
public class EpicCommandInfoImpl extends EpicCommandInfo implements Parcelable, Comparable<String>{

	
	/**
	 * A Parcelable.Creator making this class parcelable.
	 */
	public static final Parcelable.Creator<EpicCommandInfoImpl> CREATOR = new Parcelable.Creator<EpicCommandInfoImpl>() {
		public EpicCommandInfoImpl createFromParcel(Parcel in) {
			return new EpicCommandInfoImpl(in);
		}

		public EpicCommandInfoImpl[] newArray(int size) {
			return new EpicCommandInfoImpl[size];
		}
	};
	
	public EpicCommandInfoImpl(){
		super();
	}
	
	public EpicCommandInfoImpl(EpicCommandInfo command) {
		super(command.getEpicNodeId(), command.getEpicCommandId(), command.getHumanReadableName());
	}
	
	public static EpicCommandInfoImpl[] asEpicCommandArray(EpicCommandInfo[] epicCommands) {
		if(epicCommands==null){
			return null;
		}
		EpicCommandInfoImpl[] commands = new EpicCommandInfoImpl[epicCommands.length];
		for(int i=0; i<commands.length; i++){
			commands[i] = new EpicCommandInfoImpl(epicCommands[i]);
		}
		return commands;
		
	}
	
	/**
	 * Constructor required to make this class parcelable
	 * 
	 * @param in The parcel received for initialization
	 */
	private EpicCommandInfoImpl(Parcel in) {
		readFromParcel(in);
	}

	
	@Override
	public int describeContents() {
		return 0;
	}

	 /**
     * Writes the contents to a Parcel, typically in order for
     * it to be passed through an IBinder connection.
     * @param parcel The parcel to copy this bundle to.
     */
    public void writeToParcel(Parcel parcel, int flags) {
    	parcel.writeString(getEpicNodeId());
    	parcel.writeString(getEpicCommandId());
    	parcel.writeString(getHumanReadableName());
    }

    /**
     * Reads the Parcel contents into this Bundle, typically in order for
     * it to be passed through an IBinder connection.
     * @param parcel The parcel to overwrite this bundle from.
     */
    public void readFromParcel(Parcel parcel) {
    	String nodeId = parcel.readString();
    	String commandId = parcel.readString();
    	String humanReadableName = parcel.readString();
    	setEpicNodeId(nodeId);
    	setEpicCommandId(commandId);
    	setHumanReadableName(humanReadableName);
    }

    /**
     * Two commands are equal if their id is equal.
     */
	@Override
	public int compareTo(String another) {
		String id = getEpicCommandId();
		return id.compareTo(another);
	}

	



}
