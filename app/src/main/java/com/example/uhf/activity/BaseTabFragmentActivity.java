package com.example.uhf.activity;

import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import com.example.uhf.R;
import com.example.uhf.fragment.KeyDwonFragment;
import com.example.uhf.tools.UIHelper;
import com.rscja.deviceapi.RFIDWithUHFUART;
import com.rscja.utility.StringUtility;

/**
 * Created by Administrator on 2015-03-10.
 */
public class BaseTabFragmentActivity extends FragmentActivity {


	public RFIDWithUHFUART mReader;
	public KeyDwonFragment currentFragment=null;
	public int TidLen=6;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	public void initUHF() {
		try {
			mReader = RFIDWithUHFUART.getInstance();
		} catch (Exception ex) {

			toastMessage(ex.getMessage());

			return;
		}

		if (mReader != null) {
			new InitTask().execute();
		}
	}


	/**
	 * ����ActionBar
	 */
	/*@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
//		return super.onCreateOptionsMenu(menu);
	}


	public boolean onOptionsItemSelected(android.view.MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				break;
			case R.id.UHF_ver:
				getUHFVersion();
				break;
			default:
				break;
		}
		return true;
	}*/

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == 139 || keyCode == 280) {
			if (event.getRepeatCount() == 0) {
				if (currentFragment != null) {
					currentFragment.myOnKeyDwon();
				}
			}
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}


	public void toastMessage(String msg) {
		Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
	}


	public class InitTask extends AsyncTask<String, Integer, Boolean> {
		ProgressDialog mypDialog;

		@Override
		protected Boolean doInBackground(String... params) {
			// TODO Auto-generated method stub
			mReader.free();
			return mReader.init();
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			mypDialog.cancel();
			if (!result) {
				Toast.makeText(BaseTabFragmentActivity.this, "init fail", Toast.LENGTH_SHORT).show();
			}
		}

		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			super.onPreExecute();
			mypDialog = new ProgressDialog(BaseTabFragmentActivity.this);
			mypDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			mypDialog.setMessage("init...");
			mypDialog.setCanceledOnTouchOutside(false);
			mypDialog.show();
		}
	}


	public boolean vailHexInput(String str) {
		if (str == null || str.length() == 0) {
			return false;
		}
		if (str.length() % 2 == 0) {
			return StringUtility.isHexNumberRex(str);
		}
		return false;
	}

	public void getUHFVersion() {
		if(mReader!=null) {
			String rfidVer = mReader.getVersion();
			UIHelper.alert(this, R.string.action_uhf_ver,
					rfidVer, R.drawable.webtext);
		}
	}
	public  String getVerName(){
		try {
			return this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName;
		} catch (PackageManager.NameNotFoundException e) {

		}
		return "";
	}
}
