package matxorapps.com.libretext_to_pc;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.widget.Toast;

/**
 * Created by Matto on 21/01/2015.
 */
public class SMSService extends Service {
    public static SMSReceiver smsReceiver = new SMSReceiver();
    @Override
    public void onCreate(){



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
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent arg0){
        return null;
    }
}
