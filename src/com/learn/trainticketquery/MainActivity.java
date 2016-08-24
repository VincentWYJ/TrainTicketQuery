package com.learn.trainticketquery;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.DatePicker;
import android.widget.DatePicker.OnDateChangedListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


@SuppressLint("HandlerLeak")
public class MainActivity extends Activity implements OnClickListener {
    private final String API_KEY = "361cf2a2459552575b0e86e0f62302bc";
    private final String HTTP_URL = "http://apis.baidu.com/qunar/qunar_train_service/s2ssearch";
    private final String HTTP_ARG = "version=1.0&from=START&to=END&date=YEAR-MOUTH-DAY";

    private final int SUCCESS_WHAT = 0;
    private final int ERROR_WHAT = 1;

    private final String TRAIN_TYPE = "trainType";
    private final String START_TIME = "startTime";
    private final String END_TIME = "endTime";
    private final String FROM = "from";
    private final String TO = "to";
    private final String TRAIN_NO = "trainNo";
    private final String DURATION = "duration";
    private final String SEAT_INFOS = "seatInfos";
    private final String SEAT = "seat";
    private final String SEAT_PRICE = "seatPrice";
    private final String REMAIN_NUM = "remainNum";

    private EditText mStartPlaceET;
    private EditText mEndPlaceET;
    private TextView mTargetDayTV;
    private ListView mResultListView;

    private String[] mWeekDayArray;
    private String mTargetDay;
    private String tempTargetDay;
    private int mYear;
    private int mMouth;
    private int mDayOfMouth;
    private int mDayOfWeek;
    private Calendar mCalendar;
    private long mMinTimeMills;
    private long mMaxTimeMills;

    private JSONArray mJsonArray;

    private List<Map<String, Object>> mResultMapList;
    private SimpleAdapter mResultListAdapter;

    private Collator mCollator = Collator.getInstance(Locale.CHINA);
    private Comparator<Map<String, Object>> mComparatorResultMap = new Comparator<Map<String, Object>>() {

        @Override
        public int compare(Map<String, Object> lhs, Map<String, Object> rhs) {
            return mCollator.compare(lhs.get(START_TIME), rhs.get(START_TIME));
        }
    };

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            int what = msg.what;
            if(what == SUCCESS_WHAT) {
                mResultListAdapter.notifyDataSetChanged();
            }else if(what == ERROR_WHAT) {
                Utils.showToast(getString(R.string.please_input_place));
            }
        }
    };

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch(id) {
            case R.id.train_pre_day:
                break;
            case R.id.train_target_day:
                showDateSelectDialog();
                break;
            case R.id.train_next_day:
                break;
            case R.id.train_inquire:
                startTicketInquireThread();
                break;
            case R.id.place_swap:
                swapTrainPlace();
                break;
            default:
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Utils.showLog("MainActivity onCreate");

        setContentView(R.layout.activity_main);
        
        Utils.mContext = getApplicationContext();

        Utils.mIsBackFromSetNetwork = false;

        mStartPlaceET = (EditText) findViewById(R.id.train_start_place);
        mEndPlaceET = (EditText) findViewById(R.id.train_end_place);
        mResultListView = (ListView) findViewById(R.id.train_result);

        ((TextView) findViewById(R.id.train_pre_day)).setOnClickListener(this);
        mTargetDayTV = (TextView) findViewById(R.id.train_target_day);
        mTargetDayTV.setOnClickListener(this);
        ((TextView) findViewById(R.id.train_next_day)).setOnClickListener(this);
        ((TextView) findViewById(R.id.train_inquire)).setOnClickListener(this);
        ((ImageView) findViewById(R.id.place_swap)).setOnClickListener(this);

        mResultMapList = new ArrayList<Map<String,Object>>();
        mResultListAdapter = new SimpleAdapter(this, mResultMapList, R.layout.train_item,
                new String[]{START_TIME, END_TIME, FROM, TO, TRAIN_NO, DURATION, SEAT, SEAT_PRICE, REMAIN_NUM},
                new int[]{R.id.start_time, R.id.end_time, R.id.from, R.id.to, R.id.train_no, R.id.duration, R.id.seat, R.id.seat_price, R.id.remain_num});
        mResultListView.setAdapter(mResultListAdapter);
        mResultListView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String trainNo = ((TextView) view.findViewById(R.id.train_no)).getText().toString();

                Utils.showLog(trainNo);

                showTicketsInfoDialog(trainNo);
            }
        });

        getTodayTime();
    }

    private void swapTrainPlace() {
        String startPlace = mStartPlaceET.getText().toString();
        String endPlace = mEndPlaceET.getText().toString();
        mStartPlaceET.setText(endPlace);
        mEndPlaceET.setText(startPlace);
    }

    private void getTodayTime() {
        mWeekDayArray = getResources().getStringArray(R.array.train_week_day);
        mTargetDay = getString(R.string.train_target_day);
        mCalendar = Calendar.getInstance();
        mMinTimeMills = mCalendar.getTimeInMillis();
        mMaxTimeMills = mMinTimeMills+59l*24*3600*1000;
        getDateParams();
        tempTargetDay = getTempTargetDay(mYear, mMouth, mDayOfMouth, mDayOfWeek);
        mTargetDayTV.setText(tempTargetDay);

        //Utils.showLog(""+" "+mYear+" "+mMouth+" "+mDayOfMouth+" "+(mDayOfWeek-1));
    }

    private void getDateParams() {
        mYear = mCalendar.get(Calendar.YEAR);
        mMouth = mCalendar.get(Calendar.MONTH)+1;
        mDayOfMouth = mCalendar.get(Calendar.DAY_OF_MONTH);
        mDayOfWeek = mCalendar.get(Calendar.DAY_OF_WEEK);
    }

    private String getTempTargetDay(int year, int month, int dayOfMouth, int dayOfWeek) {
        String temp = mTargetDay.replace("Y", ""+year)
                .replace("M", ""+month)
                .replace("D", ""+dayOfMouth)
                .replace("W", mWeekDayArray[dayOfWeek-1]);

        return temp;
    }

    private void showDateSelectDialog() {
        final Dialog dateDialog = new Dialog(this, R.style.DialogFixTitle);
        dateDialog.setContentView(R.layout.train_datepicker);
        dateDialog.setCanceledOnTouchOutside(true);
        //final TextView title = (TextView) dateDialog.findViewById(R.id.title);
        TextView cancel = (TextView) dateDialog.findViewById(R.id.date_cancel);
        cancel.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                dateDialog.cancel();
            }
        });
        TextView confirm = (TextView) dateDialog.findViewById(R.id.date_confirm);
        confirm.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                dateDialog.cancel();
                mTargetDayTV.setText(tempTargetDay);
                getDateParams();
                startTicketInquireThread();
            }
        });
        DatePicker datePicker = (DatePicker) dateDialog.findViewById(R.id.train_datepicker);
        datePicker.setMinDate(mMinTimeMills);
        datePicker.setMaxDate(mMaxTimeMills);
        datePicker.init(mYear, mMouth-1, mDayOfMouth, new OnDateChangedListener() {

            @Override
            public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                mCalendar.set(year, monthOfYear, dayOfMonth);
                tempTargetDay = getTempTargetDay(year, monthOfYear+1, dayOfMonth, mCalendar.get(Calendar.DAY_OF_WEEK));

                Utils.showLog(tempTargetDay);
            }
        });
        dateDialog.show();
    }

    private void showTicketsInfoDialog(String trainNo) {
        final Dialog ticketDialog = new Dialog(this, R.style.DialogFixTitle);
        ticketDialog.setContentView(R.layout.train_tickets);
        ticketDialog.setCanceledOnTouchOutside(true);
        TextView confirm = (TextView) ticketDialog.findViewById(R.id.tickets_confirm);
        TextView title = (TextView) ticketDialog.findViewById(R.id.ticker_info_title);
        confirm.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                ticketDialog.cancel();
            }
        });
        ListView ticketsList = (ListView) ticketDialog.findViewById(R.id.train_tickets);
        List<Map<String, Object>> ticketsMapList = new ArrayList<Map<String,Object>>();
        try {
            JSONObject jsonObject3 = null;
            for(int i=0; i<mJsonArray.length(); ++i) {
                jsonObject3 = mJsonArray.getJSONObject(i);
                if(jsonObject3.get(TRAIN_NO).equals(trainNo)) {
                    break;
                }
            }
            title.setText(jsonObject3.get(TRAIN_NO).toString()+"--"+getString(R.string.ticket_info_title));
            JSONArray jsonArray3 = jsonObject3.getJSONArray(SEAT_INFOS);
            for(int i=0; i<jsonArray3.length(); ++i) {
                JSONObject jsonObject4 = jsonArray3.getJSONObject(i);
                Map<String, Object> map = new HashMap<String, Object>();
                map.put(SEAT, jsonObject4.get(SEAT));
                map.put(SEAT_PRICE, jsonObject4.get(SEAT_PRICE)+getString(R.string.train_price_unit));
                map.put(REMAIN_NUM, jsonObject4.get(REMAIN_NUM)+getString(R.string.train_ticket_unit));
                ticketsMapList.add(map);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        SimpleAdapter ticketsListAdapter = new SimpleAdapter(this, ticketsMapList, R.layout.ticket_item,
                new String[]{SEAT, SEAT_PRICE, REMAIN_NUM},
                new int[]{R.id.seat_type, R.id.ticket_price, R.id.ticket_num});
        ticketsList.setAdapter(ticketsListAdapter);
        ticketDialog.show();
    }

    private void startTicketInquireThread() {
        if(Utils.isNetWorkConnected()) {
            new Thread(new Runnable() {

                @Override
                public void run() {
                    inquireTrainTickets();
                }
            }).start();
        } else {
            Intent intent = new Intent(this, AccessNetworkActivity.class);
            startActivity(intent);
        }
    }

    private void inquireTrainTickets() {
        String startPlace = mStartPlaceET.getText().toString();
        String endPlace = mEndPlaceET.getText().toString();
        if(TextUtils.isEmpty(startPlace) || TextUtils.isEmpty(endPlace)) {
            mHandler.sendEmptyMessage(ERROR_WHAT);
        }else {
            String httpUrl;
            String httpArg = HTTP_ARG.replace("START", startPlace)
                    .replace("END", endPlace)
                    .replace("YEAR", ""+mYear)
                    .replace("MOUTH", mMouth>9?""+mMouth:"0"+mMouth)
                    .replace("DAY", mDayOfMouth>9?""+mDayOfMouth:"0"+mDayOfMouth);

            Utils.showLog(httpArg);

            BufferedReader reader = null;
            String result = null;
            StringBuffer sbf = new StringBuffer();
            httpUrl = HTTP_URL + "?" + httpArg;
            try {
                URL url = new URL(httpUrl);
                HttpURLConnection connection = (HttpURLConnection) url
                        .openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("apikey",  API_KEY);
                connection.connect();
                InputStream is = connection.getInputStream();
                reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                String strRead = null;
                while ((strRead = reader.readLine()) != null) {
                    sbf.append(strRead);
                    sbf.append("\r\n");
                }
                reader.close();
                result = sbf.toString();
            } catch (Exception e) {
                e.printStackTrace();
            }

            setTrainTicketList(result);
        }
    }

    private void setTrainTicketList(String result) {
        mResultMapList.clear();
        try {
            JSONArray jsonArray1 = new JSONObject(result).getJSONObject("data").getJSONArray("trainList");
            mJsonArray = jsonArray1;
            for(int i=0; i<jsonArray1.length(); ++i) {
                JSONObject jsonObject1 = jsonArray1.getJSONObject(i);

                //Utils.showLog(jsonObject1.toString());

                Map<String, Object> map = new HashMap<String, Object>();
                map.put(START_TIME, jsonObject1.get(START_TIME));
                map.put(END_TIME, jsonObject1.get(END_TIME));
                map.put(FROM, jsonObject1.get(FROM));
                map.put(TO, jsonObject1.get(TO));
                map.put(TRAIN_NO, jsonObject1.get(TRAIN_NO));
                map.put(DURATION, jsonObject1.get(DURATION));
                JSONArray jsonArray2 = jsonObject1.getJSONArray(SEAT_INFOS);
                JSONObject jsonObject2 = jsonArray2.getJSONObject(0);
                map.put(SEAT, jsonObject2.get(SEAT));
                map.put(SEAT_PRICE, jsonObject2.get(SEAT_PRICE)+getString(R.string.train_price_unit));
                map.put(REMAIN_NUM, jsonObject2.get(REMAIN_NUM)+getString(R.string.train_ticket_unit));
                mResultMapList.add(map);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Collections.sort(mResultMapList, mComparatorResultMap);
        mHandler.sendEmptyMessage(SUCCESS_WHAT);
    }

    public void onResume() {
        super.onResume();

        Utils.showLog("MainActivity Resume");

        if(Utils.mIsBackFromSetNetwork) {
            if (Utils.isNetWorkConnected()) {
                startTicketInquireThread();
            } else {
                mResultMapList.clear();
                mResultListAdapter.notifyDataSetChanged();
            }

            Utils.mIsBackFromSetNetwork = false;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Utils.showLog("MainActivity onDestroy");
    }
}
