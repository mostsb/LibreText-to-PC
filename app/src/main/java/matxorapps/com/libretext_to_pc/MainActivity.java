package matxorapps.com.libretext_to_pc;

import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.net.Inet4Address;
import java.net.InetAddress;


public class MainActivity extends ActionBarActivity {

    /*
    Intent to start the SMS service
     */
    Intent SMSServiceIntent;

    /*
    the port that is used for communication
     */
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

        /*
            Display the device's IP address for easy connecting
            */
        TextView deviceIp = (TextView) findViewById(R.id.device_address);
        WifiManager wlan = (WifiManager) getSystemService(WIFI_SERVICE);

        int ipInt = wlan.getConnectionInfo().getIpAddress();

        Thread getIpThread = new Thread( new Runnable(){
            public void run(){
                try

                {
                    Log.d("localhost", InetAddress.getLocalHost().toString());
                }

                catch(
                        Exception e
                        )

                {
                    e.printStackTrace();
                }
            }
        });

        getIpThread.start();


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


    /*
    Pressing the Start Service button invokes this method, starting the SMS service
     */
    public void beginSMSIntercept(View view){

        EditText hostPortText = (EditText) findViewById(R.id.host_port_text);
        hostPort = hostPortText.getText().toString();

        if(hostPort.length() <= 0){
            Toast.makeText(this,"Please enter a port.", Toast.LENGTH_SHORT).show();
            return;
        }else {
            startService(SMSServiceIntent);
        }

    }

    /*
    Stops the service. hopefully closing all sockets and killing the receiver in the process
     */
    public void stopSMSIntercept(View view){
        stopService(SMSServiceIntent);

    }

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
