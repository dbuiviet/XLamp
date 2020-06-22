package com.xcomp.magiclamp.activity;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import com.google.android.material.navigation.NavigationView;
import androidx.legacy.app.ActivityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.xcomp.magiclamp.R;

import com.xcomp.magiclamp.adapter.CustomListAdapter;
import com.xcomp.magiclamp.custom.SearchElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class BleActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, CustomListAdapter.customButtonListener {
    ListView devicelist;
    //private Button skipScanButton;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_ALL = 4;

    private BluetoothAdapter mBluetoothAdapter;

    private BluetoothLeScanner mBluetoothLeScanner;


    //private ScanCallback mScanCallback;

    private boolean mScanning = false;
    private Handler mHandler;
    // Stops scanning after 30 seconds.
    private static long SCAN_PERIOD = 30000;

    String[] Permission = {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.BLUETOOTH};

    Timer scanTimer;
    CustomListAdapter adapter;
    //SharedPreferences sharedpreferences;
    ArrayList<SearchElement> device_list = new ArrayList<SearchElement>();
    int progress_result = 0;
    String name_filter, mac;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble);
        //Set navigation drawer
        //Set screen orientation
//        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        //
        devicelist = (ListView) findViewById(R.id.devicelist);
        configDeviceList();


        if (!hasPermissions(this, Permission)) {
            ActivityCompat.requestPermissions(this, Permission, REQUEST_ALL);
        }

        getSupportActionBar().setTitle("Scan Device");
        //getActionBar().setBackgroundDrawable(new ColorDrawable(0xff20b2aa));
        mHandler = new Handler();
        //getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);

//        skipScanButton = (Button) findViewById(R.id.buttonSkipScan);
//        skipScanButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                onSkipScanClicked(v);
//            }
//        });

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE not supported", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "BLE not supported", Toast.LENGTH_SHORT).show();
            finish();
        }

        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()){
            Intent enableBtIntent = new Intent(mBluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

    }

    private void configDeviceList() {
        adapter = new CustomListAdapter(this, device_list);

        adapter.setCustomButtonListner(this);
        devicelist.setAdapter(adapter);
        devicelist.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //Toast.makeText(BleActivity.this, "onItemClick", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(BleActivity.this, SettingActivity.class);
                SearchElement anElement = (SearchElement) BleActivity.this.adapter.getItem(position);
//                intent.putExtra(ChatActivity.EXTRAS_DEVICE_NAME, anElement.getName());
//                intent.putExtra(ChatActivity.EXTRAS_DEVICE_ADDRESS, anElement.getMac_address());
                intent.putExtra(SettingActivity.EXTRAS_DEVICE_NAME, anElement.getName());
                intent.putExtra(SettingActivity.EXTRAS_DEVICE_ADDRESS, anElement.getMac_address());
                intent.putExtra("deviceID", getIntent().getExtras().getString("deviceID"));
                if (mScanning) {
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);

                    mScanning = false;
                }
                startActivity(intent);
            }
        });
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        Map<String, Integer> perms = new HashMap<>();
        // Initialize the map with both permissions
//        perms.put(Manifest.permission.READ_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);
//        perms.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);
        perms.put(Manifest.permission.ACCESS_COARSE_LOCATION, PackageManager.PERMISSION_GRANTED);
        perms.put(Manifest.permission.BLUETOOTH, PackageManager.PERMISSION_GRANTED);
        perms.put(Manifest.permission.BLUETOOTH_ADMIN, PackageManager.PERMISSION_GRANTED);
        perms.put(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        switch (requestCode) {

            case REQUEST_ALL: {
                if (grantResults.length > 0) {
                    for (int i = 0; i < permissions.length; i++)
                        perms.put(permissions[i], grantResults[i]);

//                    if (perms.get(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
//                            && perms.get(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
//                            && perms.get(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
//                            && perms.get(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
//
//                    }
//                    if (perms.get(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || perms.get(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//                        builder.setTitle("Functionality limited");
//                        builder.setMessage("Since storage access has not been granted, this app will not be able to do logging.");
//                        builder.setPositiveButton(android.R.string.ok, null);
//                        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
//
//                            @Override
//                            public void onDismiss(DialogInterface dialog) {
//                            }
//
//                        });
//                        builder.show();
//                    }
                    if (perms.get(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                            && perms.get(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                            && perms.get(Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED
                            && perms.get(Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {

                        builder.setTitle("Functionality limited");
                        builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                        builder.setPositiveButton(android.R.string.ok, null);
                        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                finish();
                            }
                        });
                        builder.show();
                    }
                }
            }
            return;

        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            start_scan = true;
            StartScanTimer();
            //scanLeDevice(true);
        }


    }

    boolean start_scan = true;

    private void StartScanTimer() {

        start_scan = true;
        scanTimer = new Timer();
        scanTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (start_scan)
                            scanLeDevice(true);
                    }
                });
            }
        }, 0, SCAN_PERIOD);

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            scanTimer.purge();
            start_scan =false;
            scanLeDevice(false);
        }
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    final StringBuilder stringBuilder = new StringBuilder(scanRecord.length);
                    stringBuilder.append("0x");
                    for (byte byteChar : scanRecord) {
                        String hex =String.format("%02X", byteChar);
                        stringBuilder.append(hex);
                    }


                    boolean device_added = false;
                    SearchElement wp = new SearchElement(device.getName(), device.getAddress(), rssi,stringBuilder.toString().trim());


                    adapter.filter("", "", 0);
                    for (int i = 0; i < device_list.size(); i++) {
                        if (device_list.get(i).getMac_address().equals(device.getAddress())) {
                            device_list.remove(i);
                            device_list.add(i, wp);
                            device_added = true;
                        }
                    }
                    if (!device_added)
                        device_list.add(wp);

                    adapter.update(device_list);
                }
            });
        }
    };

    private void scanLeDevice(final boolean enable) {
//        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);


                    //search.smoothToHide();
                    //searchtext.setText("No BLE Device found");
                    //invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            if(!mScanning) {
                mScanning = true;
                invalidateOptionsMenu();
            }

            mBluetoothAdapter.startLeScan(mLeScanCallback);
            //Log.e("BLE-DEBUG", "mLeScanCallback: " + mLeScanCallback.toString());

            //searchtext.setText("Searching for BLE device");
            //search.smoothToShow();

        } else {
            mScanning = false;
            //searchtext.setText("No BLE Device found");
            mBluetoothAdapter.stopLeScan(mLeScanCallback);

            //search.smoothToHide();
            invalidateOptionsMenu();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.device_scan_menu, menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                StartScanTimer();
                //scanLeDevice(true);
                break;
            case R.id.menu_stop:
                scanTimer.purge();
                start_scan = false;
                scanLeDevice(false);
                break;
        }
        return true;
    }

    @Override
    public void onButtonClickListener(int position, String name, String mac, int rssi) {
//        final Intent intent = new Intent(getBaseContext(), ChatActivity.class);
//        intent.putExtra(ChatActivity.EXTRAS_DEVICE_NAME, name);
//        intent.putExtra(ChatActivity.EXTRAS_DEVICE_ADDRESS, mac);

//        final Intent intent = new Intent(getBaseContext(), SettingActivity.class);
//        intent.putExtra(SettingActivity.EXTRAS_DEVICE_NAME, name);
//        intent.putExtra(SettingActivity.EXTRAS_DEVICE_ADDRESS, mac);
//
        if (mScanning) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);

            mScanning = false;
        }
//        startActivity(intent);
    }

    @Override
    public void onRawDataClickListner(int position, String raw_data) {
        // Toast.makeText(ScanActivity.this, "Raw data at " + position, Toast.LENGTH_SHORT).show();

//        final Dialog dialog = new Dialog(this);
//        dialog.setContentView(R.layout.raw_dialog);
//        dialog.setTitle("Custom Dialog");
//        TextView text = (TextView) dialog.findViewById(R.id.textDialog);
//        text.setText(raw_data);
//        dialog.show();
//        FancyButton declineButton = (FancyButton) dialog.findViewById(R.id.declineButton);
//        declineButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                // Close dialog
//                dialog.dismiss();
//            }
//        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        return false;
    }

//    private void onSkipScanClicked(View v){
//        Intent intent = new Intent(this, SettingActivity.class);
//        startActivity(intent);
//        finish();
//    }

}
