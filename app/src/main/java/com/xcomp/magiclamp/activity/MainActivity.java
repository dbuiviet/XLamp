package com.xcomp.magiclamp.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.AsyncTask;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.ActionBar;
import android.os.Bundle;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import com.nabto.api.NabtoAndroidAssetManager;
import com.nabto.api.NabtoApi;
import com.nabto.api.NabtoStatus;
import com.nabto.api.NabtoTunnelState;
import com.nabto.api.RpcResult;
import com.nabto.api.Session;
import com.nabto.api.Tunnel;
import com.nabto.api.TunnelInfoResult;

import com.xcomp.magiclamp.R;
import com.xcomp.magiclamp.helpers.MqttHelper;


import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class MainActivity extends BaseActivity {

    static private String TAG = MainActivity.class.getSimpleName();

    public String mqttTopic = "/magiclamp/qos0";

    //public static final String EXTRAS_DEVICE_ID = "DEVICE_ID";

    private boolean MQTT_Enable = false; //using MQTT to turn On/Off

    private boolean Nabto_OnOff = false;  //using Nabto session to turn On/Off

    private WebView webView;

    MqttHelper mqttHelper;

    String deviceID = "";

    Collection<String> deviceNames;

    View navbar_custom_view;

    //GridViewAdapter_Images gridViewAdapter;
    //RecyclerView gridRecycleView;
    //private SwipeRefreshLayout mSwipeRefreshLayout;

//    ArrayList<ImageModel> imageModelsList;
//    ArrayList<SessionModel> sessionModels;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = (WebView) findViewById(R.id.webview);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionbar = getSupportActionBar();
//        actionbar.setDisplayHomeAsUpEnabled(true);
//        actionbar.setDisplayShowTitleEnabled(false);
        assert actionbar != null;
        actionbar.setDisplayShowCustomEnabled(true);
//        actionbar.setHomeAsUpIndicator(R.drawable.ico_menu);

        LayoutInflater inflator = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        navbar_custom_view = inflator.inflate(R.layout.navbar_custom_view, null);
        ActionBar.LayoutParams layout = new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT);
        actionbar.setCustomView(navbar_custom_view);
        setupNavbarActions(navbar_custom_view);


        if (MQTT_Enable){
            mqttHelper = new MqttHelper(this, mqttTopic, new MqttCallbackExtended() {
                @Override
                public void connectComplete(boolean reconnect, String serverURI) {

                }

                @Override
                public void connectionLost(Throwable cause) {

                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    Log.e(TAG, "messageArrived on topic: " + topic + " --- message: " + message);
                    //MainActivity.this.getAllSession();
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {

                }
            });
        }


        SharedPreferences preferences = this.getSharedPreferences("paired_devices", Context.MODE_PRIVATE);
        deviceID = preferences.getString("deviceID", "");

        //initialize Nabto serivce
        //initNabto();
    }

    public void setupNavbarActions(View navbar_custom_view) {
        navbar_custom_view.findViewById(R.id.bt_setting).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.this.onMenuButtonClicked(v);
            }
        });

        navbar_custom_view.findViewById(R.id.bt_turnon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.this.onTurnOnButtonClicked(v);
            }
        });

        navbar_custom_view.findViewById(R.id.bt_turnoff).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.this.onTurnOffButtonClicked(v);
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.setting_menu, menu);
//        return true;
//    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        Log.i("deviceID: ", this.deviceID.toString());

        int id = item.getItemId();

        if (id == R.id.setting_item) {

            Intent intent = new Intent(this,SettingActivity.class);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }




    static final String TOGGLE_XML =
            "<unabto_queries>" +
                    "<query name='toggle_on_off.json' id='20010'>" + //id does matter!
                    "<request>" +
                    "<parameter name='activated' type='uint8' />" +
                    "</request>" +
                    "<response format='json'>" +
//                    "<parameter name='status' type='uint8'/>" +
//                    "<parameter name='fingerprint' type='raw' representation='hex'/>" +
//                    "<parameter name='name' type='raw'/>" +
//                    "<parameter name='permissions' type='uint32'/>" +
                    "<parameter name='activated' type='uint8' />" +
                    "</response>" +
                    "</query>" +
                    "</unabto_queries>";

    public void onTurnOnButtonClicked(View v) {
        if (MQTT_Enable){
            String msg = "on";
            byte[] encodedPayload = new byte[0];
            try {
                encodedPayload = msg.getBytes("UTF-8");
                MqttMessage message = new MqttMessage(encodedPayload);
                message.setId(5866);
                message.setRetained(true);
                message.setQos(0);
                //mqttHelper.mqttAndroidClient.publish(mqttTopic,message);
                mqttHelper.mqttAndroidClient.publish("/magiclamp/qos0", message);
            }
            catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
        else if (Nabto_OnOff){
//            api = new NabtoApi(new NabtoAndroidAssetManager(this));
//
//            status = api.startup();
//            if (status != NabtoStatus.OK) {
//                throw new RuntimeException("Nabto startup failed with status " + status);
//            }
//
//            status = api.createSelfSignedProfile(certUser, certPassword);
//            if (status != NabtoStatus.OK) {
//                throw new RuntimeException("Nabto createSelfSignedProfile failed with status " + status);
//            }

//            session = api.openSession(certUser, certPassword);
//            if (session.getStatus() != NabtoStatus.OK) {
//                throw new RuntimeException("Nabto open session failed with status " + session.getStatus());
//            }

            initNabto();

            RpcResult result = api.rpcSetDefaultInterface(TOGGLE_XML, session);
//            RpcResult result = api.rpcSetInterface(tunnelHost, TOGGLE_XML, session);

            rpcUrl = "nabto://" + tunnelHost + "/toggle_on_off.json?activated=" + LAMP_ON;

            if (result.getStatus() == NabtoStatus.OK) {
                appendText("Invoking RPC URL ...");

                new RpcTask().execute();
            } else {
                if (result.getStatus() == NabtoStatus.FAILED_WITH_JSON_MESSAGE) {
                    appendText("Nabto set RPC default interface failed: " + result.getJson());

                } else {
                    appendText("Nabto set RPC default interface failed with status " + result.getStatus());
                }
                //initNabto();
            }

            navbar_custom_view.findViewById(R.id.bt_turnon).setEnabled(false);
            navbar_custom_view.findViewById(R.id.bt_turnoff).setEnabled(true);

        }

    }

    public void onTurnOffButtonClicked(View v) {
        if (MQTT_Enable){
            String msg = "off";
            byte[] encodedPayload = new byte[0];
            try {
                encodedPayload = msg.getBytes("UTF-8");
                MqttMessage message = new MqttMessage(encodedPayload);
                message.setId(5866);
                message.setRetained(true);
                message.setQos(0);
                //mqttHelper.mqttAndroidClient.publish(mqttTopic,message);
                mqttHelper.mqttAndroidClient.publish("/magiclamp/qos0", message);
            }
            catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
        else if (Nabto_OnOff){
//            api = new NabtoApi(new NabtoAndroidAssetManager(this));
//
//            status = api.startup();
//            if (status != NabtoStatus.OK) {
//                throw new RuntimeException("Nabto startup failed with status " + status);
//            }
//
//            status = api.createSelfSignedProfile(certUser, certPassword);
//            if (status != NabtoStatus.OK) {
//                throw new RuntimeException("Nabto createSelfSignedProfile failed with status " + status);
//            }

//            session = api.openSession(certUser, certPassword);
//            if (session.getStatus() != NabtoStatus.OK) {
//                throw new RuntimeException("Nabto open session failed with status " + session.getStatus());
//            }

            RpcResult result = api.rpcSetDefaultInterface(TOGGLE_XML, session);
//            RpcResult result = api.rpcSetInterface(tunnelHost, TOGGLE_XML, session);

            rpcUrl = "nabto://" + tunnelHost + "/toggle_on_off.json?activated=" + LAMP_OFF;

            if (result.getStatus() == NabtoStatus.OK) {
                appendText("Invoking RPC URL ...");

                new RpcTask().execute();
            } else {
                if (result.getStatus() == NabtoStatus.FAILED_WITH_JSON_MESSAGE) {
                    appendText("Nabto set RPC default interface failed: " + result.getJson());

                } else {
                    appendText("Nabto set RPC default interface failed with status " + result.getStatus());
                }
                //initNabto();
            }

            closeNabto();

            navbar_custom_view.findViewById(R.id.bt_turnon).setEnabled(true);
            navbar_custom_view.findViewById(R.id.bt_turnoff).setEnabled(false);
        }
    }

    private void closeNabto(){
        status = api.tunnelClose(tunnel);
        if (status != NabtoStatus.OK){
            appendText("Close tunnel returned " + status);
        }

        status = api.closeSession(session);
        if (status != NabtoStatus.OK){
            appendText("Close session returned " + status);
        }

        api.shutdown();
    }

    public void onMenuButtonClicked(View v) {
        Intent intent = new Intent(this, BleActivity.class);

        intent.putExtra("deviceID", this.deviceID);
        startActivity(intent);
    }


    /*Nabto Video Stream Impl*/
    private int LAMP_ON = 1;
    private int LAMP_OFF = 0;
    private NabtoApi api;
    private Session session;
    private NabtoStatus status;
    private Tunnel tunnel;
    static final private String certUser = "XLamp";
    static final private String certPassword = "123456";
    private String tunnelHost = "bozgdcjz.dyfagy.trial.nabto.net";
    private String rpcUrl = ""; //"nabto://" + tunnelHost + "/pair_with_device.json?name=" + certUser;

    static final String INTERFACE_XML =
            "<unabto_queries>" +
                    "<query name='pair_with_device.json' id='11010'>" +
                    "<request>" +
                    "<parameter name='name' type='raw'/>" +
                    "</request>" +
                    "<response format='json'>" +
                    "<parameter name='status' type='uint8'/>" +
                    "<parameter name='fingerprint' type='raw' representation='hex'/>" +
                    "<parameter name='name' type='raw'/>" +
                    "<parameter name='permissions' type='uint32'/>" +
                    "</response>" +
                    "</query>" +
                    "</unabto_queries>";

    public void initClicked(View view) {
        try {
            initNabto();
        } catch (Exception e) {
            appendText("ERROR: " + e.getMessage());
        }
    }

    private void initNabto() {
        api = new NabtoApi(new NabtoAndroidAssetManager(this));

        ((TextView) findViewById(R.id.textView)).setText("");

        status = api.startup();
        if (status != NabtoStatus.OK) {
            throw new RuntimeException("Nabto startup failed with status " + status);
        }

        status = api.createSelfSignedProfile(certUser, certPassword);
        if (status != NabtoStatus.OK) {
            throw new RuntimeException("Nabto createSelfSignedProfile failed with status " + status);
        }

        session = api.openSession(certUser, certPassword);

        if (session.getStatus() != NabtoStatus.OK) {
            throw new RuntimeException("Nabto open session failed with status " + session.getStatus());
        }

        appendText("Nabto client SDK successfully initialized, version " + api.versionString());

//        if (deviceID == ""){
//            deviceNames = api.getLocalDevices();
//
//            Log.e(TAG, "init: deviceID: " + deviceNames.toString());
//
//            if (deviceNames.size() > 0){
//                deviceID = deviceNames.iterator().next();
//            }
//        }

        //deviceID = ((ArrayList<String>)deviceNames).get(0);

        //tunnelHost = this.deviceID;

        deviceID = tunnelHost;

        rpcUrl = "nabto://" + tunnelHost + "/pair_with_device.json?name=" + certUser;

    }

    private void appendText(String text) {
        ((TextView) findViewById(R.id.textView)).append("\n" + text + "\n");
    }

    public void stopClicked(View view) {
        if (this.api != null) {
            stop();
        } else {
            appendText("Not initialized yet!");
            //initNabto();
        }
    }

    //NabtoStatus status;
    private void stop() {
//        status = api.tunnelClose(tunnel);
//        if (status != NabtoStatus.OK){
//            appendText("Close tunnel returned " + status);
//        }
//
//        status = api.closeSession(session);
//        if (status != NabtoStatus.OK){
//            appendText("Close session returned " + status);
//        }
//
//        api.shutdown();

        closeNabto();

        webView.stopLoading();
    }

    public void livestreamClicked(View view) {
        Toast.makeText(this, deviceID, Toast.LENGTH_SHORT).show();

//        if (status == NabtoStatus.API_NOT_INITIALIZED || status == NabtoStatus.INVALID_SESSION || this.api == null){
//            initNabto();
//        }

        if (this.api != null) {
            livestreamTunnel();
        } else {
            appendText("Not initialized yet!");
        }
    }

    private void livestreamTunnel() {
        appendText("Opening tunnel ...");
        startProgress();
        new TunnelTask().execute();
    }

    private void startProgress() {
        ((ProgressBar) findViewById(R.id.progressBar)).setVisibility(View.VISIBLE);
    }

    private void stopProgress() {
        ((ProgressBar) findViewById(R.id.progressBar)).setVisibility(View.INVISIBLE);
    }

    private class TunnelTask extends AsyncTask<Void, Void, TunnelInfoResult> {
        protected void onPostExecute(TunnelInfoResult result) {
            stopProgress();
            if (result.getStatus() == NabtoStatus.OK) {
                appendText("Nabto tunnel open attempt completed, state is [" + result.getTunnelState() + "]");
                if (result.getTunnelState().equals(NabtoTunnelState.REMOTE_P2P)) {
                    Toast.makeText(MainActivity.this, "remote p2p", Toast.LENGTH_LONG).show();
                }
                else if (result.getTunnelState().equals(NabtoTunnelState.REMOTE_RELAY)) {
                    Toast.makeText(MainActivity.this, "remote REMOTE_RELAY", Toast.LENGTH_LONG).show();
                }
                else if (result.getTunnelState().equals(NabtoTunnelState.REMOTE_RELAY_MICRO)) {
                    Toast.makeText(MainActivity.this, "remote REMOTE_RELAY_MICRO", Toast.LENGTH_LONG).show();
                }
                else if (result.getTunnelState().equals(NabtoTunnelState.LOCAL)) {
                    Toast.makeText(MainActivity.this, "remote LOCAL", Toast.LENGTH_LONG).show();
                }
                if (result.getTunnelState().equals(NabtoTunnelState.REMOTE_P2P) ||
                        result.getTunnelState().equals(NabtoTunnelState.REMOTE_RELAY) ||
                        result.getTunnelState().equals(NabtoTunnelState.REMOTE_RELAY_MICRO) ||
                        result.getTunnelState().equals(NabtoTunnelState.LOCAL)) {

                    String url = "http://127.0.0.1:" + result.getPort() + "/";
                    Log.e(TAG, "onPostExecute: url: " + url);

                    String webviewData = "<div align=\"center\"> <img src=\""+ url +"\"/></div>";
                    //webView.getDrawableState();
                    webView.loadData(webviewData, "text/html", "UTF-8");
                }
            } else {
                appendText("Nabto tunnel open attempt failed with status " + result.getStatus());
                //webView.refreshDrawableState();
                //initNabto();
            }
        }

        @Override
        protected TunnelInfoResult doInBackground(Void... voids) {
            Log.e("TunnelInfoResult", "doInBackground: ");

            tunnel = api.tunnelOpenTcp(0, tunnelHost, "127.0.0.1", 8081, session);
//            tunnel = api.tunnelOpenTcp(0, tunnelHost, "127.0.0.1", 8081, session);
//            tunnel = api.tunnelOpenTcp(80, tunnelHost, "localhost", 8081, session);

//            return api.tunnelWait(tunnel, 100, 3000);
            return api.tunnelWait(tunnel, 300, 5000);

        }
    }

    public void pairClicked(View view) {
        Toast.makeText(this, deviceID, Toast.LENGTH_SHORT).show();

        if (this.api != null) {
            pairRpc();
        } else {
            appendText("Not initialized yet!");
        }
    }

    private void pairRpc() {

        RpcResult result = api.rpcSetDefaultInterface(INTERFACE_XML, session);

        if (result.getStatus() == NabtoStatus.OK) {
            appendText("Invoking RPC URL ...");
            startProgress();
            new RpcTask().execute();
        } else {
            if (result.getStatus() == NabtoStatus.FAILED_WITH_JSON_MESSAGE) {
                appendText("Nabto set RPC default interface failed: " + result.getJson());

            } else {
                appendText("Nabto set RPC default interface failed with status " + result.getStatus());
            }
            //initNabto();
        }
    }

    private class RpcTask extends AsyncTask<Void, Void, RpcResult> {

        protected void onPostExecute(RpcResult result) {

            SharedPreferences.Editor preference = MainActivity.this.getSharedPreferences("paired_devices", Context.MODE_PRIVATE).edit();
            preference.putString("deviceID", MainActivity.this.deviceID);
            preference.apply();

            stopProgress();
            if (result.getStatus() == NabtoStatus.OK) {
                appendText("Nabto invoke RPC OK: " + result.getJson());

            } else if (result.getStatus() == NabtoStatus.FAILED_WITH_JSON_MESSAGE) {
                appendText("Nabto invoke RPC failed: " + result.getJson());


            } else {
                appendText("Nabto invoke RPC failed with status " + result.getStatus());

            }
        }

        @Override
        protected RpcResult doInBackground(Void... voids) {
            return api.rpcInvoke(rpcUrl, session);
        }
    }


    /*Nabto Video Stream Impl*/

}
