package matxorapps.com.libretext_to_pc;

import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;
import android.widget.EditText;
import android.widget.Toast;


public class MainActivity extends ActionBarActivity {
    Intent SMSServiceIntent;
    public static String hostAddr;
    public static String hostPort;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SMSServiceIntent = new Intent(this, SMSService.class);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new MainFragment())
                    .commit();
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void beginSMSIntercept(View view){
        EditText hostAddrText = (EditText) findViewById(R.id.host_address_text);
        EditText hostPortText = (EditText) findViewById(R.id.host_port_text);

        hostAddr = hostAddrText.getText().toString();
        hostPort = hostPortText.getText().toString();

        Toast.makeText(this,"hostAddr: "+hostAddr.length(), Toast.LENGTH_SHORT).show();
        Toast.makeText(this,"hostPort: "+hostPort.length(), Toast.LENGTH_SHORT).show();

        if(hostAddr.length() <= 0 || hostPort.length() <= 0){
            Toast.makeText(this,"Please enter an address and port.", Toast.LENGTH_SHORT).show();
            return;
        }else {
            startService(SMSServiceIntent);
        }
        /*SMSReceiver smsReceiver = new SMSReceiver();
        IntentFilter smsFilter = new IntentFilter();
        smsFilter.addAction("android.provider.Telephony.SMS_DELIVER");

        registerReceiver(smsReceiver, smsFilter);*/
        //Toast.makeText(this, "Service Started",Toast.LENGTH_SHORT).show();
    }

    public void stopSMSIntercept(View view){
        stopService(SMSServiceIntent);

    }
    /**
     * A placeholder fragment containing a simple view.
     */
    public static class MainFragment extends Fragment {

        public MainFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }


    }

}
