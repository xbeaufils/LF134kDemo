package com.handheld.lf134kdemo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import cn.pda.serialport.SerialPort;
import cn.pda.serialport.Tools;

import com.handheld.LF134K.Lf134KManager;
import com.handheld.LF134K.Lf134kDataModel;
public class LF134KActivity extends Activity {

	private Button buttonStartRead;
	private Button buttonClear;
	private Button buttonExit;
	private ListView listView;
	private List<Tagid> listTag;
	private List<Map<String, String>> listMap;
	private Lf134KManager manager;
	private ReadThread mReadThread;
	//Receive data and update UI by handler
	private Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			if (msg.what == Lf134KManager.LF) {
				Bundle bundle = msg.getData();
				String data = bundle.getString("id");
				String nation = bundle.getString("nation");
				String type = bundle.getString("type");
				int datalent = data.length();
				int nationlent = nation.length();
				for (int i = 0; i < 12-datalent; i++) {
					data = "0"+data;
				}
				for (int j = 0; j < 3-nationlent; j++) {
					nation = "0"+nation;
				}
//				Log.e("ID:", nation+":"+data);
				updateUI(data,nation,type);
				Util.play(1, 0);
			}
		};
	};
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_lf);
		manager = new Lf134KManager();
		initView();
		//new list to save tag data
		listTag = new ArrayList<Tagid>();
		//init sound pool
		Util.initSoundPool(this);
		//start thread
		mReadThread = new ReadThread();
		mReadThread.start();

		if(!startFlag){
			buttonStartRead.setText(R.string.stop);
//					manager.Start();
			startFlag = true;
		}else{
			buttonStartRead.setText(R.string.start);
//					manager.Pause();
			startFlag = false;
		}
	}
	private void ConfigInfo(){
		String powerString = "";
		switch (Lf134KManager.Power) {
		case SerialPort.Power_3v3:
			powerString = "power_3V3";
			break;
		case SerialPort.Power_5v:
			powerString = "power_5V";
			break;
		case SerialPort.Power_Scaner:
			powerString = "scan_power";
			break;
		case SerialPort.Power_Psam:
			powerString = "psam_power";
			break;
		case SerialPort.Power_Rfid:
			powerString = "rfid_power";
			break;
		default:
			break;
		}
		((TextView) findViewById(R.id.textview_title_config)).setText("Port:com"+Lf134KManager.Port+";Power:"+powerString);
	}
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		ConfigInfo();
	}
//
	private void initView() {
		buttonStartRead = (Button) findViewById(R.id.button_startRead_lf);
		buttonClear = (Button) findViewById(R.id.button_clear_lf);
		buttonExit = (Button) findViewById(R.id.button_exit_lf);
		listView = (ListView) findViewById(R.id.listView1_lf);
		buttonStartRead.setOnClickListener(new MyonClick());
		buttonClear.setOnClickListener(new MyonClick());
		buttonExit.setOnClickListener(new MyonClick());
	}
	private boolean startFlag = false;
	//click listener
	private class MyonClick implements OnClickListener{
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			//start read
			case R.id.button_startRead_lf:
				if(!startFlag){
					buttonStartRead.setText(R.string.stop);
//					manager.Start();
					startFlag = true;
				}else{
					buttonStartRead.setText(R.string.start);
//					manager.Pause();
					startFlag = false;
				}
				
				break;
				//clear
			case R.id.button_clear_lf:
				listTag.removeAll(listTag);
				listView.setAdapter(null);
				break;
			case R.id.button_exit_lf:
				finish();
				break;
			default:
				break;
			}
		}
	}
	
	//add to list
	private List<Tagid> addToList(List<Tagid> list, String tagId,String nation,String type){
		Tagid tag = new Tagid();
		tag.id = tagId;
		tag.nation = nation;
		tag.type = type;
		int temp = 1;
		if(list == null || list.size() == 0){  //add this id for the first time
			tag.count = temp;
			list.add(tag);
			return list;
		}
		//the list contain this epc 
		for(int i = 0; i < list.size(); i++){  //list
			if(tagId.equals(list.get(i).id)){ 
				temp = list.get(i).count + temp;
				tag.count = temp;
				for(int j = i; j > 0 ; j--){
					list.set(j, list.get(j-1));  
				}
				list.set(0, tag);
				return list;
			}
		}
		//
		Tagid lastTagid = list.get(list.size() - 1);  //
		for(int j = list.size() - 1; j >= 0 ; j--){
			if(j == 0){
				tag.count = temp; 
				list.set(j, tag);   //
			}else{
				list.set(j, list.get(j - 1));
			}
		}
		list.add(lastTagid);
		
		return list;
	}
	
	//update UI
	private void updateUI(final String tagId,final String nation,final String type){
		
	runOnUiThread(new Runnable() {
		@Override
		public void run() {
			List<Tagid> listst = addToList(listTag, tagId,nation,type);
			listMap = new ArrayList<Map<String,String>>();
			int num = 1;
				for (Tagid tag : listst) {
					Map<String, String> map = new HashMap<String, String>();
					map.put("num", num + "");
					map.put("id", tag.id+"");
					map.put("nation", tag.nation+"");
					map.put("type", tag.type+"");
					map.put("count", tag.count + "");
					listMap.add(map);
					num++;
				}
				
			ListAdapter adapter = new SimpleAdapter(LF134KActivity.this, listMap, R.layout.listview_item,
					new String[] { "num","nation", "id","type", "count", }, new int[] {
							R.id.textView_list_item_num,
							R.id.textView_list_item_nation,
							R.id.textView_list_item_id,
							R.id.textView_list_item_type,
							R.id.textView_list_item_count });
			listView.setAdapter(adapter);
//			Util.play(1, 0);
			}
		});
	}
	@Override
	protected void onDestroy() {
		startFlag = false;
		runFlag = false;
		manager.Close();
		super.onDestroy();
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);
	}
	private String what = "lfhdx";
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		//Go to SettingActivity
		
		return super.onOptionsItemSelected(item);
	}
	
	private boolean runFlag = true;
	private class ReadThread extends Thread {
		@Override
		public void run() {
			super.run();
			while(runFlag) {
				if (startFlag) {
					Lf134kDataModel model = manager.GetData(500);
					if (model!=null) {
						sendMSG(Tools.BytesToLong(model.ID)+"",Tools.BytesToLong(model.Country)+"",model.Type);
					}
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
		
		private void sendMSG(String id,String nation,String type) {
			// TODO Auto-generated method stub
			Bundle bundle = new Bundle();
			bundle.putString("id", id);
			bundle.putString("nation", nation);
			bundle.putString("type", type);
			Message msg = new Message();
			msg.what = 	Lf134KManager.LF;
			msg.setData(bundle);
			mHandler.sendMessage(msg);
		}
	}
}
