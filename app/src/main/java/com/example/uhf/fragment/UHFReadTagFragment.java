package com.example.uhf.fragment;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;

import com.example.uhf.R;
import com.example.uhf.UhfInfo;
import com.example.uhf.activity.UHFMainActivity;
import com.example.uhf.tools.NumberTool;
import com.example.uhf.tools.StringUtils;
import com.example.uhf.tools.UIHelper;
import com.rscja.deviceapi.RFIDWithUHFUART;
import com.rscja.deviceapi.entity.UHFTAGInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;


public class UHFReadTagFragment extends KeyDwonFragment {
    private static final String TAG = "UHFReadTagFragment";
    private boolean loopFlag = false;
    private int inventoryFlag = 1;
    private final List<String> tempDatas = new ArrayList<>();
    MyAdapter adapter;
    Button BtClear;
    TextView tvTime;
    TextView tv_count;
    TextView tv_total;
    RadioGroup RgInventory;
    RadioButton RbInventorySingle;
    RadioButton RbInventoryLoop;

    Button BtInventory;
    ListView LvTags;
    private UHFMainActivity mContext;

    private int total;
    private long time;

    private CheckBox cbFilter;
    private ViewGroup layout_filter;

    public static final String TAG_EPC = "tagEPC";
    public static final String TAG_EPC_TID = "tagEpcTID";
    public static final String TAG_COUNT = "tagCount";
    public static final String TAG_RSSI = "tagRssi";

    private CheckBox cbEPC_Tam;

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            UHFTAGInfo info = (UHFTAGInfo) msg.obj;
            addDataToList(info.getEPC(),mergeTidEpc(info.getTid(), info.getEPC(),info.getUser()), info.getRssi());
            setTotalTime();
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i(TAG, "UHFReadTagFragment.onCreateView");
        return inflater.inflate(R.layout.uhf_readtag_fragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.i(TAG, "UHFReadTagFragment.onActivityCreated");
        super.onActivityCreated(savedInstanceState);
        mContext = (UHFMainActivity) getActivity();
        mContext.currentFragment=this;
        BtClear = getView().findViewById(R.id.BtClear);
        tvTime = getView().findViewById(R.id.tvTime);
        tvTime.setText("0s");
        tv_count =  getView().findViewById(R.id.tv_count);
        tv_total =  getView().findViewById(R.id.tv_total);
        RgInventory = getView().findViewById(R.id.RgInventory);
        RbInventorySingle =  getView().findViewById(R.id.RbInventorySingle);
        RbInventoryLoop =  getView().findViewById(R.id.RbInventoryLoop);

        BtInventory = getView().findViewById(R.id.BtInventory);

        LvTags = getView().findViewById(R.id.LvTags);
        adapter=new MyAdapter(mContext);
        LvTags.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                adapter.setSelectItem(position);
                adapter.notifyDataSetInvalidated();
            }
        });
        LvTags.setAdapter(adapter);
        BtClear.setOnClickListener(new BtClearClickListener());
        RgInventory.setOnCheckedChangeListener(new RgInventoryCheckedListener());
        BtInventory.setOnClickListener(new BtInventoryClickListener());

        initFilter(getView());

        initEPCTamperAlarm(getView());
        //clearData();
        tv_count.setText(mContext.tagList.size()+"");
        tv_total.setText(total+"");
        Log.i(TAG, "UHFReadTagFragment.EtCountOfTags=" + tv_count.getText());
    }

    private Button btnSetFilter;

    private void initFilter(View view) {
        layout_filter = view.findViewById(R.id.layout_filter);
        layout_filter.setVisibility(View.GONE);
        cbFilter = view.findViewById(R.id.cbFilter);
        cbFilter.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                layout_filter.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        });

        final EditText etLen = view.findViewById(R.id.etLen);
        final EditText etPtr = view.findViewById(R.id.etPtr);
        final EditText etData = view.findViewById(R.id.etData);
        final RadioButton rbEPC = view.findViewById(R.id.rbEPC);
        final RadioButton rbTID = view.findViewById(R.id.rbTID);
        final RadioButton rbUser = view.findViewById(R.id.rbUser);
        btnSetFilter = view.findViewById(R.id.btSet);

        btnSetFilter.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                int filterBank = RFIDWithUHFUART.Bank_EPC;
                if (rbEPC.isChecked()) {
                    filterBank = RFIDWithUHFUART.Bank_EPC;
                } else if (rbTID.isChecked()) {
                    filterBank = RFIDWithUHFUART.Bank_TID;
                } else if (rbUser.isChecked()) {
                    filterBank = RFIDWithUHFUART.Bank_USER;
                }
                if (etLen.getText().toString() == null || etLen.getText().toString().isEmpty()) {
                    UIHelper.ToastMessage(mContext, "????????????????????????");
                    return;
                }
                if (etPtr.getText().toString() == null || etPtr.getText().toString().isEmpty()) {
                    UIHelper.ToastMessage(mContext, "????????????????????????");
                    return;
                }
                int ptr = StringUtils.toInt(etPtr.getText().toString(), 0);
                int len = StringUtils.toInt(etLen.getText().toString(), 0);
                String data = etData.getText().toString().trim();
                if (len > 0) {
                    String rex = "[\\da-fA-F]*"; //???????????????????????????????????????????????????
                    if (data == null || data.isEmpty() || !data.matches(rex)) {
                        UIHelper.ToastMessage(mContext, "??????????????????????????????????????????");
                        return;
                    }

                    if (mContext.mReader.setFilter(filterBank, ptr, len, data)) {
                        UIHelper.ToastMessage(mContext, R.string.uhf_msg_set_filter_succ);
                    } else {
                        UIHelper.ToastMessage(mContext, R.string.uhf_msg_set_filter_fail);
                    }
                } else {
                    //????????????
                    String dataStr = "";
                    if (mContext.mReader.setFilter(RFIDWithUHFUART.Bank_EPC, 0, 0, dataStr)
                            && mContext.mReader.setFilter(RFIDWithUHFUART.Bank_TID, 0, 0, dataStr)
                            && mContext.mReader.setFilter(RFIDWithUHFUART.Bank_USER, 0, 0, dataStr)) {
                        UIHelper.ToastMessage(mContext, R.string.msg_disable_succ);
                    } else {
                        UIHelper.ToastMessage(mContext, R.string.msg_disable_fail);
                    }
                }
                cbFilter.setChecked(false);

            }
        });
        CheckBox cb_filter = view.findViewById(R.id.cb_filter);
        rbEPC.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (rbEPC.isChecked()) {
                    etPtr.setText("32");
                }
            }
        });
        rbTID.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (rbTID.isChecked()) {
                    etPtr.setText("0");
                }
            }
        });
        rbUser.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (rbUser.isChecked()) {
                    etPtr.setText("0");
                }
            }
        });
    }

    private void initEPCTamperAlarm(View view) {
        cbEPC_Tam = view.findViewById(R.id.cbEPC_Tam);
        cbEPC_Tam.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    //mContext.mReader.setEPCAndTamperAlarmMode();
                } else {
                    mContext.mReader.setEPCMode();
                }
            }
        });
    }

    @Override
    public void onPause() {
        Log.i(TAG, "UHFReadTagFragment.onPause");
        super.onPause();

        // ????????????
        stopInventory();
    }

    /**
     * ????????????????????????
     *
     * @param
     */
    private void addDataToList(String epc,String epcAndTidUser, String rssi) {
        if (StringUtils.isNotEmpty(epc)) {
            int index = checkIsExist(epc);
            HashMap<String, String> map = new HashMap<String, String>();
            map.put(TAG_EPC, epc);
            map.put(TAG_EPC_TID, epcAndTidUser);
            map.put(TAG_COUNT, String.valueOf(1));
            map.put(TAG_RSSI, rssi);
            if (index == -1) {
                mContext.tagList.add(map);
                tempDatas.add(epc);
                tv_count.setText(String.valueOf(adapter.getCount()));
            } else {
                int tagCount = Integer.parseInt(Objects.requireNonNull(mContext.tagList.get(index).get(TAG_COUNT)), 10) + 1;
                map.put(TAG_COUNT, String.valueOf(tagCount));
                map.put(TAG_EPC_TID, epcAndTidUser);
                mContext.tagList.set(index, map);
            }
            tv_total.setText(String.valueOf(++total));
            adapter.notifyDataSetChanged();

            //----------
            mContext.uhfInfo.setTempDatas(tempDatas);
            mContext.uhfInfo.setTagList(mContext.tagList);
            mContext.uhfInfo.setCount(total);
            mContext.uhfInfo.setTagNumber(adapter.getCount());
        }
    }

    public class BtClearClickListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            clearData();
            selectItem=-1;
            mContext.uhfInfo=new UhfInfo();
        }
    }

    private void clearData() {
        tv_count.setText("0");
        tv_total.setText("0");
        tvTime.setText("0s");
        total = 0;
        mContext.tagList.clear();
        tempDatas.clear();

        adapter.notifyDataSetChanged();
    }

    public class RgInventoryCheckedListener implements OnCheckedChangeListener {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            if (checkedId == RbInventorySingle.getId()) {
                // ????????????
                inventoryFlag = 0;
                cbFilter.setChecked(false);
                cbFilter.setVisibility(View.INVISIBLE);
            } else if (checkedId == RbInventoryLoop.getId()) {
                // ?????????????????????
                inventoryFlag = 1;
                cbFilter.setVisibility(View.VISIBLE);
            }
        }
    }


    public class BtInventoryClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            readTag();
        }
    }

    private void readTag() {
        cbFilter.setChecked(false);
        if (BtInventory.getText().equals(mContext.getString(R.string.btInventory))) {// ????????????
            switch (inventoryFlag) {
                case 0:// ??????
                    time = System.currentTimeMillis();
                    UHFTAGInfo uhftagInfo = mContext.mReader.inventorySingleTag();
                    if (uhftagInfo != null) {
                        String tid = uhftagInfo.getTid();
                        String epc = uhftagInfo.getEPC();
                        String user=uhftagInfo.getUser();
                        addDataToList(epc,mergeTidEpc(tid, epc, user), uhftagInfo.getRssi());
                        setTotalTime();
                        mContext.playSound(1);
                    } else {
                        UIHelper.ToastMessage(mContext, R.string.uhf_msg_inventory_fail);
//					mContext.playSound(2);
                    }
                    break;
                case 1:// ???????????????
                    if (mContext.mReader.startInventoryTag()) {
                        BtInventory.setText(mContext.getString(R.string.title_stop_Inventory));
                        loopFlag = true;
                        setViewEnabled(false);
                        time = System.currentTimeMillis();
                        new TagThread().start();
                    } else {
                        stopInventory();
                        UIHelper.ToastMessage(mContext, R.string.uhf_msg_inventory_open_fail);
//					mContext.playSound(2);
                    }
                    break;
                default:
                    break;
            }
        } else {// ????????????
            stopInventory();
            setTotalTime();
        }
    }

    private void setTotalTime() {
        float useTime = (System.currentTimeMillis() - time) / 1000.0F;
        tvTime.setText(NumberTool.getPointDouble(1, useTime) + "s");
    }

    private void setViewEnabled(boolean enabled) {
        RbInventorySingle.setEnabled(enabled);
        RbInventoryLoop.setEnabled(enabled);
        cbFilter.setEnabled(enabled);
        btnSetFilter.setEnabled(enabled);
        BtClear.setEnabled(enabled);
        cbEPC_Tam.setEnabled(enabled);
    }

    /**
     * ????????????
     */
    private void stopInventory() {
        if (loopFlag) {
            loopFlag = false;
            setViewEnabled(true);
            if (mContext.mReader.stopInventory()) {
                BtInventory.setText(mContext.getString(R.string.btInventory));
            } else {
                UIHelper.ToastMessage(mContext, R.string.uhf_msg_inventory_stop_fail);
            }
        }
    }

    /**
     * ??????EPC??????????????????
     *
     * @param epc ??????
     * @return
     */
    public int checkIsExist(String epc) {
        if (StringUtils.isEmpty(epc)) {
            return -1;
        }
        return binarySearch(tempDatas, epc);
    }

    /**
     * ????????????????????????????????????????????????????????????-1
     */
    static int binarySearch(List<String> array, String src) {
        int left = 0;
        int right = array.size() - 1;
        // ??????????????? <=
        while (left <= right) {
            if (compareString(array.get(left), src)) {
                return left;
            } else if (left != right) {
                if (compareString(array.get(right), src))
                    return right;
            }
            left++;
            right--;
        }
        return -1;
    }

    static boolean compareString(String str1, String str2) {
        if (str1.length() != str2.length()) {
            return false;
        } else if (str1.hashCode() != str2.hashCode()) {
            return false;
        } else {
            char[] value1 = str1.toCharArray();
            char[] value2 = str2.toCharArray();
            int size = value1.length;
            for (int k = 0; k < size; k++) {
                if (value1[k] != value2[k]) {
                    return false;
                }
            }
            return true;
        }
    }

    class TagThread extends Thread {
        public void run() {
            UHFTAGInfo uhftagInfo;
            Message msg;
            while (loopFlag) {
                uhftagInfo = mContext.mReader.readTagFromBuffer();
                if (uhftagInfo != null) {
                    msg = handler.obtainMessage();
                    msg.obj = uhftagInfo;
                    handler.sendMessage(msg);
                    mContext.playSound(1);
                }
            }
        }
    }

    private String mergeTidEpc(String tid, String epc,String user) {
        String data="EPC:"+ epc;
        if (!TextUtils.isEmpty(tid) && !tid.equals("0000000000000000") && !tid.equals("000000000000000000000000")) {
            data+= "\nTID:" + tid ;
        }
        if(user!=null && user.length()>0) {
            data+="\nUSER:"+user;
        }
        return  data;
    }

    @Override
    public void myOnKeyDwon() {
        readTag();
    }


    //-----------------------------
    private int  selectItem=-1;
    public final class ViewHolder {
        public TextView tvEPCTID;
        public TextView tvTagCount;
        public TextView tvTagRssi;
    }

    public class MyAdapter extends BaseAdapter {
        private final LayoutInflater mInflater;
        public MyAdapter(Context context) {
            this.mInflater = LayoutInflater.from(context);
        }
        public int getCount() {
            // TODO Auto-generated method stub
            return mContext.tagList.size();
        }
        public Object getItem(int arg0) {
            // TODO Auto-generated method stub
            return mContext.tagList.get(arg0);
        }
        public long getItemId(int arg0) {
            // TODO Auto-generated method stub
            return arg0;
        }
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = mInflater.inflate(R.layout.listtag_items, null);
                holder.tvEPCTID = convertView.findViewById(R.id.TvTagUii);
                holder.tvTagCount = convertView.findViewById(R.id.TvTagCount);
                holder.tvTagRssi = convertView.findViewById(R.id.TvTagRssi);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.tvEPCTID.setText(mContext.tagList.get(position).get(TAG_EPC_TID));
            holder.tvTagCount.setText(mContext.tagList.get(position).get(TAG_COUNT));
            holder.tvTagRssi.setText(mContext.tagList.get(position).get(TAG_RSSI));

            if (position == selectItem) {
                convertView.setBackgroundColor(mContext.getResources().getColor(R.color.lfile_colorPrimary));
            }
            else {
                convertView.setBackgroundColor(Color.TRANSPARENT);
            }
            return convertView;
        }
        public  void setSelectItem(int select) {
            if(selectItem==select){
                selectItem=-1;
                mContext.uhfInfo.setSelectItem("");
                mContext.uhfInfo.setSelectIndex(selectItem);
            }else {
                selectItem = select;
                mContext.uhfInfo.setSelectItem(mContext.tagList.get(select).get(TAG_EPC));
                mContext.uhfInfo.setSelectIndex(selectItem);
            }
        }
    }
}
