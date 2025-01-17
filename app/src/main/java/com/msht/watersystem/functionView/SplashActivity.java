package com.msht.watersystem.functionView;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.view.KeyEvent;

import com.mcloyal.serialport.entity.Packet;
import com.mcloyal.serialport.service.PortService;
import com.mcloyal.serialport.utils.ComServiceConnection;
import com.msht.watersystem.Base.BaseActivity;
import com.msht.watersystem.R;
import com.msht.watersystem.MainSerialPort;
import com.msht.watersystem.Utils.CachePreferencesUtil;
import com.msht.watersystem.Utils.InstructUtil;
import com.msht.watersystem.widget.LoadingDialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Observable;
import java.util.Observer;

public class SplashActivity extends BaseActivity  implements Observer, Handler.Callback{
    private final static String TAG = SplashActivity.class.getSimpleName();
    private PortService portService;
    private ComServiceConnection serviceConnection;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        loadingdialog = new LoadingDialog(mContext);
        loadingdialog.setLoadText("系统正在启动，请稍后...");
        loadingdialog.show();
        initData();
        OpenService();
    }

    private void initData() {
        boolean isFirstOpen = CachePreferencesUtil.getBoolean(this, CachePreferencesUtil.FIRST_OPEN, true);
        if (isFirstOpen){
            CachePreferencesUtil.putStringData(this,CachePreferencesUtil.Volume,"5");
            CachePreferencesUtil.putStringData(this,CachePreferencesUtil.outWaterTime,"30");
        }
    }
    @Override
    public boolean handleMessage(Message msg) {
        return false;
    }
    @Override
    public void update(Observable observable, Object arg) {
        PortService.MyObservable myObservable = (PortService.MyObservable) observable;
        if (myObservable != null) {
            boolean skeyEnable = myObservable.isSkeyEnable();
            Packet packet1 = myObservable.getCom1Packet();
            if (packet1 != null) {
                if (Arrays.equals(packet1.getCmd(),new byte[]{0x01,0x05})){
                    initCom105Data(packet1.getData());
                }
            }
            Packet packet2 = myObservable.getCom2Packet();
            if (packet2 != null) {
                if (Arrays.equals(packet2.getCmd(),new byte[]{0x02,0x05})){
                    initCom205Data(packet2.getData());
                }
            }
        }
    }
    private void initCom105Data(ArrayList<Byte> data) {      //接收到主板心跳指令
        if (InstructUtil.StatusInstruct(data)){
            if (loadingdialog.isShowing()&&loadingdialog!=null){
                loadingdialog.dismiss();
            }
            startActivity(new Intent(SplashActivity.this,
                    MainSerialPort.class));
            finish();
        }
    }
    private void initCom205Data(ArrayList<Byte> data) {}
    private void OpenService(){
        serviceConnection = new ComServiceConnection(SplashActivity.this, new ComServiceConnection.ConnectionCallBack() {
            @Override
            public void onServiceConnected(PortService service) {
                portService = serviceConnection.getService();
            }
        });
        bindService(new Intent(mContext, PortService.class), serviceConnection,
                BIND_AUTO_CREATE);

    }
    private void CloseService(){
        if (serviceConnection != null && portService != null) {
            portService.removeObserver(this);
            unbindService(serviceConnection);
        }
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode== KeyEvent.KEYCODE_BACK&& event.getRepeatCount()==0){
            finish();
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }
    @Override
    protected void onResume() {
        super.onResume();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        CloseService();
    }
}
