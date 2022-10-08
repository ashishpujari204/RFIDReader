package com.example.uhf.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import com.example.uhf.R;
import com.example.uhf.activity.UHFMainActivity;
import com.example.uhf.tools.UIHelper;
import com.example.uhf.view.UhfLocationCanvasView;
import com.rscja.deviceapi.interfaces.IUHF;
import com.rscja.deviceapi.interfaces.IUHFLocationCallback;


public class UHFLocationFragment extends KeyDwonFragment {

    String TAG="UHF_LocationFragment";
    private UHFMainActivity mContext;
    private UhfLocationCanvasView llChart;
    private EditText etEPC;
    private Button btStart,btStop;
    final int EPC=2;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_uhflocation, container, false);
    }
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mContext = (UHFMainActivity) getActivity();
        llChart=mContext.findViewById(R.id.llChart);
        etEPC=mContext.findViewById(R.id.etEPC);
        btStart=mContext.findViewById(R.id.btStart);
        btStop=mContext.findViewById(R.id.btStop);


        btStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startLocation();
            }
        });
        btStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopLocation();
            }
        });

        getView().post(new Runnable() {
            @Override
            public void run() {
                llChart.clean();
                if(mContext.uhfInfo.getSelectItem()!=null && !mContext.uhfInfo.getSelectItem().equals("")){
                    etEPC.setText(mContext.uhfInfo.getSelectItem());
                }else{
                    etEPC.setText("");
                }
            }
        });

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.i(TAG, "onDestroyView");
        stopLocation();
        Log.i(TAG, "onDestroyView end");
    }

    @Override
    public void myOnKeyDwon() {
        if(btStart.isEnabled()) {
            startLocation();
        }else{
            stopLocation();
        }
    }
    private void startLocation(){
       String epc=etEPC.getText().toString();
       boolean result= mContext.mReader.startLocation(mContext,epc,IUHF.Bank_EPC,32,new IUHFLocationCallback(){
            @Override
            public void getLocationValue(int Value) {
                llChart.setData(Value);
            }
        });
        if(!result){
            UIHelper.ToastMessage(mContext, R.string.psam_msg_fail);
            return;
        }
        btStart.setEnabled(false);
        etEPC.setEnabled(false);
    }

   public void stopLocation(){
       mContext.mReader.stopLocation();
       btStart.setEnabled(true);
       etEPC.setEnabled(true);
   }



}
