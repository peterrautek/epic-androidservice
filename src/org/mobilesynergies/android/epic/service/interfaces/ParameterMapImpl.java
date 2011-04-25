package org.mobilesynergies.android.epic.service.interfaces;

import java.util.Map;

import org.mobilesynergies.epic.client.remoteui.ArrayParameter;
import org.mobilesynergies.epic.client.remoteui.Parameter;
import org.mobilesynergies.epic.client.remoteui.ParameterMap;


import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class ParameterMapImpl extends ParameterMap implements Parcelable {

	public ParameterMapImpl(ParameterMap otherMap) {
		super(otherMap);
	}

	
	/**
	 * A Parcelable.Creator making this class parcelable.
	 */
	public static final Parcelable.Creator<ParameterMapImpl> CREATOR = new Parcelable.Creator<ParameterMapImpl>() {
		public ParameterMapImpl createFromParcel(Parcel in) {
			return new ParameterMapImpl(in);
		}

		public ParameterMapImpl[] newArray(int size) {
			return new ParameterMapImpl[size];
		}
	};
	
	public ParameterMapImpl(){
		super();
	}
	
	/**
	 * Constructor required to make this class parcelable
	 * 
	 * @param in The parcel received for initialization
	 */
	private ParameterMapImpl(Parcel in) {
		readFromParcel(in);
	}

	
	@Override
	public int describeContents() {
		return 0;
	}

	
	
	 /**
     * Writes the Bundle contents to a Parcel, typically in order for
     * it to be passed through an IBinder connection.
     * @param parcel The parcel to copy this bundle to.
     */
    public void writeToParcel(Parcel parcel, int flags) {
       
            parcel.writeInt(-1); // dummy, will hold length
            parcel.writeInt(0x4C444E42); // 'B' 'N' 'D' 'L'

            int oldPos = parcel.dataPosition();
            Map<String, Parameter> map = getMap();
            parcel.writeMap(map);
            int newPos = parcel.dataPosition();

            // Backpatch length
            parcel.setDataPosition(oldPos - 8);
            int length = newPos - oldPos;
            parcel.writeInt(length);
            parcel.setDataPosition(newPos);
        
    }

    /**
     * Reads the Parcel contents into this Bundle, typically in order for
     * it to be passed through an IBinder connection.
     * @param parcel The parcel to overwrite this bundle from.
     */
    public void readFromParcel(Parcel parcel) {
        int length = parcel.readInt();
        if (length < 0) {
            throw new RuntimeException("Bad length in parcel: " + length);
        }
        readFromParcelInner(parcel, length);
    }

    void readFromParcelInner(Parcel parcel, int length) {
        int magic = parcel.readInt();
        if (magic != 0x4C444E42) {
            //noinspection ThrowableInstanceNeverThrown
            String st = Log.getStackTraceString(new RuntimeException());
            Log.e("Bundle", "readBundle: bad magic number");
            Log.e("Bundle", "readBundle: trace = " + st);
        }

        // Advance within this Parcel
        int offset = parcel.dataPosition();
        parcel.setDataPosition(offset + length);

        Parcel p = Parcel.obtain();
        p.setDataPosition(0);
        p.appendFrom(parcel, offset, length);
        p.setDataPosition(0);
    }

	


}
