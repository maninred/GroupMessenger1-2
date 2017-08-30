package edu.buffalo.cse.cse486586.groupmessenger1;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {
    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final int SERVER_PORT = 10000;
    static String myPort="";

    int messageNo=0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        final Button sendButton=(Button) findViewById(R.id.button4);
        final EditText editText = (EditText) findViewById(R.id.editText1);

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());


        //setting on click listener for send button
        sendButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick(View v){
                String msg = editText.getText().toString().trim();
                editText.setText("");
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
            }
        });


        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }

        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));
        
        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            Socket socket;
            boolean check=true;
            while (check) {
                try {
                    //Input stream of server task
                    socket = serverSocket.accept();
                    InputStream serverInputStream =socket.getInputStream();
                    InputStreamReader serverISR=new InputStreamReader(serverInputStream);
                    BufferedReader serverBR = new BufferedReader(serverISR);
                    String msg = serverBR.readLine();
                    Log.i(TAG,"Message received at server of port "+myPort+" :"+msg);
                    publishProgress(msg);

                    //Output stream of server task
                    OutputStream serverOutputStream = socket.getOutputStream();
                    OutputStreamWriter serverOSW = new OutputStreamWriter(serverOutputStream);
                    BufferedWriter serverBW = new BufferedWriter(serverOSW);
                    serverBW.write("received"+"\n");
                    serverBW.flush();
                    socket.close();
                    Log.i(TAG,"Server socket is closed");
                } catch (IOException e) {
                    Log.e(TAG,"IO exception in the server");
                }
            }
            return null;
        }


        protected void onProgressUpdate(String... strings) {
            String strReceived = strings[0].trim();
            TextView textView = (TextView) findViewById(R.id.textView1);
            textView.append(strReceived+"\n");
            Uri.Builder uriBuilder = new Uri.Builder();
            uriBuilder.scheme("content").authority("edu.buffalo.cse.cse486586.groupmessenger1.provider");

            ContentValues contentValue=new ContentValues();
            contentValue.put("key",String.valueOf(messageNo++));
            contentValue.put("value",strReceived);
            getContentResolver().insert(uriBuilder.build(),contentValue);
            return;
        }
    }


    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            String[] ALL_PORTS={"11108","11112","11116","11120","11124"};

            for(String remotePort:ALL_PORTS) {
                try {


                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(remotePort));

                    String msgToSend = msgs[0];

                    boolean a = true;
                    while (a) {

                        //output stream of client task
                        OutputStream clientOutputStream = socket.getOutputStream();
                        OutputStreamWriter clientOSW = new OutputStreamWriter(clientOutputStream);
                        BufferedWriter clientBW = new BufferedWriter(clientOSW);
                        clientBW.write(msgToSend + "\n");
                        clientBW.flush();

                        //input stream of client task
                        InputStream clientInputStream = socket.getInputStream();
                        InputStreamReader clientISR = new InputStreamReader(clientInputStream);
                        BufferedReader clientBR = new BufferedReader(clientISR);
                        String message = clientBR.readLine();
                        if (message.equals("received")) {
                            Log.i(TAG, "---------------------the message sent by port " + msgs[1] + " is received !!!");
                            socket.close();
                            Log.i(TAG, "client socket is closed");
                            break;
                        }
                    }

                } catch (UnknownHostException e) {
                    Log.e(TAG, "ClientTask UnknownHostException");
                } catch (IOException e) {
                    Log.e(TAG, "ClientTask socket IOException");
                }
            }
            return null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }
}
