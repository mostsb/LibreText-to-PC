package matxorapps.com.libretext_to_pc;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by Mostsb on 21/01/2015.
 * Using code by Christopher Gwilliams
 */
public class SMSReceiver extends BroadcastReceiver {

   public void onReceive(Context context, Intent intent){
       Bundle pudsBundle = intent.getExtras();
       Object[] pdus = (Object[]) pudsBundle.get("pdus");
       SmsMessage messages = SmsMessage.createFromPdu((byte[]) pdus[0]);
       Log.i("SMS", messages.getMessageBody());
       Toast.makeText(context, "WHAT: " + messages.getMessageBody(), Toast.LENGTH_SHORT).show();


   }

}
