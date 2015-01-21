package matxorapps.com.libretext_to_pc;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.telephony.SmsManager;

/**
 * Created by Matto on 21/01/2015.
 */
public class SMSService extends Service {

    @Override
    public void onCreate(){
        super.onCreate();
        SMSReceiver smsReceiver = new SMSReceiver();

        IntentFilter smsFilter = new IntentFilter();
        smsFilter.addAction(TELEPHONY_SERVICE);

        registerReceiver(smsReceiver, smsFilter);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();

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
