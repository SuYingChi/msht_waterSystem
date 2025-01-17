package com.msht.watersystem.functionView;

import android.content.Context;
import android.content.Intent;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.mcloyal.serialport.entity.Packet;
import com.mcloyal.serialport.exception.CRCException;
import com.mcloyal.serialport.exception.CmdTypeException;
import com.mcloyal.serialport.exception.FrameException;
import com.mcloyal.serialport.service.PortService;
import com.mcloyal.serialport.utils.ComServiceConnection;
import com.mcloyal.serialport.utils.FrameUtils;
import com.mcloyal.serialport.utils.PacketUtils;
import com.msht.watersystem.Base.BaseActivity;
import com.msht.watersystem.R;
import com.msht.watersystem.Utils.BitmapUtil;
import com.msht.watersystem.Utils.BusinessInstruct;
import com.msht.watersystem.Utils.ByteUtils;
import com.msht.watersystem.Utils.InstructUtil;
import com.msht.watersystem.Utils.DataCalculateUtils;
import com.msht.watersystem.Utils.FormatToken;
import com.msht.watersystem.Utils.VariableUtil;
import com.msht.watersystem.widget.MyImgScroll;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class NotSufficient extends BaseActivity implements Observer, Handler.Callback {
    private boolean     buyStatus=false;
    private TextView    tv_Balalance;
    private TextView    tv_CardNo;
    private TextView    tv_time;
    private TextView    tv_Success;
    private TextView    tv_Notbalance;
    private ImageView   imageView;
    private ImageView   textView;
    private boolean     bindStatus=false;
    private Context     mContext;
    private MyCountDownTimer myCountDownTimer;// 倒计时对象
    private PortService      portService;
    private ComServiceConnection serviceConnection;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_not_sufficient);
        mContext=this;
        myCountDownTimer=new MyCountDownTimer(30000,1000);
        initView();
        initWaterQuality();
        OpenService();
    }
    private void OpenService() {
        serviceConnection = new ComServiceConnection(NotSufficient.this, new ComServiceConnection.ConnectionCallBack() {
            @Override
            public void onServiceConnected(PortService service) {
                //此处给portService赋值有如下两种方式
                //portService=service;
                portService = serviceConnection.getService();
            }
        });
        bindService(new Intent(mContext, PortService.class), serviceConnection,
                BIND_AUTO_CREATE);
        bindStatus=true;
    }
    private void initView() {
        imageView=(ImageView)findViewById(R.id.id_erwei_code);
        tv_Success=(TextView)findViewById(R.id.id_success_text);
        tv_Notbalance=(TextView) findViewById(R.id.id_not_balance);
        tv_Balalance=(TextView)findViewById(R.id.id_balance_amount);
        tv_CardNo=(TextView)findViewById(R.id.id_tv_customerNo);
        tv_time=(TextView)findViewById(R.id.id_time) ;
        double balance= DataCalculateUtils.TwoDecinmal2(FormatToken.Balance/100.0);
        tv_Balalance.setText(String.valueOf(balance));
        tv_CardNo.setText(String.valueOf(FormatToken.StringCardNo));
        myCountDownTimer.start();
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
                if (Arrays.equals(packet1.getCmd(),new byte[]{0x01,0x04})){
                   // MyLogUtil.d("主板控制指令104：", CreateOrderType.getPacketString(packet1));
                    initCom104Data(packet1.getData());
                }else if (Arrays.equals(packet1.getCmd(),new byte[]{0x01,0x05})){
                    initCom105Data(packet1.getData());
                }else if (Arrays.equals(packet1.getCmd(),new byte[]{0x02,0x04})){
                    initCom204Data();
                }
            }
            Packet packet2 = myObservable.getCom2Packet();
            if (packet2 != null) {
               if (Arrays.equals(packet2.getCmd(),new byte[]{0x01,0x04})){
                    String stringWork= DataCalculateUtils.IntToBinary(ByteUtils.byteToInt(packet2.getData().get(45)));
                    if (DataCalculateUtils.isRechargeData(stringWork,5,6)){
                        responseServer(packet2.getFrame());   //回复
                    }
                    initCom104Data2(packet2.getData());
                } else if (Arrays.equals(packet2.getCmd(),new byte[]{0x01,0x07})){
                    responseService(packet2.getFrame());
                    initCom107Data(packet2.getData());
                }else if (Arrays.equals(packet2.getCmd(),new byte[]{0x01,0x02})){
                    response102(packet2.getFrame());
                    initCom102Data2(packet2.getData());
                }
            }
        }
    }
    private void initCom204Data() {
        if (buyStatus){
            buyStatus=false;
            if (FormatToken.ConsumptionType==1){
                Intent intent=new Intent(mContext,IcCardoutWater.class);
                startActivityForResult(intent,1);
                finish();
            }else if (FormatToken.ConsumptionType==3){
                Intent intent=new Intent(mContext,AppoutWater.class);
                startActivityForResult(intent,1);
                finish();
            }else if (FormatToken.ConsumptionType==5){
                Intent intent=new Intent(mContext,DeliveryOutWater.class);
                startActivityForResult(intent,1);
                finish();
            }
        }
    }
    private void response102(byte[] frame) {
        if (portService != null) {
            try {
                byte[] type = new byte[]{0x02, 0x02};
                byte[] packet = PacketUtils.makePackage(frame, type, null);
                portService.sendToCom2(packet);
            } catch (CRCException e) {
                e.printStackTrace();
            } catch (FrameException e) {
                e.printStackTrace();
            } catch (CmdTypeException e) {
                e.printStackTrace();
            }
        }
    }
    private void initCom102Data2(ArrayList<Byte> data) {
        if (BusinessInstruct.ControlModel(mContext,data)){
            if (FormatToken.ShowTDS==0){
                layout_TDS.setVisibility(View.GONE);
            }else {
                layout_TDS.setVisibility(View.VISIBLE);
            }
        }
    }
    private void initCom107Data(ArrayList<Byte> data) {
        if (BusinessInstruct.CalaculateBusiness(data)){
            if (FormatToken.BusinessType==3){
                FormatToken.Balance=FormatToken.Balance+FormatToken.rechargeAmount;
                double balance= DataCalculateUtils.TwoDecinmal2(FormatToken.Balance/100.0);
                tv_Balalance.setText(String.valueOf(balance));
                if (FormatToken.AppBalance<20){
                    imageView.setVisibility(View.VISIBLE);
                    tv_Notbalance.setVisibility(View.VISIBLE);
                    tv_Success.setVisibility(View.GONE);
                }else {
                    imageView.setVisibility(View.GONE);
                    tv_Notbalance.setVisibility(View.INVISIBLE);
                    tv_Success.setVisibility(View.VISIBLE);
                }
            }else {
                VariableUtil.byteArray.clear();
                VariableUtil.byteArray=data;
                buyStatus=true;
                if (FormatToken.BusinessType==1){
                    if (FormatToken.AppBalance<20){
                        double balance= DataCalculateUtils.TwoDecinmal2(FormatToken.AppBalance/100.0);
                        tv_Balalance.setText(String.valueOf(balance));
                    }else {
                        setBusiness(1);
                    }
                }else if (FormatToken.BusinessType==2){
                    setBusiness(2);
                }
            }
        }
    }
    private void setBusiness(int business) {
        if (portService != null) {
            try {
                byte[] frame = FrameUtils.getFrame(mContext);
                byte[] type = new byte[]{0x01, 0x04};
                if (business==1){
                    byte[] data= InstructUtil.setBusinessType01();
                    byte[] packet = PacketUtils.makePackage(frame, type, data);
                    portService.sendToCom1(packet);
                }else if (business==2){
                    byte[] data= InstructUtil.setBusinessType02();
                    byte[] packet = PacketUtils.makePackage(frame, type, data);
                    portService.sendToCom1(packet);
                }
            } catch (CRCException e) {
                e.printStackTrace();
            } catch (FrameException e) {
                e.printStackTrace();
            } catch (CmdTypeException e) {
                e.printStackTrace();
            }
        }
    }
    private void responseService(byte[] frame) {
        if (portService != null) {
            try {
                byte[] type = new byte[]{0x02, 0x07};
                byte[] packet = PacketUtils.makePackage(frame, type, null);
                portService.sendToCom2(packet);
            } catch (CRCException e) {
                e.printStackTrace();
            } catch (FrameException e) {
                e.printStackTrace();
            } catch (CmdTypeException e) {
                e.printStackTrace();
            }
        }
    }
    private void initCom104Data(ArrayList<Byte> data) {
        try {
            if(InstructUtil.ControlInstruct(data)){
                    if (FormatToken.Balance<20){
                        double balance= DataCalculateUtils.TwoDecinmal2(FormatToken.Balance/100.0);
                        tv_Balalance.setText(String.valueOf(balance));
                        tv_CardNo.setText(String.valueOf(FormatToken.StringCardNo));
                    }else {
                        String stringWork= DataCalculateUtils.IntToBinary(FormatToken.Updateflag3);
                        if (DataCalculateUtils.isEvent(stringWork,3)){
                            double balance= DataCalculateUtils.TwoDecinmal2(FormatToken.Balance/100.0);
                            tv_Balalance.setText(String.valueOf(balance));
                            tv_CardNo.setText(String.valueOf(FormatToken.StringCardNo));
                        }else {
                            if (FormatToken.ConsumptionType==1){
                                Intent intent=new Intent(mContext,IcCardoutWater.class);
                                startActivityForResult(intent,1);
                                myCountDownTimer.cancel();
                                finish();
                            }else if (FormatToken.ConsumptionType==3){
                                Intent intent=new Intent(mContext,AppoutWater.class);
                                startActivityForResult(intent,1);
                                myCountDownTimer.cancel();
                                finish();
                            }else if (FormatToken.ConsumptionType==5){
                                Intent intent=new Intent(mContext,DeliveryOutWater.class);
                                startActivityForResult(intent,1);
                                myCountDownTimer.cancel();
                                finish();
                            }
                        }
                    }
                }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    private void responseServer(byte[] frame) {
        if (portService != null) {
            try {
                byte[] type = new byte[]{0x02, 0x04};
                byte[] packet = PacketUtils.makePackage(frame, type, null);
                portService.sendToCom2(packet);
            } catch (CRCException e) {
                e.printStackTrace();
            } catch (FrameException e) {
                e.printStackTrace();
            } catch (CmdTypeException e) {
                e.printStackTrace();
            }
        }
    }
    private void initCom105Data(ArrayList<Byte> data) {
        try {
            if (InstructUtil.StatusInstruct(data)){
                tv_InTDS.setText(String.valueOf(FormatToken.OriginTDS));
                tv_OutTDS.setText(String.valueOf(FormatToken.PurificationTDS));
                String stringWork= DataCalculateUtils.IntToBinary(FormatToken.WorkState);
                if (!DataCalculateUtils.isEvent(stringWork,6)){
                    Intent intent=new Intent(mContext, CannotBuywater.class);
                    startActivityForResult(intent,1);
                    finish();
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    private void initCom104Data2(ArrayList<Byte> data) {
        String stringWork= DataCalculateUtils.IntToBinary(ByteUtils.byteToInt(data.get(45)));
        int Switch=ByteUtils.byteToInt(data.get(31));
        if (Switch==2&&DataCalculateUtils.isEvent(stringWork,0)){
            Intent intent=new Intent(mContext, CloseSystem.class);
            startActivityForResult(intent,1);
            finish();
        }
    }
    private void CloseService(){
        if (serviceConnection != null && portService != null) {
            if (bindStatus){
                portService.removeObserver(this);
                unbindService(serviceConnection);
                bindStatus=false;
            }
        }
    }
    class MyCountDownTimer extends CountDownTimer {
        public MyCountDownTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }
        @Override
        public void onTick(long millisUntilFinished) {  //计时过程
            tv_time.setText(millisUntilFinished/1000+"");
        }
        @Override
        public void onFinish() {
            finish();
        }
    }
    private void endTimeCount() {
        if (myCountDownTimer != null) {
            myCountDownTimer.cancel();
        }
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode== KeyEvent.KEYCODE_BACK&& event.getRepeatCount()==0){
            showTips();
        }else if (keyCode==KeyEvent.KEYCODE_MENU){
            showTips();
        }else if (keyCode==KeyEvent.KEYCODE_DPAD_UP){
            showTips();
        }else if (keyCode==KeyEvent.KEYCODE_DPAD_DOWN){
            showTips();
        }else if (keyCode==KeyEvent.KEYCODE_F1){
            showTips();
        }
        return true;
    }
    private void showTips() {
        finish();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        CloseService();
        endTimeCount();
    }
}
