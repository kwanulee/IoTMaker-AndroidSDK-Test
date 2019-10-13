package com.example.kwanwoo.iotmaker_android;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.gson.Gson;
import com.kt.gigaiot_sdk.DeviceApi;
import com.kt.gigaiot_sdk.data.Device;
import com.kt.gigaiot_sdk.data.DeviceApiResponse;

import java.util.ArrayList;

public class DeviceListActivity extends AppCompatActivity implements AdapterView.OnItemClickListener{
    private final int ROW_CNT = 10;
    private int mPageNum = 1;

    private ListView mListView;
    ArrayList<Device> mDevices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);

        mListView = findViewById(R.id.lv_device_list);
        mListView.setOnItemClickListener(this);

        new GetDevListTask().execute(); // GetDevListTask의 doInBackground() 메소드 수행

    }

    /**
     * 디바이스 목록 중 하나가 선택되었을 때 호출되는 메소드
     * @param parent 디바이스 목록을 표시하는 리스트뷰 객체
     * @param view 선택된 항목 뷰 객체
     * @param position 디바이스 목록 중 선택된 항목 위치
     * @param id
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (mDevices != null) {
            Gson gson = new Gson();
            String strDevice = gson.toJson(mDevices.get(position)); // 리스트뷰에서 선택된 디바이스 객체 정보를 JSON 문자열로 변환

            Intent intent = new Intent(DeviceListActivity.this, DeviceActivity.class);
            intent.putExtra(DeviceActivity.EXTRA_DEVICE, strDevice);    // JSON 문자열로 변환된 디바이스 객체 정보를  DeviceActivity에 Extras를 통해 전달
            startActivity(intent);
        }
    }

    /*
     * DeviceListActivity UI 스레드와는 별도의 스레드로 IoTMaker와 네트워크 통신 수행
     */
    private class GetDevListTask extends AsyncTask<Void, Void, DeviceApiResponse> {

        @Override
        protected DeviceApiResponse doInBackground(Void... voids) {
            // Access Token을 통해 DeviceApi 객체 획득
            DeviceApi deviceApi = new DeviceApi(ApplicationPreference.getInstance().getPrefAccessToken());
            // 디바이스 목록 조회 요청
            DeviceApiResponse response = deviceApi.getDeviceList(mPageNum, ROW_CNT);

            return response;
        }

        /**
         * doInBackground 메소드가 수행된 직후에 수행되는 메소드로서 doInBackground 메소드의 리턴 값이 result 파라미터 값으로 전달됨
         * @param result: Device 목록 요청에 대한 응답 정보를 포함한 DeviceApiResponse 객체
         */
        @Override
        protected void onPostExecute(DeviceApiResponse result) {
            // 전체 디바이스 객체 목록을  ArrayList 객체인 mDevices 멤버변수에 저장
            mDevices = result.getDevices();

            ArrayList<String> device_names = new ArrayList<String>();
            for (Device d: mDevices) {
                device_names.add(d.getDevNm());
            }

            // ArrayAdpater 객체 생성 및 설정
            ArrayAdapter dAdapter = new ArrayAdapter(DeviceListActivity.this,
                    android.R.layout.simple_list_item_1, device_names.toArray());

            // 리스트뷰에 어뎁터 객체 연결
            mListView.setAdapter(dAdapter);

        }
    }
}
