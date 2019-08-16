package com.lmgy.redirect.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatEditText;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.lmgy.redirect.R;
import com.lmgy.redirect.bean.HostData;
import com.lmgy.redirect.utils.SPUtils;

import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChangeSettingActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "ChangeSettingActivity";
    /**
     * ip address
     */
    private AppCompatEditText mIpAddress;
    /**
     * hostname
     */
    private AppCompatEditText mHostname;
    /**
     * remark
     */
    private AppCompatEditText mRemark;
    /**
     * SAVE
     */
    private Button mBtnSave;
    private int mId;
    private HostData hostData;
    private CoordinatorLayout mCoordinatorLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_setting);
        initView();
        ActionBar actionBar = Objects.requireNonNull(getSupportActionBar());
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle("Edit");
        hostData = (HostData) getIntent().getSerializableExtra("hostData");
        mId = getIntent().getIntExtra("id", -1);
        if (hostData != null) {
            mIpAddress.setText(hostData.getIpAddress());
            mHostname.setText(hostData.getHostName());
            mRemark.setText(hostData.getRemark());
        }
    }

    private boolean isIP(String address) {
        //首先对长度进行判断
        if (address.length() < 7 || address.length() > 15) {
            return false;
        }
        /**
         * 判断IP格式和范围
         */
        String rexp = "([1-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}";
        Pattern pat = Pattern.compile(rexp);
        Matcher mat = pat.matcher(address);
        boolean ipAddress = mat.find();
        if (ipAddress) {
            String ips[] = address.split("\\.");
            if (ips.length == 4) {
                try {
                    for (String ip : ips) {
                        if (Integer.parseInt(ip) < 0 || Integer.parseInt(ip) > 255) {
                            return false;
                        }
                    }
                } catch (Exception e) {
                    return false;
                }
                return true;
            } else {
                return false;
            }
        }
        return ipAddress;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initView() {
        mIpAddress = (AppCompatEditText) findViewById(R.id.ipAddress);
        mHostname = (AppCompatEditText) findViewById(R.id.hostname);
        mRemark = (AppCompatEditText) findViewById(R.id.remark);
        mBtnSave = (Button) findViewById(R.id.btn_save);
        mCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinatorLayout);
        mBtnSave.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            default:
                break;
            case R.id.btn_save:
                if (isIP(mIpAddress.getText().toString())) {
                    if (!mHostname.getText().toString().isEmpty()) {
                        hostData = new HostData(true, mIpAddress.getText().toString(), mHostname.getText().toString(), mRemark.getText().toString());
                        saveOrUpdate(hostData);
                    } else {
                        Snackbar.make(mCoordinatorLayout, getString(R.string.input_correct_hostname), Snackbar.LENGTH_SHORT).show();
                    }
                } else {
                    Snackbar.make(mCoordinatorLayout, getString(R.string.input_correct_ip), Snackbar.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private void saveOrUpdate(final HostData savedHostData) {
        final List<HostData> hostDataList = SPUtils.getDataList(this, "hostList", HostData.class);
        if (mId != -1) {//覆盖
            HostData hostData = hostDataList.get(mId);
            hostData.setIpAddress(savedHostData.getIpAddress());
            hostData.setHostName(savedHostData.getHostName());
            hostData.setType(savedHostData.getType());
            hostData.setRemark(savedHostData.getRemark());
        } else {//新建
            hostDataList.add(savedHostData);
        }
        SPUtils.setDataList(this, "hostList", hostDataList);
        Intent i = new Intent();
        i.putExtra("result", getString(R.string.save_successful));
        setResult(3, i);
        finish();
    }
}
