package com.xcomp.magiclamp.activity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.xcomp.magiclamp.R;
import com.xcomp.magiclamp.custom.ByteQueue;
import com.xcomp.magiclamp.custom.DialogFrag;
import com.xcomp.magiclamp.helpers.MqttHelper;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class SettingActivity extends AppCompatActivity implements DialogFrag.Communicator{

    private final String TAG = SettingActivity.class.getSimpleName();

    MqttHelper mqttHelper;
    WebView webview;
    EditText tfSsid;
    EditText tfPassword;
    android.widget.Button saveWifiBtn;
    Button audioStreamBtn;

    String deviceName;
    String deviceMacAddress;

    private BluetoothAdapter bluetoothAdapter;

    private String mDeviceAddress, mDeviceName, mDeviceID;

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private TextView Connection;
    private int REQUEST_ENABLE_BT = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        setTitle("Config Settings");

        tfSsid = (EditText) findViewById(R.id.tfWifiName);
        tfPassword = (EditText) findViewById(R.id.tfPassword);
        saveWifiBtn = (Button) findViewById(R.id.saveWifiBtn);
        saveWifiBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SettingActivity.this.onSaveWifiButtonClicked(v);
            }
        });

        audioStreamBtn = findViewById(R.id.audioStreamBtn);
        audioStreamBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SettingActivity.this.onAudioStreamButtonClicked(v);
            }
        });

        //webview = (WebView) findViewById(R.id.webview);
        //ImageView imgBackground = (ImageView) findViewById(R.id.img_background);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        //FrameLayout.LayoutParams layoutparams = (FrameLayout.LayoutParams) imgBackground.getLayoutParams();
        //layoutparams.height = height;
        //imgBackground.setLayoutParams(layoutparams);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        deviceName = getIntent().getStringExtra(SettingActivity.EXTRAS_DEVICE_NAME);
        deviceMacAddress = getIntent().getStringExtra(SettingActivity.EXTRAS_DEVICE_ADDRESS);

        Connection = (TextView) findViewById(R.id.connect);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        mDeviceID = intent.getExtras().getString("deviceID");

        Toast.makeText(this, "deviceID: " + mDeviceID, Toast.LENGTH_SHORT).show();

        //mqttHelper = new MqttHelper(getApplicationContext(), "", null);

        mWriteCharacteristics = new ArrayList<>();
        mWriteServices = new ArrayList<>();

        // Use this check to determine whether BLE is supported on the device. Then
        // you can selectively disable BLE-related features.
//        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
//            Toast.makeText(this, "BLE is not supported", Toast.LENGTH_SHORT).show();
//            finish();
//        }

        //Initialize Bluetooth Adapter
//        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
//        bluetoothAdapter = bluetoothManager.getAdapter();
//
//        // Ensures Bluetooth is available on the device and it is enabled. If not,
//        // displays a dialog requesting user permission to enable Bluetooth.
//        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()){
//            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
//        }


//        Intent settingInt = this.getIntent();
//
//        if (settingInt == null)
//        {
//            Toast.makeText(this, "Intent Null", Toast.LENGTH_SHORT).show();
//        }
//        else if (settingInt.hasExtra("id_main")){
//            deviceIDSetting = Objects.requireNonNull(settingInt.getStringExtra("id_main"));//getStringExtra("deviceID-Main");
//        }
//        else {
//            Toast.makeText(this, "What happened?", Toast.LENGTH_SHORT).show();
//        }

    }

    private byte[]  mReceiveBuffer = new byte[10 * 1024];
    private ByteQueue mByteQueue = new ByteQueue(10 * 1024);
    public static final int UPDATE = 1;
    /**
     * Look for new input from the ptty, send it to the terminal emulator.
     */
    private void update() {
        int bytesAvailable = mByteQueue.getBytesAvailable();
        if(bytesAvailable>0) {
            int bytesToRead = Math.min(bytesAvailable, mReceiveBuffer.length);
            try {
                int bytesRead = mByteQueue.read(mReceiveBuffer, 0, bytesToRead);
                //String stringRead = new String(mReceiveBuffer, 0, bytesRead);

                //displayData(mReceiveBuffer,bytesRead);
                //textView.append(new String(mReceiveBuffer), 0, bytesRead);

            /*if(mRecording) {
                this.writeLog( stringRead );
            }*/
            } catch (InterruptedException e) {
            }
        }
    }

    private void displayData(final byte[] bytes , final int size) {
        if (bytes != null) {
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final StringBuilder stringBuilder = new StringBuilder(bytes.length);
//                    if (rxhex) {
//
//                        for (int i=0;i<size;i++) {
//                            String hex ="0x" + String.format("%02X", bytes[i])+ "  ";
//                            stringBuilder.append(hex);
//                        }
//
//                        RxText.append(stringBuilder.toString());
//                        if (Log_flag)
//                            writeToFile(stringBuilder.toString(),stringBuilder.toString().length());
//                    } else {
//                        RxText.append(new String(bytes) ,0, size);
//                        if (Log_flag)
//                            writeToFile(new String(bytes), size);
//                    }
                }
            });
        }
    }

    /**
     * Our message handler class. Implements a periodic callback.
     */
    private final Handler mHandler = new Handler() {
        /**
         * Handle the callback message. Call our enclosing class's update
         * method.
         * */
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == UPDATE) {
                update();
            }
        }
    };

    private Runnable mCheckSize = new Runnable() {
        public void run() {
            update();
            mHandler.postDelayed(this, 500);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        initialize();

        final boolean result = connect(mDeviceAddress);
        mHandler.postDelayed(mCheckSize, 500);
    }

    @Override
    protected void onPause() {
        super.onPause();
        disconnect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        close();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                break;
        }
        return true;
    }

    public void onSaveWifiButtonClicked(View v){

        Log.e(TAG, "onSaveWifiButtonClicked: mac_address: " + this.deviceMacAddress);
        Log.e(TAG, "onSaveWifiButtonClicked: deviceName: " + this.deviceName);

        final String wifi_format = " - ";
        String ssid = tfSsid.getText().toString().trim();
        String password = tfPassword.getText().toString().trim();
        StringBuilder wifi_setting = new StringBuilder();
        wifi_setting.append(ssid);
        wifi_setting.append(wifi_format);
        wifi_setting.append(password);

        System.out.println("wifi_setting is " + wifi_setting);
        //webview.loadUrl("http://192.168.1.100/?ssid=" + ssid + "&pass=" + password);

//        String msg = "a test message";
//        byte[] encodedPayload = new byte[0];
//        try {
//            encodedPayload = msg.getBytes("UTF-8");
//            MqttMessage message = new MqttMessage(encodedPayload);
//            message.setId(5866);
//            message.setRetained(true);
//            message.setQos(0);
//            mqttHelper.mqttAndroidClient.publish("hientest",message);
//        }
//        catch (Exception e) {
//            e.printStackTrace();
//            return;
//        }

        if (mConnected) {
            if (!writecharselect) {
                selectkey = 0;
                ShowDialog("Select Write Characteristic", mWriteServices, mWriteCharacteristics);
            } else {
//                if (txhex) {
//
//                    //String newValue = sendtext.getText().toString().toLowerCase(Locale.getDefault());
//                    if (newValue.length() != 0) {
//                        if (newValue.contains("0x")) {
//                            byte[] dataToWrite = parseHexStringToBytes(newValue);
//                            if (dataToWrite.length != 0) {
//                                if (mWriteCharacteristic != null) {
//                                    writeDataToCharacteristic(mWriteCharacteristic, dataToWrite);
//                                }
//                            }
//                        } else
//                            showMessage("Enter char array eg. 0x1234");
//                    } else
//                        showMessage("Text box cannot be empty");

//                } else{}
                byte[] bytes = wifi_setting.toString().getBytes();
                if (bytes.length != 0) {
                    if (mWriteCharacteristic != null) {
                        writeDataToCharacteristic(mWriteCharacteristic, bytes);
//                        String text  = sendtext.getText().toString();
//                        RxText.append("\n\r" + text);
//                        if (Log_flag)
//                            writeToFile("\n\r" + text, text.length() + 2);
                        //sendtext.setText("");
                    }
                } else
                    showMessage("Please enter wifiname and password");
            }

        } else
            showMessage("Connect with device first");

//        if (wifi_setting.length() != 0) {
//            if (wifi_setting.contains("0x")) {
//                byte[] dataToWrite = parseHexStringToBytes(wifi_setting);
//                if (dataToWrite.length != 0) {
//                    if (mWriteCharacteristic != null) {
//                        writeDataToCharacteristic(mWriteCharacteristic, dataToWrite);
//                    }
//                }
//            } else
//                showMessage("Enter char array eg. 0x1234");
//        } else
//            showMessage("Text box cannot be empty");






    }

    public void onAudioStreamButtonClicked(View v){
        Intent intent = new Intent(this,AudioActivity.class);
        //intent.putExtras(Objects.requireNonNull(this.getIntent().getExtras()));
        intent.putExtra("deviceID", mDeviceID);
        startActivity(intent);

    }

    public void onScanBleButtonClicked(View v){
        Intent intent = new Intent(this,BleActivity.class);
        startActivity(intent);

    }


    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private String mBluetoothDeviceAddress;
    private BluetoothGattCharacteristic mWriteCharacteristic;
    private ArrayList<BluetoothGattCharacteristic> mWriteCharacteristics;
    int CHUNK_SIZE = 20;
    //Connection params
    private boolean mConnected = false;
    private boolean mConnecting = false;
    boolean writecharselect = false;
    // Key for setting read /write/notify
    int selectkey = 0;
    private ArrayList<BluetoothGattService> mWriteServices;
    List<BluetoothGattCharacteristic> chars = null;


    /* set new value for particular characteristic */
    public void writeDataToCharacteristic(final BluetoothGattCharacteristic ch, final byte[] dataToWrite) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null || ch == null) return;

        for (int start = 0; start < dataToWrite.length; start += CHUNK_SIZE) {
            int end = Math.min(start + CHUNK_SIZE, dataToWrite.length);
            byte[] chunk = Arrays.copyOfRange(dataToWrite, start, end);
            // first set it locally....
            ch.setValue(chunk);
            mBluetoothGatt.writeCharacteristic(ch);
            //while (flag) ;

        }
    }

    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }


    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            //return  (mBluetoothGatt.connect());

            mBluetoothGatt.close();
        }


        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mchatbleCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;

        return true;
    }

    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mConnected = false;
        mBluetoothGatt.disconnect();
    }

    private void Init_variables() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //mDeviceRssiView.setText("___db");
                ClearVariables();
            }
        });
    }

    private void ClearVariables() {

        writecharselect = false;
        //readcharselect = false;
        //notifycharselect = false;

        mWriteCharacteristics.clear();
        mWriteServices.clear();
//        mReadCharacteristics.clear();
//        mReadServices.clear();
//        mNotifyCharacteristics.clear();
//        mNotifyServices.clear();
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Connection.setText(resourceId);
            }
        });
    }

    boolean flag = true;
    private final BluetoothGattCallback mchatbleCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mConnected = true;
                mConnecting = false;
                Init_variables();
                //readPeriodicalyRssiValue(true);
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
                mBluetoothGatt.discoverServices();


            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {

                //readPeriodicalyRssiValue(false);
                Init_variables();
                mConnected = false;
                mConnecting = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();


                /*onDisconnectInit();*/
            } else if (newState == BluetoothProfile.STATE_CONNECTING) {
                mConnected = false;
                mConnecting = true;
                updateConnectionState(R.string.connecting);
                invalidateOptionsMenu();

            }
        }


        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    for (BluetoothGattService service : getSupportedGattServices()) {
                        //addService(service);
                        String serviceuuid = service.getUuid().toString();
                        chars = service.getCharacteristics();
                        if (chars.size() > 0) {
                            for (BluetoothGattCharacteristic characteristic : chars) {
                                if (characteristic != null) {
                                    String charuuid = characteristic.getUuid().toString();
                                    final int charaProp = characteristic.getProperties();
                                    final int read = charaProp & BluetoothGattCharacteristic.PROPERTY_READ;
                                    final int write = charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE;
                                    final int notify = charaProp & BluetoothGattCharacteristic.PROPERTY_NOTIFY;

//                                    if (read > 0) {
//                                        // If there is an active notification on a characteristic, clear
//                                        // it first so it doesn't update the data field on the user interface.
//                                           /*mReadCharacteristic = characteristic;
//                                                setCharacteristicNotification(mReadCharacteristic, false);
//                                                readCharacteristic(characteristic);*/
//                                        mReadServices.add(service);
//                                        mReadCharacteristics.add(characteristic);
//                                    }
//                                    if (notify > 0) {/*
//                                             mNotifyCharacteristic = characteristic;
//                                                setCharacteristicNotification(characteristic, true);*/
//
//                                        mNotifyServices.add(service);
//                                        mNotifyCharacteristics.add(characteristic);
//                                    }

                                    if (write > 0) {/*
                                            mWriteCharacteristic = characteristic;*/
                                        //addCharacteristic(characteristic);
                                        mWriteServices.add(service);
                                        mWriteCharacteristics.add(characteristic);
                                    }

                                }
                            }
                        }
                    }
                    //handle on savedinstance here
                   /* selectkey = 0;
                    SetCharacteristic(writechar);
                    selectkey = 1;
                    SetCharacteristic(readchar);
                    selectkey = 2;
                    SetCharacteristic(notifychar);*/
                }


            });
        }

//        @Override
//        public void onCharacteristicRead(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, int status) {
//
//            write(characteristic.getValue(),characteristic.getValue().length);
//
//            //displayData(characteristic.getValue());
//        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            flag = false;
        }

//        @Override
//        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
//
//            write(characteristic.getValue(),characteristic.getValue().length);
//            //displayData(characteristic.getValue());
//        }

//        @Override
//        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
//            if (status == BluetoothGatt.GATT_SUCCESS)
//                displayRSSI(rssi);
//        }
    };

    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }

    public void showMessage(String theMsg) {
        Toast msg = Toast.makeText(getBaseContext(), theMsg, Toast.LENGTH_SHORT);
        msg.show();
    }

    private void ShowDialog(String title, ArrayList<BluetoothGattService> service,
                            ArrayList<BluetoothGattCharacteristic> characteristics) {
        String[] serviceuuid = new String[service.size()];
        String[] charuuid = new String[service.size()];
        if (service.size() == characteristics.size()) {
            for (int i = 0; i < service.size(); i++) {
                serviceuuid[i] = service.get(i).getUuid().toString();
                charuuid[i] = characteristics.get(i).getUuid().toString();
            }
            final FragmentManager fm = getSupportFragmentManager();
            // FragmentManager fm = getSupportFragmentManager();
            final DialogFrag d = new DialogFrag();
            Bundle args = new Bundle();
            args.putStringArray("service", serviceuuid);
            args.putStringArray("char", charuuid);
            args.putString("title", title);
            d.setArguments(args);
            d.show(this.getFragmentManager(), "");
        }

    }

    public void onDialogMessage(String service, String chars) {

        //Toast.makeText(this, service+chars, Toast.LENGTH_SHORT).show();
        SetCharacteristic(chars);

    }

    private BluetoothGattCharacteristic getCharacteristic(String CharsUuid, ArrayList<BluetoothGattCharacteristic> characteristics) {
        for (int i = 0; i < characteristics.size(); i++) {
            if (characteristics.get(i).getUuid().toString().equals(CharsUuid))
                return characteristics.get(i);
        }
        return null;
    }

    private void SetCharacteristic(String chars) {
        switch (selectkey) {
            case 0: //write
                if (chars != null) {
                    writecharselect = true;
                    //RxText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
                    //RxText.setGravity(Gravity.BOTTOM);
                    mWriteCharacteristic = getCharacteristic(chars, mWriteCharacteristics);
                }
                break;
            case 1: //read
                if (chars != null) {
//                    readcharselect = true;
//                    RxText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
//                    // RxText.setGravity(Gravity.BOTTOM);
//                    mReadCharacteristic = getCharacteristic(chars, mReadCharacteristics);
//                    if (mReadCharacteristic != null)
//                        readCharacteristic(mReadCharacteristic);
                }
                break;
            case 2: //notify
                if (chars != null) {
//                    notifycharselect = true;
//                    RxText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
//                    //RxText.setGravity(Gravity.BOTTOM);
//                /*if (mNotifyCharacteristic != null)
//                    setCharacteristicNotification(mNotifyCharacteristic, false);//clear previous notify*/
//                    mNotifyCharacteristic = getCharacteristic(chars, mNotifyCharacteristics);
//                    if (mNotifyCharacteristic != null)
//                        setCharacteristicNotification(mNotifyCharacteristic, true);
                    break;
                }
        }
    }
}
