package com.example.kwanwoo.iotmaker_android;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.kt.gigaiot_sdk.GigaIotOAuth;
import com.kt.gigaiot_sdk.data.GiGaIotOAuthResponse;
import com.kt.gigaiot_sdk.network.ApiConstants;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    private EditText mEtId, mEtPw, mEtAppId, mEtSec;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        ApplicationPreference.init(this);

        String token = ApplicationPreference.getInstance().getPrefAccessToken();
        if (token != null && token.equals("") == false) {
            Intent intent = new Intent(LoginActivity.this, DeviceListActivity.class);
            startActivity(intent);
            finish();
        }

        mEtId = findViewById(R.id.et_login_id);
        mEtPw = findViewById(R.id.et_login_pw);
        mEtAppId = findViewById(R.id.et_app_id);
        mEtSec = findViewById(R.id.et_app_secret);

        ImageView ivLogin = (ImageView) findViewById(R.id.iv_login_bt);
        ivLogin.setOnClickListener(this);
    }


    public void onClick(View v) {
        switch(v.getId()){

            case R.id.iv_login_bt:
                String id = mEtId.getText().toString();
                String pw = mEtPw.getText().toString();
                String app_id = mEtAppId.getText().toString();
                String secret = mEtSec.getText().toString();

                if(TextUtils.isEmpty(id)){
                    Toast.makeText(LoginActivity.this, R.string.login_id_empty, Toast.LENGTH_SHORT).show();
                    return;
                } else if(TextUtils.isEmpty(pw)){
                    Toast.makeText(LoginActivity.this, R.string.login_pw_empty, Toast.LENGTH_SHORT).show();
                    return;
                } else if(TextUtils.isEmpty(app_id)){
                    Toast.makeText(LoginActivity.this, R.string.login_app_id_empty, Toast.LENGTH_SHORT).show();
                    return;
                } else if(TextUtils.isEmpty(secret)){
                    Toast.makeText(LoginActivity.this, R.string.login_secret_empty, Toast.LENGTH_SHORT).show();
                    return;
                }

                new LoginTask().execute();

                break;

        }
    }

    private class LoginTask extends AsyncTask<Void, Void, GiGaIotOAuthResponse> {
        ProgressDialog progressDialog;
        String id;

        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(LoginActivity.this, "", getResources().getString(R.string.common_wait), true, false);
        }

        @Override
        protected GiGaIotOAuthResponse doInBackground(Void... params) {

            id = mEtId.getText().toString();
            String pw = mEtPw.getText().toString();
            String app_id = mEtAppId.getText().toString();
            String secret = mEtSec.getText().toString();

            //테스트용
            GigaIotOAuth gigaIotOAuth = new GigaIotOAuth(app_id, secret);
            GiGaIotOAuthResponse response = gigaIotOAuth.loginWithPassword(id, pw);

            return response;
        }

        @Override
        protected void onPostExecute(GiGaIotOAuthResponse result) {
            if(progressDialog != null && progressDialog.isShowing()){
                progressDialog.dismiss();
                progressDialog = null;
            }

            if(result != null && result.getResponseCode().equals(ApiConstants.CODE_OK)){
                ApplicationPreference.getInstance().setPrefAccountId(id);
                ApplicationPreference.getInstance().setPrefAccessToken(result.getAccessToken());
                //Toast.makeText(LoginActivity.this, getResources().getString(R.string.login_success), Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(LoginActivity.this, DeviceListActivity.class);
                startActivity(intent);

            }else{
                Toast.makeText(LoginActivity.this, getResources().getString(R.string.login_fail) + result.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
}
