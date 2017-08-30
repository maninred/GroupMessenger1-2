package edu.buffalo.cse.cse486586.groupmessenger2;

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
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

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
    int proposed=0;
    int agreed=0;
    int receivedCount=0;
    int TotalCount=5;
    Queue<String> AllMsgs=new LinkedList<String>();
    boolean previousMessageSent=true;
    String nodeFailed="0000";


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
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
                editText.setText("");
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

                    String msg;
                    while((msg= serverBR.readLine())!=null){
                        break;
                    }
                    Log.i(TAG,"Message received at server of port "+myPort+" :"+msg);


                    //Output stream of server task
                    OutputStream serverOutputStream = socket.getOutputStream();
                    OutputStreamWriter serverOSW = new OutputStreamWriter(serverOutputStream);
                    BufferedWriter serverBW = new BufferedWriter(serverOSW);
                    serverBW.write("received"+"\n");
                    serverBW.flush();
                    serverBW.write((proposed++)+"\n");
                    serverBW.flush();
                    String ReceivedNumber;
                    while((ReceivedNumber=serverBR.readLine())!=null) {
                        int ReceivedAgreed = Integer.valueOf(ReceivedNumber);
                        Log.i("3rd way Message", "Received Areed: " + ReceivedAgreed);
                        if (ReceivedAgreed >= proposed - 1) {
                            publishProgress(msg);
                            socket.close();
                            Log.i(TAG,"Server socket is closed");
                        }
                        break;
                    }
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
            uriBuilder.scheme("content").authority("edu.buffalo.cse.cse486586.groupmessenger2.provider");

            ContentValues contentValue=new ContentValues();
            contentValue.put("key",String.valueOf(messageNo++));
            contentValue.put("value",strReceived);
            getContentResolver().insert(uriBuilder.build(),contentValue);
            return;
        }
    }


    private class ClientTask extends AsyncTask<String, Void, Void> {


        List<BufferedWriter> MessageReceivedBW=new LinkedList<BufferedWriter>();
        String[] ALL_PORTS={"11108","11112","11116","11120","11124"};

        @Override
        protected Void doInBackground(String... msgs) {
            AllMsgs.add(msgs[0]);
            while(!AllMsgs.isEmpty() && previousMessageSent) {
                String msgToSend = AllMsgs.poll();
                for (String remotePort : ALL_PORTS) {
                    if(remotePort.equals(nodeFailed)) continue;
                    try {


                        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                Integer.parseInt(remotePort));


                        boolean a = true;
                        while (a) {

                            //output stream of client task
                            OutputStream clientOutputStream = socket.getOutputStream();
                            OutputStreamWriter clientOSW = new OutputStreamWriter(clientOutputStream);
                            BufferedWriter clientBW = new BufferedWriter(clientOSW);
                            clientBW.write(msgToSend + "\n");
                            clientBW.flush();
                            previousMessageSent=false;


                            //input stream of client task
                            InputStream clientInputStream = socket.getInputStream();
                            InputStreamReader clientISR = new InputStreamReader(clientInputStream);
                            BufferedReader clientBR = new BufferedReader(clientISR);
                            String message = clientBR.readLine();
                            Log.i("Validation","2nd way message"+message+"");
                            if(message==null){
                                nodeFailed=remotePort;
                                TotalCount--;
                            }
                            else if (message.equals("received")) {
                                MessageReceivedBW.add(clientBW);
                                int receivedProposed=Integer.valueOf(clientBR.readLine());
                                Log.i("2nd way message","receivedProposed : "+receivedProposed+"");
                                agreed=Math.max(agreed,receivedProposed);
                                receivedCount++;
                                Log.i(TAG, "the message sent by port " + msgs[1] + " is received !!!");
                                Log.i("count",""+receivedCount);

                            }
                            if(receivedCount==TotalCount) {
                                proposed=agreed+1;
                                receivedCount=0;
                                allSend();
                                previousMessageSent=true;
                                socket.close();
                                Log.i(TAG, "client socket is closed");
                            }
                            break;
                        }

                    } catch (UnknownHostException e) {
                        Log.e(TAG, "ClientTask UnknownHostException");
                    } catch (IOException e) {
                        Log.e(TAG, "ClientTask socket IOException");
                    }
                }
            }
            return null;
        }




        public Void allSend(){
                for(BufferedWriter MessageReceivedBWTemp:MessageReceivedBW) {
                    try {
                        Log.i("3rd Way Message","Agreed message writing!!");
                        MessageReceivedBWTemp.write(agreed + "\n");
                        MessageReceivedBWTemp.flush();
                    } catch (IOException e) {
                        Log.e("3rd Way Message", "IO Exception");
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
