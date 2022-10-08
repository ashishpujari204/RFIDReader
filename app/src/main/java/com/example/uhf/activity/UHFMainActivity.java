package com.example.uhf.activity;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTabHost;

import com.example.uhf.R;
import com.example.uhf.UhfInfo;
import com.example.uhf.fragment.BlockPermalockFragment;
import com.example.uhf.fragment.BlockWriteFragment;
import com.example.uhf.fragment.UHFKillFragment;
import com.example.uhf.fragment.UHFLocationFragment;
import com.example.uhf.fragment.UHFLockFragment;
import com.example.uhf.fragment.UHFReadFragment;
import com.example.uhf.fragment.UHFReadTagFragment;
import com.example.uhf.fragment.UHFSetFragment;
import com.example.uhf.fragment.UHFWriteFragment;

import java.util.ArrayList;
import java.util.HashMap;

public class UHFMainActivity extends BaseTabFragmentActivity {

    private FragmentTabHost mTabHost;
    private FragmentManager fm;
    public UhfInfo uhfInfo=new UhfInfo();
    public ArrayList<HashMap<String, String>> tagList = new ArrayList<HashMap<String, String>>();;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkReadWritePermission();
        //setTitle(String.format(getString(R.string.app_name) + "(v%s)", getVerName()));
        setTitle(getString(R.string.app_name));
        //setTitle("Eminent");
        initSound();
        initUHF();
        initViewPageData();

    }

    protected void initViewPageData() {

        fm = getSupportFragmentManager();
        mTabHost = findViewById(android.R.id.tabhost);
        mTabHost.setup(this, fm, R.id.realtabcontent);
        mTabHost.addTab(mTabHost.newTabSpec(getString(R.string.uhf_msg_tab_scan)).setIndicator(getString(R.string.uhf_msg_tab_scan)),
                UHFReadTagFragment.class, null);

        mTabHost.addTab(mTabHost.newTabSpec(getString(R.string.location)).setIndicator(getString(R.string.location)),
                UHFLocationFragment.class, null);

        mTabHost.addTab(mTabHost.newTabSpec(getString(R.string.uhf_msg_tab_read)).setIndicator(getString(R.string.uhf_msg_tab_read)),
                UHFReadFragment.class, null);

        mTabHost.addTab(mTabHost.newTabSpec(getString(R.string.uhf_msg_tab_write)).setIndicator(getString(R.string.uhf_msg_tab_write)),
                UHFWriteFragment.class, null);

        mTabHost.addTab(mTabHost.newTabSpec(getString(R.string.uhf_msg_tab_set)).setIndicator(getString(R.string.uhf_msg_tab_set)),
                UHFSetFragment.class, null);

        mTabHost.addTab(mTabHost.newTabSpec(getString(R.string.uhf_msg_tab_lock)).setIndicator(getString(R.string.uhf_msg_tab_lock)),
                UHFLockFragment.class, null);

        mTabHost.addTab(mTabHost.newTabSpec(getString(R.string.uhf_msg_tab_kill)).setIndicator(getString(R.string.uhf_msg_tab_kill)),
                UHFKillFragment.class, null);

        mTabHost.addTab(mTabHost.newTabSpec("BlockWrite").setIndicator("BlockWrite"),
                BlockWriteFragment.class, null);

        mTabHost.addTab(mTabHost.newTabSpec("BlockPermalock").setIndicator("BlockPermalock"),
                BlockPermalockFragment.class, null);
    }

    @Override
    protected void onDestroy() {
        releaseSoundPool();
        if (mReader != null) {
            mReader.free();
        }
        super.onDestroy();
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    HashMap<Integer, Integer> soundMap = new HashMap<Integer, Integer>();
    private SoundPool soundPool;
    private float volumnRatio;
    private AudioManager am;

    private void initSound() {
        soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 5);
        soundMap.put(1, soundPool.load(this, R.raw.barcodebeep, 1));
        soundMap.put(2, soundPool.load(this, R.raw.serror, 1));
        am = (AudioManager) this.getSystemService(AUDIO_SERVICE);
    }

    private void releaseSoundPool() {
        if(soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }
    public void playSound(int id) {
        float audioMaxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        float audioCurrentVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
        volumnRatio = audioCurrentVolume / audioMaxVolume;
        try {
            soundPool.play(soundMap.get(id), volumnRatio,
                    volumnRatio,
                    1,
                    0,
                    1
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkReadWritePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 0);
                finish();
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 2);
            }
        }
    }
}
