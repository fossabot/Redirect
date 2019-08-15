package com.lmgy.redirect.ui.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.lmgy.redirect.R;
import com.lmgy.redirect.bean.HostData;
import com.lmgy.redirect.net.LocalVpnService;
import com.lmgy.redirect.utils.SPUtils;
import com.suke.widget.SwitchButton;

import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";
    private static final int VPN_REQUEST_CODE = 0x0F;

    private boolean waitingForVPNStart;
    private BroadcastReceiver vpnStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (LocalVpnService.BROADCAST_VPN_STATE.equals(intent.getAction())) {
                if (intent.getBooleanExtra("running", false))
                    waitingForVPNStart = false;
            }
        }
    };

    private SwitchButton mBtnVpn;
    private Button mBtnSetting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        mBtnVpn.setOnCheckedChangeListener(new SwitchButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(SwitchButton view, boolean isChecked) {
                if (isChecked) {
                    if (checkHost() == -1) showDialog();
                    else startVPN();
                } else {
                    shutdownVPN();
                }
            }
        });

        LocalBroadcastManager.getInstance(this).registerReceiver(vpnStateReceiver,
                new IntentFilter(LocalVpnService.BROADCAST_VPN_STATE));
    }

    @Override
    protected void onResume() {
        super.onResume();
        setButton(!waitingForVPNStart && !LocalVpnService.isRunning());
    }

    private void startVPN() {
        waitingForVPNStart = false;
        Intent vpnIntent = LocalVpnService.prepare(this);
        if (vpnIntent != null)
            startActivityForResult(vpnIntent, VPN_REQUEST_CODE);
        else
            onActivityResult(VPN_REQUEST_CODE, RESULT_OK, null);
    }

    private int checkHost() {
        List<HostData> list = SPUtils.getDataList(this, "hostList", HostData.class);
        if (list.size() == 0) return -1;
        else return 1;
    }

    private void shutdownVPN() {
        if (LocalVpnService.isRunning())
            startService(new Intent(this, LocalVpnService.class).setAction(LocalVpnService.ACTION_DISCONNECT));
        setButton(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VPN_REQUEST_CODE && resultCode == RESULT_OK) {
            waitingForVPNStart = true;
            startService(new Intent(this, LocalVpnService.class).setAction(LocalVpnService.ACTION_CONNECT));
            setButton(false);
        }
    }

    private void setButton(boolean enable) {
        if (enable) {
            mBtnVpn.setChecked(false);
            mBtnSetting.setAlpha(1.0f);
            mBtnSetting.setClickable(true);
        } else {
            mBtnVpn.setChecked(true);
            mBtnSetting.setAlpha(.5f);
            mBtnSetting.setClickable(false);
        }
    }

    private void showDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setTitle(R.string.dialog_title);
        builder.setMessage(R.string.dialog_message);
        builder.setPositiveButton(R.string.dialog_confirm, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                setButton(true);
                startActivity(new Intent(getApplicationContext(), HostSettingActivity.class));
            }
        });

        builder.setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                setButton(true);
            }
        });
        builder.show();
    }

    private void initView() {
        mBtnVpn = (SwitchButton) findViewById(R.id.btn_vpn);
        mBtnSetting = (Button) findViewById(R.id.btn_setting);
        mBtnSetting.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            default:
                break;
            case R.id.btn_setting:
                startActivity(new Intent(getApplicationContext(), HostSettingActivity.class));
                break;
        }
    }
}