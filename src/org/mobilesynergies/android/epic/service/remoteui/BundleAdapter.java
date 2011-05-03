package org.mobilesynergies.android.epic.service.remoteui;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.mobilesynergies.epic.client.remoteui.BooleanParameter;
import org.mobilesynergies.epic.client.remoteui.FloatParameter;
import org.mobilesynergies.epic.client.remoteui.IntParameter;
import org.mobilesynergies.epic.client.remoteui.Parameter;
import org.mobilesynergies.epic.client.remoteui.ParameterMap;
import org.mobilesynergies.epic.client.remoteui.StringParameter;

import android.os.Bundle;
import android.util.Log;

/** 
 * Converts a Parameter to a bundle.
 * ParameterMaps are converted to Bundles, all other Parameters are directly put into the Bundle
 */
public class BundleAdapter {
	
	private static final String CLASS_TAG = "BundleAdapter";

	public static ParameterMap makeParameterMap(Bundle bundle){
		ParameterMap map = makeParameterMapFromBundle(bundle);
		return map;
	}
	
	private static ParameterMap makeParameterMapFromBundle(Bundle bundle) {
		ParameterMap parameterMap = new ParameterMap();
		Set<String> keys = bundle.keySet();
		Iterator<String> iterKeys = keys.iterator();
		while(iterKeys.hasNext()){
			String key = iterKeys.next();
			Object o = bundle.get(key);
			if(o.getClass().equals(BundleClass)){
				Bundle value = (Bundle) o;
				ParameterMap inner = makeParameterMapFromBundle(value);
				parameterMap.putMap(key, inner);
			} else if(o.getClass().equals(IntegerClass)){
				Integer value = (Integer) o;
				parameterMap.putInt(key, value);
			} else if(o.getClass().equals(StringClass)){
				String value = (String) o;
				parameterMap.putString(key, value);
			} else if(o.getClass().equals(FloatClass)){
				Float value = (Float) o;
				parameterMap.putFloat(key, value);
			} else if(o.getClass().equals(BooleanClass)){
				Boolean value = (Boolean) o;
				parameterMap.putBoolean(key, value);
			} else {
				Log.d(CLASS_TAG, "Unsupported class: " + o.getClass().getName());
			}
		}
		return parameterMap;
	}
	
	static Class<? extends Integer> IntegerClass = new Integer(0).getClass();
	static Class<? extends String> StringClass = new String("").getClass();
	static Class<? extends Float> FloatClass = new Float(0).getClass();
	//static Class<? extends Double> DoubleClass = new Double(0).getClass();
	static Class<? extends Boolean> BooleanClass = new Boolean(false).getClass();
	static Class<? extends Bundle> BundleClass = new Bundle().getClass();

	public static Bundle makeBundle(ParameterMap map){
		Bundle bundle = makeBundleFromParameterMap(map);
		return bundle;
	}

	private static void addToBundle(Bundle bundle, String name, Parameter param) {
		String type = param.getType();
		if(type.equalsIgnoreCase(Parameter.TYPENAME_INT)){
			int val = ((IntParameter) param).getValue();
			bundle.putInt(name, val);
		} else if(type.equalsIgnoreCase(Parameter.TYPENAME_MAP)){
			Bundle b2 = makeBundleFromParameterMap((ParameterMap)param);
			bundle.putBundle(name, b2);
		} else if(type.equalsIgnoreCase(Parameter.TYPENAME_ARRAY)){
			//currently we do not support arrays!
			//there is no support in the Bundle class to put arbitrary arrays
			//the putParcelableArray could work but is not implemented yet
			Log.w(CLASS_TAG, "Ignored parameter of type array");
		} else if(type.equalsIgnoreCase(Parameter.TYPENAME_BOOLEAN)){
			boolean val = ((BooleanParameter) param).getValue();
			bundle.putBoolean(name, val);
		} else if(type.equalsIgnoreCase(Parameter.TYPENAME_FLOAT)){
			float val = ((FloatParameter) param).getValue();
			bundle.putFloat(name, val);
		} else if(type.equalsIgnoreCase(Parameter.TYPENAME_STRING)){
			String val = ((StringParameter) param).getValue();
			bundle.putString(name, val);
		} else {
			Log.w(CLASS_TAG, "Ignored parameter of type " + param.getType());
		}
	}

	private static Bundle makeBundleFromParameterMap(ParameterMap param) {
		Bundle b = new Bundle();
		Map<String, Parameter> map = param.getMap();
		Set<String> keys = map.keySet();
		Iterator<String> iterKeys = keys.iterator();
		while(iterKeys.hasNext()){
			String key = iterKeys.next();
			Parameter innerParam = map.get(key);
			addToBundle(b, key, innerParam);
		}
		return b;
	}
	
	
	
	/*
	 private Bundle makeBundleFromArray(ArrayParameter param) {
		Bundle b = new Bundle();
		ArrayList<Parameter> array = param.getArray();
		Iterator<Parameter> iter = array.iterator();
		while(iter.hasNext()){
			Parameter innerParam = iter.next();
			addToBundle(b, , innerParam);
			
		}
		return b;
	}*/
	
	
	
}