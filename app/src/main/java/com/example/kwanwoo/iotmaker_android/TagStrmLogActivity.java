package com.example.kwanwoo.iotmaker_android;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.kt.gigaiot_sdk.TagStrmApi;
import com.kt.gigaiot_sdk.data.Device;
import com.kt.gigaiot_sdk.data.Log;
import com.kt.gigaiot_sdk.data.TagStrm;
import com.kt.gigaiot_sdk.data.TagStrmApiResponse;
import com.kt.gigaiot_sdk.network.ApiConstants;

import org.w3c.dom.Text;

import java.util.ArrayList;

public class TagStrmLogActivity extends AppCompatActivity {
    public static final String EXTRA_TAGSTRM = "tagstrm";
    private TagStrm mTagStrm;
    private Device mDevice;

    private String period;
    private String count;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tag_strm_log);

        Gson gson = new Gson();

        // JSON 문자열 형태로 TagStrmLogActivity로 전달된 TagStrm 객체를 받아옴
        String strTag = getIntent().getStringExtra(EXTRA_TAGSTRM);
        mTagStrm = gson.fromJson(strTag, TagStrm.class);

        // JSON 문자열 형태로 TagStrmLogActivity로 전달된 Device 객체를 받아옴
        String strDevice = getIntent().getStringExtra(DeviceActivity.EXTRA_DEVICE);
        mDevice = gson.fromJson(strDevice, Device.class);

        // 화면 상단에 조회할 태그스트림 ID를 표시함
        TextView textView = findViewById(R.id.tv_tagstrm);
        String tag_id = mTagStrm.getTagStrmId();
        textView.setText("태그 스트림 [" +tag_id + "] 조회");

        // 조회 버튼이 클릭되었을 때 호출될 이벤트 리스너 정의
        Button btn_log = findViewById(R.id.btn_log);
        btn_log.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // 두개의 텍스트뷰로부터 기간(period)과 개수(count)정보를 읽어옴
                TextView tv = findViewById(R.id.et_log_period);
                period = tv.getText().toString();
                tv = findViewById(R.id.et_log_count);
                count = tv.getText().toString();

                // 미입력시 오류 처리
                if(TextUtils.isEmpty(period)){
                    Toast.makeText(TagStrmLogActivity.this, R.string.period_empty, Toast.LENGTH_SHORT).show();
                    return;
                } else if(TextUtils.isEmpty(count)){
                    Toast.makeText(TagStrmLogActivity.this, R.string.count_empty, Toast.LENGTH_SHORT).show();
                    return;
                }

                // TagStrmApi를 통해 TagStrmLog를 읽어오는 AsyncTask 수행
                new GetLogPoolingTask().execute();
            }
        });
    }

    /**
     * TagStrmApi를 통해 TagStrmLog를 읽어와서, 현재 조회할 태그스트림 id와 일치하는 로그에 대해서만
     * 로그 값과 로그가 측정된 시간정보를 하나의 문자열로 만들고, 이들을 누적시킨 결과를 텍스트 뷰에 출력한다.
     *
     */
    private class GetLogPoolingTask extends AsyncTask<Void, Void, TagStrmApiResponse> {

        @Override
        protected TagStrmApiResponse doInBackground(Void... params) {

            TagStrmApi tagStrmApi = new TagStrmApi(ApplicationPreference.getInstance().getPrefAccessToken());
            TagStrmApiResponse response = tagStrmApi.getTagStrmLog(mDevice.getSpotDevId(),period, count );
            // mDevice.setTagStrmList(response.getTagStrms());

            return response;
        }

        @Override
        protected void onPostExecute(TagStrmApiResponse result) {
          //  ArrayList<Object> log_str = new ArrayList<Object>();

            if (result.getResponseCode().equals(ApiConstants.CODE_OK)) {
                ArrayList<Log> logs = result.getLogs();
                StringBuffer result_str = new StringBuffer();
                for(Log log : logs){
                    Object tagValue = log.getAttributes().get(mTagStrm.getTagStrmId());

                    if (log.getAttributes().get(mTagStrm.getTagStrmId()) != null) { // 태그스트림 id와 일치하는 태그스트림 로그에 대해서.
                        // result_str 문자열에 값과 시간정보를 나타내는 문자열을 추가
                        result_str.append(tagValue.toString()+ " [" + log.getOccDt() + "] \n");
                    }
                }
                TextView tv_result_str = findViewById(R.id.tv_log_result);
                tv_result_str.setText(result_str);
            }

        }
    }
}
