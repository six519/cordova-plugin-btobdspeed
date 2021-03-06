package com.ferdinandsilva.android;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;
import android.content.Context;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class BTOBDSpeedPlugin extends CordovaPlugin {
    public static final String TAG = "BTOBDSpeedPlugin";
    public static Context thisContext;

    public static final int NONE_STATE = 0;
    public static final int LISTENING_STATE = 1;
    public static final int CONNECTED_STATE = 2;

    public static final String SERVICE_ID = "fa87c0d0-afac-11de-8a39-0800200c9a66";
    public static final String SERVICE_NAME = "BlueTeeth";

    public static BluetoothAdapter btAdapter;
    private ListenThread listenThread;
    private ConnectedThread connectedThread;
    private int CURRENT_STATE = NONE_STATE;

    private CordovaInterface thisCordovaInterface;
    private CallbackContext thisCallbackContext;

    public BTOBDSpeedPlugin() {
    }

    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        BTOBDSpeedPlugin.thisContext = cordova.getActivity().getApplicationContext();
        thisCordovaInterface = cordova;
    }

    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        thisCallbackContext = callbackContext;

        if ("start".equals(action)) {
            btAdapter = BluetoothAdapter.getDefaultAdapter();

            if(btAdapter == null) {
                Toast.makeText(thisCordovaInterface.getActivity().getApplicationContext(), "Bluetooth not available!", Toast.LENGTH_LONG).show();
            } else {
                if(!btAdapter.isEnabled()) {
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

                    thisCordovaInterface.getActivity().startActivityForResult(enableIntent, 3);
                } else {
                    listenThread = new ListenThread();
                    listenThread.start();
                }
            }
        } else {
            return false;
        }

        return true;    
    }

    public class ConnectedThread extends Thread {

        private final BluetoothSocket thisbtSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public ConnectedThread(BluetoothSocket socket) {
            thisbtSocket = socket;
            InputStream tmpInput = null;
            OutputStream tmpOutput = null;

            try {
                tmpInput = socket.getInputStream();
                tmpOutput = socket.getOutputStream();
            }catch(IOException e) {

            }

            inputStream = tmpInput;
            outputStream = tmpOutput;
        }

        public void cancel() {
            if(thisbtSocket != null) {
                try {
                    thisbtSocket.close();
                }catch(IOException e) {

                }
            }
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while(CURRENT_STATE == CONNECTED_STATE) {
                try{
                    bytes = inputStream.read(buffer);

                    String msgString = new String(buffer, 0, bytes);
                    outputStream.write(buffer, 0 , bytes);
                    PluginResult result = new PluginResult(PluginResult.Status.OK, msgString);
                    result.setKeepCallback(true);
                    thisCallbackContext.sendPluginResult(result);
                    //thisCallbackContext.success(msgString);

                }catch(IOException e) {
                    //DISCONNECTED

                    CURRENT_STATE = LISTENING_STATE;
                    if(listenThread != null) {
                        listenThread.cancel();
                        listenThread = null;
                    }
                    listenThread = new ListenThread();
                    listenThread.start();
                }

            }
        }
    }

    public class ListenThread extends Thread {
        private BluetoothServerSocket thisbtServerSocket;
        private int ctr = 0;
        public ListenThread() {
            CURRENT_STATE = LISTENING_STATE;

            try {
                //thisbtServerSocket = btAdapter.listenUsingInsecureRfcommWithServiceRecord(BTOBDSpeedPlugin.SERVICE_NAME, UUID.fromString(BTOBDSpeedPlugin.SERVICE_ID));
                thisbtServerSocket = btAdapter.listenUsingRfcommWithServiceRecord(BTOBDSpeedPlugin.SERVICE_NAME, UUID.fromString(BTOBDSpeedPlugin.SERVICE_ID));
            }catch(IOException e) {
                thisbtServerSocket = null;
            }
        }

        public void cancel() {
            if(thisbtServerSocket != null) {
                try {
                    thisbtServerSocket.close();
                }catch(IOException e) {

                }
            }
        }

        public void run() {
            BluetoothSocket btSocket;

            while(CURRENT_STATE == LISTENING_STATE) {
                try {
                    btSocket = thisbtServerSocket.accept();
                } catch (IOException e) {
                    break;
                }

                if(btSocket != null) {
                    CURRENT_STATE = CONNECTED_STATE;
                    if(connectedThread != null) {
                        connectedThread.cancel();
                        connectedThread = null;
                    }

                    connectedThread = new ConnectedThread(btSocket);
                    connectedThread.start();
                }
            }
        }
    }

}