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
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;

/**
 * Created by Mostsb on 21/01/2015.
 */
public class SMSService extends Service {
    public static Socket smsSocket;
    public static ServerSocket serverSocket;
    public static ServerSocket serverReaderSocket;
    public static SMSReceiver smsReceiver = new SMSReceiver();
    public static OutputStreamWriter smsWriter;
    public static Socket[] openSockets = new Socket[1024];
    public static Socket[] openReaderSockets = new Socket[1024];
    public static String currentSMS = "";

    @Override
    public void onCreate(){


        //Attempt to create a server socket
        Thread serverThread = new Thread(){

            int socketServerPort = Integer.parseInt(MainActivity.hostPort);

            public void run(){
                Looper.prepare();
                try {
                    serverSocket = new ServerSocket(socketServerPort);

                    int socketCounter = 0;
                    while(true){
                        Socket clSocket = serverSocket.accept();
                        openSockets[socketCounter++] = clSocket;
                        String connectedMessage = "Connected to client at " + String.valueOf(clSocket.getInetAddress()) + " on port " + String.valueOf(clSocket.getPort());

                        //Toast.makeText(getApplicationContext(),connectedMessage,Toast.LENGTH_SHORT).show();

                        ClCommunication newClient = new ClCommunication(clSocket);
                        newClient.start();


                    }


                }catch (Exception e){
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "Could not create the server socket, or socket closed",Toast.LENGTH_SHORT).show();
                }



                Looper.loop();
            }

        };
        serverThread.start();

        //Attempt to create a server READER socket
        Thread serverReaderThread = new Thread(){

            int socketServerPort = Integer.parseInt(MainActivity.hostPort)+1;

            public void run(){
                Looper.prepare();
                try {
                    serverReaderSocket = new ServerSocket(socketServerPort);

                    int socketCounter = 0;
                    while(true){
                        Socket clSocket = serverReaderSocket.accept();
                        openReaderSockets[socketCounter++] = clSocket;
                        String connectedMessage = "Connected to client at " + String.valueOf(clSocket.getInetAddress()) + " on port " + String.valueOf(clSocket.getPort());

                        //Toast.makeText(getApplicationContext(),connectedMessage,Toast.LENGTH_SHORT).show();

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
        //Attempt to create a client socket
        Thread initSocket = new Thread(){
            public void run() {
                Looper.prepare();
                try {


                    InetAddress remoteAddr = InetAddress.getByName(MainActivity.hostAddr);
                    smsSocket = new Socket(remoteAddr, Integer.parseInt(MainActivity.hostPort));
                    smsWriter = new OutputStreamWriter(smsSocket.getOutputStream());

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "Could not connect to server", Toast.LENGTH_SHORT).show();

                }
                Looper.loop();
            }
        };

        initSocket.start();
        */
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
        unregisterReceiver(smsReceiver);
        try {
            serverSocket.close();
            //close all open client sockets
            for(int i = 0; i < openSockets.length; i++){
                if(!(openSockets[i].isClosed())) {
                    openSockets[i].close();
                }
            }
            //smsSocket.close();
            //smsWriter.close();
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


    public static void pushSMS(String sms){

        currentSMS = sms;
        Log.d("sms", currentSMS);

        /*for(int i = 0; i < openSockets.length;i++){
            try {
                if(!openSockets[i].isClosed()){

                    OutputStreamWriter clWriter = new OutputStreamWriter(openSockets[i].getOutputStream());
                    clWriter.write(currentSMS);
                    clWriter.close();
                }
            }catch (Exception e){
                e.printStackTrace();
                }

        }*/
       // try {


            //smsWriter.write(sms);
            //smsWriter.flush();

       // }catch(Exception e){
       //     //Toast.makeText(, "Could not write to socket", Toast.LENGTH_SHORT).show();
       //     Log.d("SOCKET", "Could not write to socket");
       // }


    }

    public class ClSMSPush extends Thread{

        private Socket clientSocket = null;
        public ClSMSPush(Socket clSocket){
            super("ClSMSPush");
            clientSocket = clSocket;
        }

        public void run(){
            Looper.prepare();
            OutputStreamWriter clWriter;

            try {
                clWriter = new OutputStreamWriter(clientSocket.getOutputStream());

                clWriter.write(currentSMS);

                clWriter.close();

            }catch (Exception e){
                e.printStackTrace();
            }




            Looper.loop();
        }
    }
    public class ClCommunication extends Thread{

            //Server Communication Thread
            private Socket clientSocket = null;
            public ClCommunication(Socket clSocket){
                super("ClCommunication");
                clientSocket = clSocket;
            }
            public void run(){
                Looper.prepare();

                //Send a welcome message after connection is established
                try {
                    OutputStreamWriter clSocketWriter = new OutputStreamWriter(clientSocket.getOutputStream());
                    InputStreamReader clSocketReader = new InputStreamReader(clientSocket.getInputStream());
                    char[] messageBuffer = new char[2];
                    int isNewLine = 0;
                    String clMessage = "";
                    String oldSMS = "";
                    String welcomeMessage = "Connection Established\n";
                    clSocketWriter.write(welcomeMessage);
                    clSocketWriter.flush();
                    while(true){
                        if(currentSMS != oldSMS){
                            clSocketWriter.write(currentSMS);
                            clSocketWriter.flush();
                            oldSMS = currentSMS;
                        }

                        /*while(isNewLine == 0) {
                            clSocketReader.read(messageBuffer, 0, 1);
                            if(messageBuffer[0] == 0x0a){
                                isNewLine = 1;
                            }else{
                                clMessage += String.valueOf(messageBuffer[0]);
                            }
                        }
                        /*Log.d("clMessage",clMessage);
                        Toast.makeText(getApplicationContext(),clMessage,Toast.LENGTH_SHORT).show();

                        clSocketWriter.write("\n");
                        clSocketWriter.flush();
                        if(clMessage.equals("exit")){
                            clSocketWriter.write("Good-Bye!\n");
                            clSocketWriter.flush();
                            clSocketWriter.close();
                            clSocketReader.close();
                            clientSocket.close();
                            break;

                        }
                        clMessage = "";
                        isNewLine = 0;
                        */
                    }
                }catch (Exception e){
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(),"Could not write to client socket",Toast.LENGTH_SHORT).show();
                }
                Looper.loop();
            }

    }
    public class ClReaderCommunication extends Thread{

        //Server Communication Thread
        private Socket clientSocket = null;
        public ClReaderCommunication(Socket clSocket){
            super("ClReaderCommunication");
            clientSocket = clSocket;
        }
        public void run(){
            Looper.prepare();

            //Send a welcome message after connection is established
            try {
                OutputStreamWriter clSocketWriter = new OutputStreamWriter(clientSocket.getOutputStream());
                InputStreamReader clSocketReader = new InputStreamReader(clientSocket.getInputStream());
                char[] messageBuffer = new char[2];
                int isNewLine = 0;
                String clMessage = "";

                String welcomeMessage = "Connection Established\nSMS-sh - >";
                clSocketWriter.write(welcomeMessage);
                clSocketWriter.flush();
                while(true){


                        while(isNewLine == 0) {
                            clSocketReader.read(messageBuffer, 0, 1);
                            if(messageBuffer[0] == 0x0a){
                                isNewLine = 1;
                            }else{
                                clMessage += String.valueOf(messageBuffer[0]);
                            }
                        }
                        Log.d("clMessage",clMessage);
                        Toast.makeText(getApplicationContext(),clMessage,Toast.LENGTH_SHORT).show();

                        clSocketWriter.write("\nSMS-sh - >");
                        clSocketWriter.flush();
                        if(clMessage.equals("exit")){
                            clSocketWriter.write("Good-Bye!\n");
                            clSocketWriter.flush();
                            clSocketWriter.close();
                            clSocketReader.close();
                            clientSocket.close();
                            break;

                        }
                        clMessage = "";
                        isNewLine = 0;

                }
            }catch (Exception e){
                e.printStackTrace();
                Toast.makeText(getApplicationContext(),"Could not write to client socket",Toast.LENGTH_SHORT).show();
            }
            Looper.loop();
        }

    }


}
