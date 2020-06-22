package com.xcomp.magiclamp.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.DataSetObserver;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.nabto.api.NabtoAndroidAssetManager;
import com.nabto.api.NabtoApi;
import com.nabto.api.NabtoStatus;
import com.nabto.api.RpcResult;
import com.nabto.api.Session;
import com.nabto.api.Stream;
import com.nabto.api.StreamReadResult;
import com.xcomp.magiclamp.R;
import com.xcomp.magiclamp.utils.ADPCM;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class AudioActivity extends BaseActivity {
    // UI
    private EditText editText;
    private Button openStreamButton;
    private Button closeStreamButton;
    private Button scanDeviceButton;
    private Button toggleButton;
    private ListView deviceList;
    private TextView textView;

    // Nabto API
    private NabtoApi nabtoApi;
    private Session session;
    private Stream stream;

    // Audio
    private AudioRecord record;
    private AudioTrack track;

    private final static int BITRATE = 8000; //16000;
    private final static double RECORD_BUFFER_TIME = 4;  // seconds
    private final static double PLAY_BUFFER_TIME = 4;  // seconds

    static final private String certUser = "XLamp";// "dbuiviet@gmail.com";
    static final private String certPassword = "123456"; // "9121981130q97";

    private ADPCM adpcm;
    private boolean streaming = false;

    String deviceID = "";
//    ListAdapter adapter;
//    List<String> list;
    String rpcUrl = "";
    String tunnelHost = "";

    public AudioActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio);

        ActivityCompat.requestPermissions(AudioActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);


        textView = (TextView) findViewById(R.id.textView);

        // Init UI
        editText = (EditText) findViewById(R.id.editTextDeviceId);

        //deviceID = Objects.requireNonNull(getIntent().getExtras()).getString("deviceID"); //.getStringExtra("deviceID-Setting");
        //Log.i("Audio deviceID: ", deviceID);
//        deviceID =  Objects.requireNonNull(getIntent().getStringExtra("id_main"));

//        deviceID = "bozgdcjz.dyfagy.trial.nabto.net";
        deviceID = "prrwysed.dyfagy.trial.nabto.net";
        editText.setText(deviceID);

        //tunnelHost = deviceID;
        initNabto();

        openStreamButton = (Button) findViewById(R.id.buttonOpenStream);
        openStreamButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //String deviceId = editText.getText().toString();
                startStreaming(deviceID);
            }
        });

        closeStreamButton = (Button) findViewById(R.id.buttonCloseStream);
        closeStreamButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stopStreaming();
            }
        });

        scanDeviceButton = (Button) findViewById(R.id.buttonScanDevice);
        scanDeviceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanDevices();
            }
        });

        toggleButton = (Button) findViewById(R.id.buttonToggleOnOff);
        toggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleOnOffDevice();
            }
        });

//        rpcUrl = "nabto://" + tunnelHost; // + "/pair_with_device.json?name=" + certUser;

        adpcm = new ADPCM();

    }

    private void initNabto(){
        // Initialize Nabto and open session with guest account
        nabtoApi = new NabtoApi(new NabtoAndroidAssetManager(this));
        Log.v(this.getClass().toString(), "Nabto API version: " + nabtoApi.versionString());
        NabtoStatus status = nabtoApi.startup();

        if (status != NabtoStatus.OK) {
            throw new RuntimeException("Nabto startup failed with status " + status);
        }

//        status = nabtoApi.createSelfSignedProfile(certUser, certPassword);
        status = nabtoApi.createSelfSignedProfile("guest", "");

        if (status != NabtoStatus.OK) {
            throw new RuntimeException("Nabto createSelfSignedProfile failed with status " + status);
        }

//        session = nabtoApi.openSession(certUser, certPassword);
        session = nabtoApi.openSession("guest", "");


        //textView.append(session.getStatus().toString());

        if (session.getStatus() != NabtoStatus.OK) {

            throw new RuntimeException("Nabto open session failed with status " + session.getStatus());
        }

        tunnelHost = deviceID;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    Toast.makeText(AudioActivity.this, "Permission denied to record audio", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }


    /**
     * Start streaming.
     * @param deviceId Device ID to connect to.
     */
    private void startStreaming(final String deviceId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable(){
                    @Override
                    public void run() {
                        editText.setEnabled(false);
                        openStreamButton.setEnabled(false);
                    }
                });

                if (!openStream(deviceId)) {
                    stopStreaming();
                    return;
                }

                if (!sendCommand("audio\n")) {
                    stopStreaming();
                    return;
                }

                streaming = true;

                startRecording();
                startPlaying();

                runOnUiThread(new Runnable(){
                    @Override
                    public void run() {
                        closeStreamButton.setEnabled(true);
                    }
                });
            }
        }).start();
    }

    /**
     * Open stream.
     * @param deviceId Device ID to connect to.
     * @return Returns TRUE if stream was successfully opened.
     */
    private boolean openStream(final String deviceId) {
        // Open stream
        Log.v(this.getClass().toString(), "Opening stream to " + deviceId + "...");
        stream = nabtoApi.streamOpen(deviceId, session);
        if (stream == null) {
            Log.v(this.getClass().toString(), "Failed to open stream!");
            return false;
        }
        NabtoStatus status = stream.getStatus();
        Log.v(this.getClass().toString(), "Stream open status: " + status);
        if (status != NabtoStatus.OK) {
            Log.v(this.getClass().toString(), "Failed to open stream!");
            return false;
        }
        return true;
    }

    /**
     * Send command to the device.
     * @param command Command to send
     * @return Returns TRUE if the command was accepted by the device.
     */
    private boolean sendCommand(final String command) {
        // Send command
        Log.v(this.getClass().toString(), "Send command '" + command + "'...");
        NabtoStatus status = nabtoApi.streamWrite(stream, command.getBytes());
        Log.v(this.getClass().toString(), "Command write status: " + status);
        if (status != NabtoStatus.OK) {
            Log.v(this.getClass().toString(), "Failed to send command!");
            return false;
        }
        // Verify command result
        final StreamReadResult result = nabtoApi.streamRead(stream);
        status = result.getStatus();
        Log.v(this.getClass().toString(), "Command result read status: " + status);
        if (status != NabtoStatus.OK) {
            Log.v(this.getClass().toString(), "Failed to read command result!");
            return false;
        }
        final String received = new String(result.getData());
        if (!received.trim().equals("+")) {
            Log.v(this.getClass().toString(), "Failed to invoke command. Result was: " + received);
            return false;
        }
        return true;
    }

    /**
     * Stop streaming.
     */
    private void stopStreaming() {
        new Thread(new Runnable(){
            @Override
            public void run() {
                runOnUiThread(new Runnable(){
                    @Override
                    public void run() {
                        closeStreamButton.setEnabled(false);
                    }
                });
                streaming = false;
                closeStream();

                runOnUiThread(new Runnable(){
                    @Override
                    public void run() {
                        editText.setEnabled(true);
                        openStreamButton.setEnabled(true);
                    }
                });
            }
        }).start();
    }

    /**
     * Close stream.
     */
    private void closeStream() {
        adpcm.resetEncoder();
        adpcm.resetDecoder();

        if (stream != null && stream.getStatus() != NabtoStatus.STREAM_CLOSED) {
            NabtoStatus status = nabtoApi.streamClose(stream);
            Log.v(this.getClass().toString(), "Stream close status: " + status);
        }

        if (nabtoApi != null){
            nabtoApi.closeSession(session);
            nabtoApi.shutdown();
        }
    }

    /**
     * Start recording & sending thread.
     */
    private void startRecording() {
        //adpcm.resetEncoder();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.v(this.getClass().toString(), "Start recording... ");

                int minBufferSize = AudioRecord.getMinBufferSize(
                        BITRATE,
                        AudioFormat.CHANNEL_IN_STEREO,
                        AudioFormat.ENCODING_PCM_16BIT);
//                        AudioFormat.CHANNEL_IN_MONO,
//                        AudioFormat.ENCODING_PCM_8BIT);


                int bufferSizeInBytes = (int)(RECORD_BUFFER_TIME * 2 * 2 * BITRATE);
                if (bufferSizeInBytes < minBufferSize)
                    bufferSizeInBytes = minBufferSize;

                record = new AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        BITRATE,
                        AudioFormat.CHANNEL_IN_STEREO,
                        AudioFormat.ENCODING_PCM_16BIT,
//                        AudioFormat.CHANNEL_IN_MONO,
//                        AudioFormat.ENCODING_PCM_8BIT,
                        bufferSizeInBytes);

                record.startRecording();

                int pos = 0;
                short[] recordChunk = new short[2048]; // -> 1024kB send chunk after encoding
                //double startTime = System.currentTimeMillis();

                while (streaming) {
                    pos += record.read(recordChunk, pos, recordChunk.length - pos);
                    assert (pos <= recordChunk.length );

                    if(pos == recordChunk.length) {
                        pos = 0;

                        final byte[] bytes = adpcm.encode(recordChunk);
                        NabtoStatus status = nabtoApi.streamWrite(stream, bytes);

                        /*double endTime = System.currentTimeMillis();
                        int kilobytes = bytes.length / 1024;
                        double seconds = (endTime-startTime) / 1000.0;
                        double bandwidth = (kilobytes / seconds);  //kilobytes-per-second (kBs)
                        startTime = endTime;
                        Log.v(this.getClass().toString(), "sending = " + bandwidth + " kBs");*/

                        if (status == NabtoStatus.INVALID_STREAM || status == NabtoStatus.STREAM_CLOSED) {
                            stopStreaming();
                            break;
                        } else if (status != NabtoStatus.OK && status != NabtoStatus.BUFFER_FULL) {
                            Log.v(this.getClass().toString(), "Write error: " + status);
                            stopStreaming();
                            break;
                        }
                    }
                }

                record.stop();
                record.release();
                record = null;

                Log.v(this.getClass().toString(), "Stopped recording... ");
            }
        }).start();
    }

    /**
     * Start receiving and playing thread.
     */
    private void startPlaying() {
        //adpcm.resetEncoder();

        new Thread(new Runnable() {
            @Override
            public void run() {
                int minBufferSize = AudioTrack.getMinBufferSize(
                        BITRATE,
                        AudioFormat.CHANNEL_IN_STEREO,
                        AudioFormat.ENCODING_PCM_16BIT);
//                        AudioFormat.CHANNEL_IN_MONO,
//                        AudioFormat.ENCODING_PCM_8BIT);

                int bufferSizeInBytes = (int)(PLAY_BUFFER_TIME * 2 * 2 * BITRATE);
                if (bufferSizeInBytes < minBufferSize)
                    bufferSizeInBytes = minBufferSize;

                track = new AudioTrack(AudioManager.STREAM_MUSIC,
                        BITRATE,
                        AudioFormat.CHANNEL_OUT_STEREO,
                        AudioFormat.ENCODING_PCM_16BIT,
//                        AudioFormat.CHANNEL_IN_MONO,
//                        AudioFormat.ENCODING_PCM_8BIT,
                        bufferSizeInBytes,
                        AudioTrack.MODE_STREAM);

                track.play();

                int bufferWriteFrame = 0;
                //double startTime = System.currentTimeMillis();

                while (streaming) {
                    StreamReadResult result = nabtoApi.streamRead(stream);
                    NabtoStatus status = result.getStatus();
                    if (status == NabtoStatus.INVALID_STREAM || status == NabtoStatus.STREAM_CLOSED) {
                        stopStreaming();
                        break;
                    } else if (status != NabtoStatus.OK) {
                        Log.v(this.getClass().toString(), "Read error: " + status);
                        stopStreaming();
                        break;
                    }

                    /*double endTime = System.currentTimeMillis();
                    int kilobytes = result.getData().length / 1024;
                    double seconds = (endTime-startTime) / 1000.0;
                    double bandwidth = (kilobytes / seconds);  //kilobytes-per-second (kBs)
                    startTime = endTime;
                    Log.v(this.getClass().toString(), "receiving = " + bandwidth + " kBs");*/

                    final short[] playChunk = adpcm.decode(result.getData());
//                    final  byte[] playChunk = result.getData();
                    track.write(playChunk, 0, playChunk.length);

                    bufferWriteFrame += playChunk.length / 2;
                    int bufferPlaybackFrame = track.getPlaybackHeadPosition();
                    double bufferFilled = (bufferWriteFrame - bufferPlaybackFrame) / (bufferSizeInBytes/4.0);
                    Log.v(this.getClass().toString(), "Play buffer filled: " + bufferFilled + "%");
                }

                track.stop();
                track.release();
                track = null;
            }
        }).start();
    }


    static final String PAIR_DEVICE_XML =
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

    /**
     * Start scanning devices
     */
    private void scanDevices(){
//        while (nabtoApi.getLocalDevices().iterator().hasNext()){
//            textView.append("\n"+ Arrays.toString(nabtoApi.getLocalDevices().toArray())+"\n");
//        }

        rpcUrl = "nabto://" + tunnelHost + "/pair_with_device.json?name=" + certUser;

        RpcResult result = nabtoApi.rpcSetInterface(tunnelHost, PAIR_DEVICE_XML, session);

        textView.append("\n"+ Arrays.toString(nabtoApi.getLocalDevices().toArray())+"\n");

        if (result.getStatus() == NabtoStatus.OK) {
            textView.append("Invoking RPC URL ...");

            new RpcTask().execute();
        } else {
            if (result.getStatus() == NabtoStatus.FAILED_WITH_JSON_MESSAGE) {
                textView.append("Nabto set RPC default interface failed: " + result.getJson());

            } else {
                textView.append("Nabto set RPC default interface failed with status " + result.getStatus());
            }
            //initNabto();
        }

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


    private int data = 1;
    private void toggleOnOffDevice(){
        //initNabto();

        //RpcResult result = nabtoApi.rpcSetDefaultInterface(SEND_DATA_XML, session);
        RpcResult result = nabtoApi.rpcSetInterface(tunnelHost, TOGGLE_XML, session);

        rpcUrl = "nabto://" + tunnelHost + "/toggle_on_off.json?activated=" + data;

        textView.append("\n"+rpcUrl+"\n");

        if (data == 1){
            data = 0;
        }
        else{
            data = 1;
        }

        if (result.getStatus() == NabtoStatus.OK) {
            textView.append("Invoking RPC URL ...");

            new RpcTask().execute();
        } else {
            if (result.getStatus() == NabtoStatus.FAILED_WITH_JSON_MESSAGE) {
                textView.append("Nabto set RPC default interface failed: " + result.getJson());

            } else {
                textView.append("Nabto set RPC default interface failed with status " + result.getStatus());
            }
            //initNabto();
        }

    }

    private class RpcTask extends AsyncTask<Void, Void, RpcResult> {

        protected void onPostExecute(RpcResult result) {

//            SharedPreferences.Editor preference = MainActivity.this.getSharedPreferences("paired_devices", Context.MODE_PRIVATE).edit();
//            preference.putString("deviceID", MainActivity.this.deviceID);
//            preference.commit();

//            stopProgress();
            if (result.getStatus() == NabtoStatus.OK) {
                textView.append("Nabto invoke RPC OK: " + result.getJson());

            } else if (result.getStatus() == NabtoStatus.FAILED_WITH_JSON_MESSAGE) {
                textView.append("Nabto invoke RPC failed: " + result.getJson());


            } else {
                textView.append("Nabto invoke RPC failed with status " + result.getStatus());

            }
        }

        @Override
        protected RpcResult doInBackground(Void... voids) {
            return nabtoApi.rpcInvoke(rpcUrl, session);
        }
    }
}
