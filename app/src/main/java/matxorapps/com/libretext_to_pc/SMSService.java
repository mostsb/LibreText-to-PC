package matxorapps.com.libretext_to_pc;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.Looper;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;

/**
 * Created by Matto on 21/01/2015.
 */
public class SMSService extends Service {
    public static Socket smsSocket;
    public static SMSReceiver smsReceiver = new SMSReceiver();
    public static OutputStreamWriter smsWriter;
    @Override
    public void onCreate(){


        Thread initSocket = new Thread(){
            public void run() {
                Looper.prepare();
                try {
                    InetAddress remoteAddr = InetAddress.getByName("192.168.1.6");
                    smsSocket = new Socket(remoteAddr, 7666);
                    smsWriter = new OutputStreamWriter(smsSocket.getOutputStream());
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "Could not connect to server", Toast.LENGTH_SHORT).show();
                }
                Looper.loop();
            }
        };

        initSocket.start();
        IntentFilter smsFilter = new IntentFilter();
        smsFilter.addAction("android.provider.Telephony.RECEIVE_SMS");

        registerReceiver(smsReceiver, smsFilter);

        ComponentName receiver = new ComponentName(this, SMSReceiver.class);
        PackageManager pm = this.getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);

        Toast.makeText(this,"Receiver Engaged", Toast.LENGTH_SHORT).show();


        super.onCreate();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        ComponentName receiver = new ComponentName(this, SMSReceiver.class);
        PackageManager pm = this.getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
        Toast.makeText(this,"Killed Receiver",Toast.LENGTH_SHORT).show();
        //unregisterReceiver(smsReceiver);
        try {
            smsSocket.close();
            smsWriter.close();
        }catch (Exception e){
            Toast.makeText(this, "Could not close socket", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent arg0){
        return null;
    }


    public static void pushSMS(String sms, OutputStreamWriter smsWriter){
        try {


            smsWriter.write(sms);
            smsWriter.flush();

        }catch(Exception e){
            //Toast.makeText(, "Could not write to socket", Toast.LENGTH_SHORT).show();
            Log.d("SOCKET", "Could not write to socket");
        }


    }


}
