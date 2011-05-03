package org.mobilesynergies.android.epic.service.remoteui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.mobilesynergies.epic.client.remoteui.BooleanParameter;
import org.mobilesynergies.epic.client.remoteui.FloatParameter;
import org.mobilesynergies.epic.client.remoteui.IntParameter;
import org.mobilesynergies.epic.client.remoteui.OptionParameter;
import org.mobilesynergies.epic.client.remoteui.Parameter;
import org.mobilesynergies.epic.client.remoteui.ParameterMap;
import org.mobilesynergies.epic.client.remoteui.StringParameter;

import android.content.Context;
//import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView.OnEditorActionListener;


/**
 * The class UIGenerator gets a ParameterMap as input and transforms it to a user interface that is displayed by an Activity. 
 * It also constructs a ParameterMap reading out the values on any submit action triggered by the user. 
 * Further it can handle partial updates to the ui when it receives (partial) updates from the server.
 * 
 * @author rautek
 */
public class UiGenerator {

	private static final int DEFAULT_MIN = 0;
	private static final int DEFAULT_MAX = 100;

	private ParameterMap mMap = null;
	private View mView = null;
	private HashMap<String, Integer> mKeyIdMap = new HashMap<String, Integer>();
	private HashMap<String, Integer> mKeyIntegerRangeMap = new HashMap<String, Integer>();
	private HashMap<String, Float> mKeyFloatRangeMap = new HashMap<String, Float>();
	private SubmitActionListener mSubmitActionListener = null;

	
	public void updateUi(ParameterMap parameterMap) throws Exception {
		//while updating we disable the submitaction listener
		SubmitActionListener tempListener = mSubmitActionListener;
		mSubmitActionListener = null;
		
		Set<String> set = parameterMap.keySet();
		Iterator<String> iterSet = set.iterator();
		while(iterSet.hasNext()){
			String key = iterSet.next();
			Parameter  p = parameterMap.get(key);
			mMap.remove(key);
			mMap.putParameter(key, p);
			int id = mKeyIdMap.get(key);
			if(p.getType().equalsIgnoreCase(Parameter.TYPENAME_BOOLEAN)){
				BooleanParameter param = (BooleanParameter) p;
				CheckBox cb = (CheckBox) mView.findViewById(id);
				if(cb==null){
					throw new Exception("The CheckBox view was not found!");
				} else {
					cb.setChecked(param.getValue());
				}
			} else if(p.getType().equalsIgnoreCase(Parameter.TYPENAME_STRING)){
				StringParameter param = (StringParameter) p;
				EditText et = (EditText) mView.findViewById(id);
				if(et==null){
					throw new Exception("The EditText view was not found!");
				} else {
					et.setText(param.getValue());
				}
			} else if(p.getType().equalsIgnoreCase(Parameter.TYPENAME_FLOAT)){
				FloatParameter param = (FloatParameter) p;
				SeekBar sb = (SeekBar) mView.findViewById(id);
				if(sb == null){
					throw new Exception("The SeekBar view was not found!");
				} else {
					int progressbarWidth = sb.getMax();

					float fValue = param.getValue();
					float fMin = Math.min(fValue, DEFAULT_MIN);
					float fMax = Math.max(fValue, DEFAULT_MAX);
					Float definedMin = param.getMin();
					Float definedMax = param.getMax();
					if(definedMin!=null){
						fMin = definedMin; 
					}
					if(definedMax!=null){
						fMax = definedMax; 
					}
					mKeyFloatRangeMap.remove(key);
					mKeyFloatRangeMap.put(key, fMax-fMin);
					int progress = (int) ((fValue - fMin) / (fMax - fMin) * (float) progressbarWidth) ;
					sb.setProgress(progress);
				}

			} else if(p.getType().equalsIgnoreCase(Parameter.TYPENAME_INT)){
				IntParameter param = (IntParameter) p;
				SeekBar sb = (SeekBar) mView.findViewById(id);
				if(sb ==null){
					throw new Exception("The SeekBar view was not found!");
				} else {
					int progressbarWidth = sb.getMax();


					int iValue = param.getValue();
					int iMin = Math.min(iValue, DEFAULT_MIN);
					int iMax = Math.max(iValue, DEFAULT_MAX);
					Integer definedMin = param.getMin();
					Integer definedMax = param.getMax();
					if(definedMin!=null){
						iMin = definedMin; 
					}
					if(definedMax!=null){
						iMax = definedMax; 
					}
					
					int progress =  (int) ( (float) (iValue - iMin) / (float)(iMax - iMin) * (float) progressbarWidth);
					sb.setProgress(progress);
					
					mKeyIntegerRangeMap.remove(key);
					mKeyIntegerRangeMap.put(key, iMax-iMin);
					
				}

			} else if(p.getType().equalsIgnoreCase(Parameter.TYPENAME_OPTION)){
				
				OptionParameter param = (OptionParameter) p;
				Spinner spinner = (Spinner) mView.findViewById(id);
				
				if(spinner==null){
					throw new Exception("The Spinner view was not found!");
				}
				
				
				
				ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinner.getAdapter();
				ArrayList<Parameter> options = param.getOptions();
				if(options!=null){
					adapter.clear();
					Iterator<Parameter> iterOptions = options.iterator();
					while(iterOptions.hasNext()){
						Parameter paramOption = iterOptions.next();
						String optionValue = paramOption.getValueAsString(0);
						if(optionValue!=null){
							adapter.add(optionValue);
						}
					}
					adapter.notifyDataSetChanged();
				}
				int iSelectionPos = adapter.getPosition(p.getValueAsString(0));
				if(iSelectionPos>0){
					spinner.setSelection(iSelectionPos);
				}
			}
		}
		
		//after updating we enable the submitActionListener again
		mSubmitActionListener = tempListener;
	};
	
	
	/**
	 * Initializes the user interface from a given ParameterMap
	 * @param c The context used to initialize new views.
	 * @param parameterMap The ParameterMap that holds all parameters that need to be displayed to the user.
	 * @return A LinearLayout that contains a ScrollView with all necessary views. 
	 */
	public View initializeUi(Context c, ParameterMap parameterMap) {
		
		mMap = parameterMap;
		mKeyIdMap.clear();
		mKeyIntegerRangeMap.clear();
		mKeyFloatRangeMap.clear();
		
		
		LinearLayout linearLayout = new LinearLayout(c);
		linearLayout.setOrientation(LinearLayout.VERTICAL);
		ScrollView scrollView = new ScrollView(c);
		scrollView.addView(linearLayout);

		Set<String> set = parameterMap.keySet();
		Iterator<String> iterSet = set.iterator();
		while(iterSet.hasNext()){
			String key = iterSet.next();
			Parameter p = parameterMap.get(key);
			int id = mKeyIdMap.size();
			mKeyIdMap.put(key, id);
			if(p.getType().equalsIgnoreCase(Parameter.TYPENAME_BOOLEAN)){
				BooleanParameter param = (BooleanParameter) p;
				CheckBox cb = new CheckBox(c);
				cb.setId(id);
				cb.setChecked(param.getValue());
				cb.setText(param.getLabel());
				
				if(param.getSubmitActionHint()){
					OnCheckedChangeListener listener = new MyOnCheckedChangeListener(key);
					cb.setOnCheckedChangeListener(listener);
				}
				
				linearLayout.addView(cb);
			} else if(p.getType().equalsIgnoreCase(Parameter.TYPENAME_STRING)){
				StringParameter param = (StringParameter) p;
				String label = param.getLabel();
				if((label!=null)&&(label.length()>1)){
					TextView tv = new TextView(c);
					tv.setText(label);
					linearLayout.addView(tv);
				}

				EditText et = new EditText(c);
				et.setId(id);
				et.setText(param.getValue());
				
				if(param.getSubmitActionHint()){
					OnEditorActionListener listener = new MyOnEditorActionListener(key);
					et.setOnEditorActionListener(listener);
				}
				
				linearLayout.addView(et);
			}  else if(p.getType().equalsIgnoreCase(Parameter.TYPENAME_OPTION)){
				OptionParameter param = (OptionParameter) p;
				String label = param.getLabel();
				if((label!=null)&&(label.length()>1)){
					TextView tv = new TextView(c);
					tv.setText(label);
					linearLayout.addView(tv);
				}
				Spinner spinner = new Spinner(c);
				spinner.setId(id);

				ArrayList<Parameter> options = param.getOptions();
				Iterator<Parameter> iterOptions = options.iterator();

				ArrayAdapter<String> adapter = new ArrayAdapter<String>(c, android.R.layout.simple_spinner_item);


				while(iterOptions.hasNext()){
					Parameter paramOption = iterOptions.next();
					String optionValue = paramOption.getValueAsString(0);
					if(optionValue!=null){
						adapter.add(optionValue);
					}
				}


				adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
				
				if(param.getSubmitActionHint()){
					OnItemSelectedListener listener = new MyOnItemSelectedListener(key);
					spinner.setOnItemSelectedListener(listener);
				}
				
				spinner.setAdapter(adapter);

				String value = param.getValueAsString(0);
				int iSelectionPos = adapter.getPosition(value);
				if(iSelectionPos>0){
					spinner.setSelection(iSelectionPos);
				}


				linearLayout.addView(spinner);
			} else if(p.getType().equalsIgnoreCase(Parameter.TYPENAME_FLOAT)){
				FloatParameter param = (FloatParameter) p;
				String label = param.getLabel();
				if((label!=null)&&(label.length()>1)){
					TextView tv = new TextView(c);
					tv.setText(label);
					linearLayout.addView(tv);
				}

				SeekBar s = new SeekBar(c);
				s.setMax(1000);
				s.setId(id);
				int progressbarWidth = s.getMax();

				float fValue = param.getValue();
				float fMin = Math.min(fValue, DEFAULT_MIN);
				float fMax = Math.max(fValue, DEFAULT_MAX);
				Float definedMin = param.getMin();
				Float definedMax = param.getMax();
				if(definedMin!=null){
					fMin = definedMin; 
				}
				if(definedMax!=null){
					fMax = definedMax; 
				}
				mKeyFloatRangeMap.put(key, fMax-fMin);
				int progress = (int) ((fValue - fMin) / (fMax - fMin) * (float) progressbarWidth) ;
				s.setProgress(progress);
				linearLayout.addView(s);
			} else if(p.getType().equalsIgnoreCase(Parameter.TYPENAME_INT)){

				IntParameter param = (IntParameter) p;
				String label = param.getLabel();
				if((label!=null)&&(label.length()>1)){
					TextView tv = new TextView(c);
					tv.setText(label);
					linearLayout.addView(tv);
				}
				SeekBar s = new SeekBar(c);
				s.setMax(1000);
				s.setId(id);
				int progressbarWidth = s.getMax();


				int iValue = param.getValue();
				int iMin = Math.min(iValue, DEFAULT_MIN);
				int iMax = Math.max(iValue, DEFAULT_MAX);
				Integer definedMin = param.getMin();
				Integer definedMax = param.getMax();
				if(definedMin!=null){
					iMin = definedMin; 
				}
				if(definedMax!=null){
					iMax = definedMax; 
				}
				mKeyIntegerRangeMap.put(key, iMax-iMin);
				int progress =  (int) ( (float) (iValue - iMin) / (float)(iMax - iMin) * (float) progressbarWidth);
				s.setProgress(progress);
				
				if(param.getSubmitActionHint()){
					MyOnSeekBarChangeListener listener = new MyOnSeekBarChangeListener(key);
					s.setOnSeekBarChangeListener(listener);
				}
				
				

				
				linearLayout.addView(s);
			}
		}

		
		LinearLayout ll = new LinearLayout(c);
		ll.setOrientation(LinearLayout.VERTICAL);
		ll.addView(scrollView);
		mView = ll;
		return ll;
	}

	/**
	 * Retrieves the value of one parameter from the user interface.
	 * @param key The key of the parameter. 
	 * @return The Parameter associated with the given key. The parameter is of correct type. Null is returned if no view was initialized 
	 * @throws Exception If the parameter was not found an exception is thrown.
	 */
	public Parameter getValue(String key) throws Exception {
		if((mMap==null)||(mView == null))
			return null;

		Parameter p = mMap.get(key);
		int id = mKeyIdMap.get(key);
		if(p.getType().equalsIgnoreCase(Parameter.TYPENAME_BOOLEAN)){
			BooleanParameter param = (BooleanParameter) p;
			CheckBox cb = (CheckBox) mView.findViewById(id);
			if(cb==null){
				throw new Exception("The CheckBox view was not found!");
			} else {
				param.setValue(cb.isChecked());
			}


		} else if(p.getType().equalsIgnoreCase(Parameter.TYPENAME_STRING)){
			StringParameter param = (StringParameter) p;
			EditText et = (EditText) mView.findViewById(id);
			if(et==null){
				throw new Exception("The EditText view was not found!");
			} else {
				param.setValue(et.getText().toString());
			}
		} else if(p.getType().equalsIgnoreCase(Parameter.TYPENAME_FLOAT)){
			FloatParameter param = (FloatParameter) p;
			SeekBar sb = (SeekBar) mView.findViewById(id);
			if(sb == null){
				throw new Exception("The SeekBar view was not found!");
			} else {
				float fPercent = (float)sb.getProgress() / (float)sb.getMax();
				float fMin = DEFAULT_MIN;
				Float fDeclaredMin = param.getMin();  
				if(fDeclaredMin!=null){
					fMin=fDeclaredMin;
				}
				float fRange = mKeyFloatRangeMap.get(key);
				float value = fPercent * fRange;
				param.setValue(value + fMin);
			}

		} else if(p.getType().equalsIgnoreCase(Parameter.TYPENAME_INT)){
			IntParameter param = (IntParameter) p;
			SeekBar sb = (SeekBar) mView.findViewById(id);
			if(sb ==null){
				throw new Exception("The SeekBar view was not found!");
			} else {
				float fPercent = (float)sb.getProgress() / (float)sb.getMax();
				int iMin = DEFAULT_MIN;

				Integer iDeclaredMin = param.getMin();  

				if(iDeclaredMin!=null){
					iMin=iDeclaredMin;
				}

				int iRange = mKeyIntegerRangeMap.get(key);
				int value = (int) (fPercent * (float) iRange);
				param.setValue(value + iMin);
			}

		} else if(p.getType().equalsIgnoreCase(Parameter.TYPENAME_OPTION)){
			OptionParameter param = (OptionParameter) p;
			Spinner spinner = (Spinner) mView.findViewById(id);
			if(spinner==null){
				throw new Exception("The Spinner view was not found!");
			}
			TextView tv = (TextView)spinner.getSelectedView();
			String s = tv.getText().toString();
			param.setValue(s);
		}
		return p;
	}


	/**
	 * Retrieves values for all Parameters from the current user interface.
	 * @return A ParameterMap containing all Parameters, or null if no view was initialized
	 * @throws Exception If a was initialized but could not be retrieved from the view an exception is thrown.
	 */
	public ParameterMap getValues() throws Exception {
		if((mMap==null)||(mView == null))
			return null;

		ParameterMap resultMap = new ParameterMap();
		Set<String> set = mMap.keySet();
		Iterator<String> iterKeys = set.iterator();
		
		while(iterKeys.hasNext()){
			String key = iterKeys.next();
			Parameter parameter = getValue(key);
			resultMap.putParameter(key, parameter);
		}
		return resultMap;
	}

	/**
	 * A SubmitActionListener is called when a Parameter (that has the submit action hint set to true) was changed by the user. 
	 * @param submitActionListener The SubmitActionListener to be called on subsequent submit actions. 
	 */
	public void setSubmitActionListener(SubmitActionListener submitActionListener) {
		mSubmitActionListener  = submitActionListener;
	}
	

	
	
	private class MyOnCheckedChangeListener implements OnCheckedChangeListener{
		
		private String mKey = ""; 
		
		public MyOnCheckedChangeListener(String key){
			super();
			mKey = key;
		}
		
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			if(mSubmitActionListener!=null){
				mSubmitActionListener.onSubmitAction(mKey);
			}
		}
	};
	
	
	
	
	private class MyOnItemSelectedListener implements OnItemSelectedListener {
		private String mKey = "";
		
		public MyOnItemSelectedListener(String key){
			super();
			mKey = key;
		}

		@Override
		public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
				long arg3) {
			if(mSubmitActionListener!=null){
				mSubmitActionListener.onSubmitAction(mKey);
			}
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) {
			// TODO Auto-generated method stub
			
		}
	}
	
	private class MyOnEditorActionListener implements OnEditorActionListener {
		
		
		private String mKey = "";
		
		public MyOnEditorActionListener(String key){
			super();
			mKey = key;
		}

		@Override
		public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
			if(mSubmitActionListener!=null){
				mSubmitActionListener.onSubmitAction(mKey);
			}
			return true;
		}
	}
		
	private class MyOnSeekBarChangeListener implements OnSeekBarChangeListener {
		
		
		private String mKey = "";
		
		public MyOnSeekBarChangeListener(String key){
			super();
			mKey = key;
		}
		
		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			if(mSubmitActionListener!=null){
				mSubmitActionListener.onSubmitAction(mKey);
			}
		}
		
		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onProgressChanged(SeekBar seekBar,
				int progress, boolean fromUser) {
			// TODO Auto-generated method stub
			
		}
	}

	


}
