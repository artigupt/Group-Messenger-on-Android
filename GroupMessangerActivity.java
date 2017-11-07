package edu.buffalo.cse.cse486586.groupmessenger1;


import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 */
public class GroupMessengerActivity extends Activity {

    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final int SERVER_PORT = 10000;
    static final String[] REMOTE_PORT = {"11108", "11112", "11116", "11120", "11124"};

    int timeKeeper = 0;
    TextView textView = null;
    Uri uri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        /* Build URI */
        uri = OnPTestClickListener.buildUri(OnPTestClickListener.URI_SCHEME, OnPTestClickListener.URI);

        /* TextView: To display all messages.
        *  EditText: To type in a message */
        textView = (TextView) findViewById(R.id.textView1);
        textView.setMovementMethod(new ScrollingMovementMethod());
        final EditText editText = (EditText) findViewById(R.id.editText1);
        final Button sendButton = (Button) findViewById(R.id.button4);
       // final Button showDataButton = (Button) findViewById(R.id.showdatabutton);

        try {
            /* Create a server socket and a thread (AsyncTask) that listens on the server port */
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
            e.printStackTrace();
            return;
        }

        /*
         * Register an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = editText.getText().toString() + "\n";
                editText.setText(""); // Reset the input box.
                Log.d("DEBUG", "Send button clicked. Message: " + msg);

                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg);
            }
        });

//        showDataButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                StringBuilder allMessages = new StringBuilder("");
//                for (int i=0; i <= timeKeeper; i++) {
//                    Cursor resultCursor = getContentResolver().query(uri, null, String.valueOf(i), null, null);
//                    String cursorText = getTextFromCursor(resultCursor);
//                    if (cursorText != null && cursorText.length() > 0)
//                        allMessages.append(String.valueOf(i) + " ==--> " + cursorText + "\n");
//                }
//
//                Log.d("DEBUG", "Messages here...\n" + allMessages.toString());
//                Log.d("DEBUG", "Timekeeper: " + timeKeeper);
//            }
//        });

        /*
         * Register an OnKeyListener for the input box. OnKeyListener is an event handler that
         * processes each key event. The purpose of the following code is to detect an enter key
         * press event, and create a client thread so that the client thread can send the string
         * in the input box over the network.
         */
        editText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    String msg = editText.getText().toString() + "\n";
                    editText.setText(""); // Reset the input box.
                    Log.d("DEBUG", "Sending message: " + msg);

                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg);
                    return true;
                }
                return false;
            }
        });

        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(textView, getContentResolver(), this));

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    /**
     * ClientTask is an AsyncTask that should send a string over the network.
     * It is created by ClientTask.executeOnExecutor() call whenever OnKeyListener.onKey() detects
     * an enter key press event.
     *
     * @author stevko
     */
    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            Socket socket = null;
            DataOutputStream outputStream;

            try {
                /* Message-text */
                String msgToSend = msgs[0];

                for (int remoteHostNumber = 0; remoteHostNumber < REMOTE_PORT.length; remoteHostNumber++) {
                    String remotePort = REMOTE_PORT[remoteHostNumber];

                    socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(remotePort));

                    /* Client code that sends out a message. */
                    if (socket != null) {
                        outputStream = new DataOutputStream(socket.getOutputStream());
                        if (outputStream != null)
                            outputStream.writeBytes(msgToSend);
                    }
                }
            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException");
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (socket != null)
                        socket.close();
                } catch (IOException e) {
                    Log.e(TAG, "ClientTask socket IOException");
                }
            }
            return null;
        }
    }

    /**
     * ServerTask is an AsyncTask that should handle incoming messages. It is created by
     * ServerTask.executeOnExecutor() call in SimpleMessengerActivity.
     * <p/>
     * Please make sure you understand how AsyncTask works by reading
     * http://developer.android.com/reference/android/os/AsyncTask.html
     *
     * @author stevko
     */
    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];

            /* Server code that receives messages and passes them to onProgressUpdate(). */
            Socket clientSocket;
            DataInputStream inputStream;
            String messages[] = new String[1000];

            try {
                while (true) {
                    clientSocket = serverSocket.accept();
//                    inputStream = new DataInputStream(clientSocket.getInputStream());

                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    String message = bufferedReader.readLine();

//                    if (inputStream != null && (messages[0] = inputStream.readLine()) != null) {


                /* Write what was received to the content provider */
                    ContentValues contentValue = new ContentValues();
                    contentValue.put(OnPTestClickListener.KEY_FIELD, Integer.toString(timeKeeper));
                    contentValue.put(OnPTestClickListener.VALUE_FIELD, message);
                    getContentResolver().insert(uri, contentValue);

                        /* Increment the timer */
                    timeKeeper++;

                    publishProgress(messages);
//                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        protected void onProgressUpdate(String... strings) {

            /* Extract the timestamp and message from the received string */
//            String strReceived = strings[0].trim();
//            if (strReceived != null) {
//
//                String message = strReceived;
//
//                /* Write what was received to the content provider */
//                ContentValues contentValue = new ContentValues();
//                contentValue.put(OnPTestClickListener.KEY_FIELD, Integer.toString(timeKeeper));
//                contentValue.put(OnPTestClickListener.VALUE_FIELD, message);
//                getContentResolver().insert(uri, contentValue);

                /* Refresh the content of the TextView */
            showChatOnTextView();

                /* Increment the timer */
//                timeKeeper++;
//            }

            return;
        }
    }

    private void showChatOnTextView() {
        /* Refresh the content of the TextView */
        StringBuilder allMessages = new StringBuilder("");
        for (int i=0; i <= timeKeeper; i++) {
            Cursor resultCursor = getContentResolver().query(uri, null, String.valueOf(i), null, null);
            String cursorText = getTextFromCursor(resultCursor);
            if (cursorText != null && cursorText.length() > 0)
                allMessages.append(cursorText + "\n");
        }

        /* Display all the messages onto the text-view */
        textView.setText(allMessages.toString());
    }

    public String getTextFromCursor(Cursor cursor) {
        String messageText = null;
        if (cursor != null) {
            int keyIndex = cursor.getColumnIndex(OnPTestClickListener.KEY_FIELD);
            int valueIndex = cursor.getColumnIndex(OnPTestClickListener.VALUE_FIELD);

            if (keyIndex != -1 && valueIndex != -1) {

                cursor.moveToFirst();
                if (!(cursor.isFirst() && cursor.isLast())) {
                    Log.e(TAG, "Wrong number of rows in cursor");
                } else {
                    messageText = cursor.getString(valueIndex);
                }
            }
            cursor.close();
        }
        return messageText;
    }
}