package matxorapps.com.libretext_to_pc;

import android.app.Service;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.os.Looper;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;

/**
 * Created by Mostsb on 21/01/2015.
 */
public class SMSService extends Service {
    public static Socket smsSocket;
    public static SSLServerSocket sslServerSocket;
    public static SSLServerSocket sslReaderServerSocket;
    public static SMSReceiver smsReceiver = new SMSReceiver();
    public static OutputStreamWriter smsWriter;
    public static String currentSMS = "";
    public static String originatingAddress = "";
    public static int isStopped = 0;

    @Override
    public void onCreate(){

        isStopped = 0;

        /* Attempt to create a server READER socket - two way communication */
        Thread serverReaderThread = new Thread(){

            /*
            create the communication channel on a different port than the one used to push sms messages.
            this prevents blocking on the connection
             */

            int socketServerPort = Integer.parseInt(MainActivity.hostPort)+1;

            public void run(){
                Looper.prepare();
                try {

                    /*
                    The SSL Keystore uses Bouncy Castle, the android default.
                    The following code sets up retrieval of the keystore from the raw file
                     */
                    String keyStoreType = KeyStore.getDefaultType();
                    KeyStore keyStore = KeyStore.getInstance(keyStoreType);

                    /*
                     Opens the raw file as an input stream, required by the keystore.load method
                     */
                    InputStream keyStream = getApplication().getResources().openRawResource(R.raw.serverkeystore);

                    /*
                    The default keystore password is 'libretextssl'. I strongly suggest compiling this app from source and substituting your own keystore for added security.
                    Since, as far as I know, generation of a new keystore for every app downloaded from google play is impossible, this default keystore is used for the google play app.

                    If I am incorrect in this assumption, please correct me.
                     */
                    keyStore.load(keyStream,"libretextssl".toCharArray());

                    /*
                    When an SSL connection is in the handshake phase, a session key algorithm is negotiated. The following code sets this up using a default algorithm.
                     */
                    String keyalgorithm = KeyManagerFactory.getDefaultAlgorithm();
                    KeyManagerFactory keyManFact = KeyManagerFactory.getInstance(keyalgorithm);

                    /*
                    provide the KeyManagerFactory with the keystore previously loaded, note the default password is used again here as well.
                     */
                    keyManFact.init(keyStore,"libretextssl".toCharArray());

                    /*
                    Which version of SSL to use? TLS of course.
                     */
                    SSLContext context = SSLContext.getInstance("TLS");
                    context.init(keyManFact.getKeyManagers(),null,null);

                    /*
                    Create an server socket for the ssl connection, allowing multiple connections.
                     */
                    sslReaderServerSocket = (SSLServerSocket) context.getServerSocketFactory().createServerSocket(socketServerPort);

                    /*
                    Main loop for creating connections
                    */
                    while(true){
                        /*
                        FYI the following line blocks while waiting for a connection
                         */
                        SSLSocket clSocket = (SSLSocket) sslReaderServerSocket.accept();

                       /*
                        All new SSL client sockets are passed off to a new thread, defined by the class CLReaderCommunication.
                        This class deals with the port responsible for receiving messages from clients. There is a separate class
                        for pushing SMS messages to connected clients

                        After the new thread is started, execution loops back up top and waits for a new client connection.
                         */
                        ClReaderCommunication newClient = new ClReaderCommunication(clSocket);
                        newClient.start();


                    }


                }catch (Exception e){
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "Could not create the server socket, or socket closed",Toast.LENGTH_SHORT).show();
                }



                Looper.loop();
            }

        };
        serverReaderThread.start();

        /*
        Attempt to create an SSL Socket for pushing SMS
         */

        Thread sslCommSocketThread = new Thread(){


            /*
            SMS messages are pushed on the port the user designated within the app.
             */
            int sslServerPort = Integer.parseInt(MainActivity.hostPort);

            public void run(){
                Looper.prepare();
                try {
                    /*
                    The SSL Keystore uses Bouncy Castle, the android default.
                    The following code sets up retrieval of the keystore from the raw file
                     */
                    String keyStoreType = KeyStore.getDefaultType();
                    KeyStore keyStore = KeyStore.getInstance(keyStoreType);

                    /*
                     Opens the raw file as an input stream, required by the keystore.load method
                     */
                    InputStream keyStream = getApplication().getResources().openRawResource(R.raw.serverkeystore);

                    /*
                    The default keystore password is 'libretextssl'. I strongly suggest compiling this app from source and substituting your own keystore for added security.
                    Since, as far as I know, generation of a new keystore for every app downloaded from google play is impossible, this default keystore is used for the google play app.

                    If I am incorrect in this assumption, please correct me.
                     */
                    keyStore.load(keyStream,"libretextssl".toCharArray());

                    /*
                    When an SSL connection is in the handshake phase, a session key algorithm is negotiated. The following code sets this up using a default algorithm.
                     */
                    String keyalgorithm = KeyManagerFactory.getDefaultAlgorithm();
                    KeyManagerFactory keyManFac = KeyManagerFactory.getInstance(keyalgorithm);

                    /*
                    provide the KeyManagerFactory with the keystore previously loaded, note the default password is used again here as well.
                     */
                    keyManFac.init(keyStore,"libretextssl".toCharArray());

                    /*
                    Which version of SSL to use? TLS of course.
                     */
                    SSLContext context = SSLContext.getInstance("TLS");
                    context.init(keyManFac.getKeyManagers(),null,null);

                    /*
                    Create an server socket for the ssl connection, allowing multiple connections.
                    NOTE: SMS messages will be pushed to ALL CLIENTS connected to this port
                     */
                    sslServerSocket = (SSLServerSocket) context.getServerSocketFactory().createServerSocket(sslServerPort);

                    /*
                    Main loop for creating connections
                    */
                    while(true){

                        /*
                        FYI the following line blocks while waiting for a connection
                         */
                        SSLSocket sslClSocket = (SSLSocket) sslServerSocket.accept();

                         /*
                        All new SSL client sockets are passed off to a new thread, defined by the class SSLClCommunication.
                        This class deals with the port responsible for pushing SMS messages to clients. There is a separate class
                        for two-way communication with connected clients

                        After the new thread is started, execution loops back up top and waits for a new client connection.
                         */
                        SSLClCommunication sslClComm = new SSLClCommunication(sslClSocket);
                        sslClComm.start();



                    }
                }catch(Exception e){
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "Could not create the server socket, or socket closed",Toast.LENGTH_SHORT).show();
                }


                Looper.loop();
            }
        };
        sslCommSocketThread.start();

        /*
        Define the IntentFilter for the BroadcastReceiver that will intercept all incoming SMS messages
         */
        IntentFilter smsFilter = new IntentFilter();
        smsFilter.addAction("android.provider.Telephony.RECEIVE_SMS");

        /*
        Register the receiver
         */
        registerReceiver(smsReceiver, smsFilter);

        /*
        For some reason, when a receiver is unregistered, it still runs and intercepts SMS as if it was registered.
        The following code provides a fix to *Enable* a register.

        There is a complementary block of code in the onDestroy method that properly *Disables* the register after being unregistered
         */
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

        /*
        Fixes an issue where the receiver does not *Disable* after being unregistered.
         */
        ComponentName receiver = new ComponentName(this, SMSReceiver.class);
        PackageManager pm = this.getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);

        Toast.makeText(this,"Killed Receiver",Toast.LENGTH_SHORT).show();

        /*
        The SMS BroadcastReceiver is unregistered
         */
        unregisterReceiver(smsReceiver);
        try {
            /*
            Close all open sockets
             */
            sslServerSocket.close();
            sslReaderServerSocket.close();

            /*
            Iteration through the socket arrays would usually fail on close, so just set a global variable which all threads can check, if it == 1, close all connections.
             */
            isStopped = 1;

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


    public static void pushSMS(String sms,String origAddress){

        /*
        For some reason the BroadcastReceiver cannot set a variable in this class directly (even though it is public and static),
        so this function does just that

        currentSMS = the body of the message received
        originatingAddress = the phone number from which the SMS was received
         */
        currentSMS = sms;
        originatingAddress = origAddress;




    }


    /*
    Pushes SMS messages received to connected clients
     */
    public class SSLClCommunication extends Thread{

            /*
            Establish a variable used to interact with the socket
             */
            private SSLSocket sslClSocket = null;

            /*
            Allow sockets to be passed to this class
             */
            public SSLClCommunication(SSLSocket sslClientSocket){
                super("SSLClCommunication");
                sslClSocket = sslClientSocket;
            }
            public void run() {
                Looper.prepare();
                    try{
                        /*
                        Open an output stream for pushing SMS messages.

                        Upon Connection:
                                        A "Connection Established" message will be presented on a successful connection

                        On SMS Push:
                                        SMS messages are pushed as follows:
                                                smsPacket:
                                                        [origin]phonenumberhere\n
                                                        [message]sms message body here\n

                         */
                        OutputStreamWriter clSocketWriter = new OutputStreamWriter(sslClSocket.getOutputStream());
                        String welcomeMessage = "Connection Established\n";
                        String oldSMS = "";
                        clSocketWriter.write(welcomeMessage);
                        clSocketWriter.flush();
                        String smsPacket = "";
                        while(true){
                            /*
                            if the isStopped variable is set, kill the thread and socket
                             */
                            if(isStopped == 1){
                                break;
                            }
                            /*
                            Only push new SMS messages received
                             */
                            if(currentSMS != oldSMS){

                                /*
                                construct the packet
                                 */
                                smsPacket = "[origin]" + originatingAddress + "\n[message]" + currentSMS + "\n";
                                clSocketWriter.write(smsPacket);
                                clSocketWriter.flush();
                                /*
                                make sure the SMS is not pushed indefinitely
                                 */
                                oldSMS = currentSMS;
                            }
                        }


                        /*
                        Close sockets in use on thread termination
                         */
                        clSocketWriter.close();
                        sslClSocket.close();
                    }catch(Exception e){

                    }


                Looper.loop();
            }

    }

    /*
    Allows two-way communication between client and server.
    Commands can be sent to send SMS messages and to terminate the connection.
     */
    public class ClReaderCommunication extends Thread{

        /*
        establish a local variable for interaction with the socket
         */
        private SSLSocket clientSocket = null;

        /*
        allows a socket to be passed to this class
         */
        public ClReaderCommunication(SSLSocket clSocket){
            super("ClReaderCommunication");
            clientSocket = clSocket;


        }

        /*
        Using the Android SMSManager, send an SMS text message to a designated phone number
         */
        public void sendSMSMessage(String message, String phoneNumber){
            SmsManager sendSMS = SmsManager.getDefault();
            sendSMS.sendTextMessage(phoneNumber,null,message,null,null);
        }

        public void run(){
            Looper.prepare();


            try {

                /*
                open a writing stream and a reading stream on the socket, allowing two-way communication
                 */
                OutputStreamWriter clSocketWriter = new OutputStreamWriter(clientSocket.getOutputStream());
                InputStreamReader clSocketReader = new InputStreamReader(clientSocket.getInputStream());

                /*
                This buffer is used to prevent permanent blocking on the InputStreamReader. By reading one character at a time and checking for a newline character,
                blocking can be circumvented.
                 */
                char[] messageBuffer = new char[1];

                /*
                Used to designate whether a newline character was found
                 */
                int isNewLine = 0;

                /*
                Message received from the client
                 */
                String clMessage = "";

                /*
                The number to send an SMS message to
                 */
                String recipient = "";

                /*
                The actual message to send to a recipient number
                 */
                String smsMessage = "";

                /*
                Displayed to the client upon a successful connection
                 */
                String welcomeMessage = "Connection Established\nSMS-sh - >";
                clSocketWriter.write(welcomeMessage);
                clSocketWriter.flush();
                while(true){

                        /*
                        If this variable is set, kill the thread
                         */
                        if(isStopped == 1){
                            break;
                        }

                        /*
                        If no newline character is found, continue the read operation
                         */
                        while(isNewLine == 0) {

                            /*
                            Read one character at a time, preventing blocking
                             */
                            clSocketReader.read(messageBuffer, 0, 1);

                            /*
                            FYI 0x0a is the hexcode for newline, so this is checking if a newline character was detected

                            if a newline is detected, set the isNewLine flag and stop the read operation

                            if a newline is not detected, all the character to the clMessage string
                             */
                            if(messageBuffer[0] == 0x0a){
                                isNewLine = 1;
                            }else{
                                clMessage += String.valueOf(messageBuffer[0]);
                            }
                        }
                        Log.d("clMessage",clMessage);
                        Toast.makeText(getApplicationContext(),clMessage,Toast.LENGTH_SHORT).show();

                        /*
                        Re-establish the prompt after receiving a message

                        This prompt may be removed when communication is finalized
                         */
                        clSocketWriter.write("\nSMS-sh - >");
                        clSocketWriter.flush();



                        /*
                        if the message received is equal to "exit", terminate the connection
                         */
                        if(clMessage.equals("exit")){
                        clSocketWriter.write("Good-Bye!\n");
                        clSocketWriter.flush();

                        break;

                        }
                        try {
                            /*
                            if the clMessage begins with sendsms, interpret this as a command to send an SMS message

                            command structure is as follows:

                                        sendsms[phonenumberhere][messagehere]
                            */
                            if (clMessage.substring(0, clMessage.indexOf("[")).equals("sendsms")) {
                                recipient = clMessage.substring(clMessage.indexOf("[") + 1, clMessage.indexOf("]"));

                            /*
                            check if the phone number is indeed only digits

                            the first if statement also checks if the number is a regular 10-digit phone number.
                            if it is, it adds "+1" to the beginning of the number. Android commits suicide if you don't do this.

                            if the phone number is non-numeric, refuse to send the message.

                            Looking to add contact lookup soon!
                             */
                                if (TextUtils.isDigitsOnly(recipient) || recipient.length() == 10) {

                                /*
                                extract the phonenumber from the message
                                 */
                                    recipient = "+1" + recipient;
                                } else if (!TextUtils.isDigitsOnly(recipient)) {
                                    recipient = null;
                                    clSocketWriter.write("Invalid phone number");
                                    clSocketWriter.flush();
                                }

                            /*
                            If a proper phone number is not provided, do not send the message
                             */
                                if (recipient != null) {

                                /*
                                extract the message body from the message
                                */
                                    smsMessage = clMessage.substring(clMessage.indexOf("]") + 2, clMessage.lastIndexOf("]"));

                                /*
                                send the message
                                 */
                                    sendSMSMessage(smsMessage, recipient);

                                /*
                                let the client know the message was sent
                                 */
                                    clSocketWriter.write("sent message \"" + smsMessage + "\" to \"" + recipient + "\"\nSMS-sh - >");
                                    clSocketWriter.flush();
                                }


                            }
                        }catch (StringIndexOutOfBoundsException e){
                            ;
                        }
                        /*
                        clean up the variables for the next read operation
                         */
                        recipient = "";
                        smsMessage = "";
                        clMessage = "";
                        isNewLine = 0;

                }
                clSocketWriter.close();
                clSocketReader.close();
                clientSocket.close();
            }catch (Exception e){
                e.printStackTrace();
                Toast.makeText(getApplicationContext(),"Could not write to client socket",Toast.LENGTH_SHORT).show();
            }
            Looper.loop();
        }

    }


}
