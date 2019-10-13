package com.example.kwanwoo.iotmaker_android;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.kt.gigaiot_sdk.TagStrmApi;
import com.kt.gigaiot_sdk.data.Device;
import com.kt.gigaiot_sdk.data.TagStrm;
import com.kt.gigaiot_sdk.data.TagStrmApiResponse;
import com.kt.gigaiot_sdk.network.ApiConstants;

import java.util.ArrayList;

public class DeviceActivity extends AppCompatActivity implements AdapterView.OnItemClickListener{
    String TAG="DeviceActivity";
    public static final String EXTRA_DEVICE = "device";
    private Device mDevice;
    private ArrayList<TagStrm> mTagStrms;

    private ListView mListView;
    Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        mListView = findViewById(R.id.lv_tagstrm_list);
        mListView.setOnItemClickListener(this);

        String strDevice = getIntent().getStringExtra(EXTRA_DEVICE);

        Gson gson = new Gson();
        mDevice = gson.fromJson(strDevice, Device.class);

        TextView tw_device = findViewById(R.id.tv_device_info);
        tw_device.setText("디바이스 이미지 일련번호: "+ mDevice.getAtcFileSeq() +"\n"+
                "디바이스 생성일시: "+ mDevice.getCretDt() +"\n"+
                "디바이스 모델 명: "+ mDevice.getDevModelNm() +"\n"+
                "디바이스 모델 일련번호: "+ mDevice.getDevModelSeq() +"\n"+
                "디바이스 명: "+ mDevice.getDevNm() +"\n"+
                "게이트웨이 연결 아이디: "+ mDevice.getGwCnctId() +"\n"+
                "프로토콜 아이디: "+ mDevice.getProtID() +"\n"+
                "프로토콜 명: " + mDevice.getProtNm() + "\n"+
                "디바이스 아이디: " + mDevice.getSpotDevId() + "\n"+
                "디바이스 일련번호: " + mDevice.getSpotDevSeq() + "\n"+
                "서비스 대상 일련번호: " + mDevice.getSvcTgtSeq() + "\n"
        );

        // 태그스트림 목록을 읽어오는 AsyncTask 수행
        new GetTagStrmListTask().execute();

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (mTagStrms != null) {

            TagStrm tagStrm = mTagStrms.get(position);
            Gson gson = new Gson();
            String strTag = gson.toJson(tagStrm);
            String strDevice = gson.toJson(mDevice);

            if (tagStrm.getTagStrmPrpsTypeCd().equals(TagStrmApi.TAGSTRM_DATA)) {
                Intent intent = new Intent(DeviceActivity.this, TagStrmLogActivity.class);
                intent.putExtra(DeviceActivity.EXTRA_DEVICE, strDevice);
                intent.putExtra(TagStrmLogActivity.EXTRA_TAGSTRM, strTag);
                startActivity(intent);
            } else if (tagStrm.getTagStrmPrpsTypeCd().equals(TagStrmApi.TAGSTRM_CTRL)) {
                createSendCtrlMsgDialog(position);
            }
        }

    }

    private void createSendCtrlMsgDialog(final int position){

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View dilogView = inflater.inflate(R.layout.dialog_device_ctrl, null);
        final EditText etCtrlMsg = (EditText) dilogView.findViewById(R.id.et_device_ctrl);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("제어요청 보내기")
                .setView(dilogView)
                .setPositiveButton("보내기", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final String ctrlMsg = etCtrlMsg.getText().toString();

                        //TODO : 제어요청 API 호출

                        new Thread(new Runnable() {
                            @Override
                            public void run() {

                                TagStrmApi tagStrmApi = new TagStrmApi(ApplicationPreference.getInstance().getPrefAccessToken());
                                final TagStrmApiResponse response = tagStrmApi.sendCtrlMsg(
                                        mDevice.getSvcTgtSeq(), mDevice.getSpotDevSeq(), mDevice.getSpotDevId(), mDevice.getGwCnctId(),
                                        mTagStrms.get(position).getTagStrmId(),
                                        mTagStrms.get(position).getTagStrmValTypeCd(),
                                        ApplicationPreference.getInstance().getPrefAccountId(), ctrlMsg);

                                if (response.getResponseCode().equals(ApiConstants.CODE_OK)) {

                                    mHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(DeviceActivity.this, "제어 요청이 성공하였습니다.", Toast.LENGTH_SHORT).show();
                                        }
                                    });

                                } else if (response.getResponseCode().equals(ApiConstants.CODE_NG)) {

                                    mHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(DeviceActivity.this, "제어 요청이 실패하였습니다.\n[" + response.getMessage() + "]", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }

                            }
                        }).start();
                    }
                })
                .setNegativeButton("취소", null);


        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * TagStrmApi를 통해 태그스트림 목록을 읽어와서 리스트뷰에 제어 및 수집용으로 구분하여
     * 태그스트림을 표시한다.
     */
    private class GetTagStrmListTask extends AsyncTask<Void, Void, TagStrmApiResponse> {

        @Override
        protected TagStrmApiResponse doInBackground(Void... params) {

            TagStrmApi tagStrmApi = new TagStrmApi(ApplicationPreference.getInstance().getPrefAccessToken());
            TagStrmApiResponse response = tagStrmApi.getTagStrmList(mDevice.getSpotDevId());

            return response;
        }

        @Override
        protected void onPostExecute(TagStrmApiResponse result) {

            mTagStrms = result.getTagStrms();

            ArrayList<String> tagStrm_names = new ArrayList<String>();
            for (TagStrm tag: mTagStrms) {
                String type ="UnKnown";
                if (tag.getTagStrmPrpsTypeCd().equals("0000010"))
                    type ="수집";
                else if (tag.getTagStrmPrpsTypeCd().equals("0000020"))
                    type = "제어";
                tagStrm_names.add("[" + type +"] "+ tag.getTagStrmId());
            }

            // ArrayAdpater 객체 생성 및 설정
            ArrayAdapter dAdapter = new ArrayAdapter(DeviceActivity.this,
                    android.R.layout.simple_list_item_1, tagStrm_names.toArray());

            // 리스트뷰에 어뎁터 객체 연결
            mListView.setAdapter(dAdapter);

        }
    }
}
