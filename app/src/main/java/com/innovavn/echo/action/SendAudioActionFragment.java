package com.innovavn.echo.action;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.innovavn.bluetoothutils.BluetoothA2dpUtils;
import com.willblaschko.android.alexa.requestbody.DataRequestBody;
import com.innovavn.echo.BuildConfig;
import com.innovavn.echo.R;
import com.willblaschko.android.recorderview.RecorderView;

import java.io.IOException;

import ee.ioc.phon.android.speechutils.AudioRecorder;
import ee.ioc.phon.android.speechutils.BluetoothAudioRecorder;
import ee.ioc.phon.android.speechutils.RawAudioRecorder;
import okio.BufferedSink;


/**
 * @author will on 5/30/2016.
 */

public class SendAudioActionFragment extends BaseListenerFragment {

    private static final String TAG = "SendAudioActionFragment";

    private final static int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    private static final int AUDIO_RATE = 16000;
    private RawAudioRecorder recorder;
    private BluetoothAudioRecorder bluetoothRecorder;
    private BluetoothA2dpUtils mA2dpService;
    private RecorderView recorderView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_action_audio, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recorderView = (RecorderView) view.findViewById(R.id.recorder);
        mA2dpService = new BluetoothA2dpUtils(getActivity());
        mA2dpService.startBluetooth();
        recorderView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(recorder == null) {
//                if(bluetoothRecorder == null) {
                    startListening();
                } else {
                    stopListening();
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                    Manifest.permission.RECORD_AUDIO)) {
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        MY_PERMISSIONS_REQUEST_RECORD_AUDIO);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_RECORD_AUDIO: {
                // If request is cancelled, the result arrays are empty.
                if (!(grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED)){
                    getActivity().getSupportFragmentManager().beginTransaction().remove(this).commit();
                }
            }

        }
    }

    @Override
    public void onStop() {
        super.onStop();
        //tear down our recorder on stop
        if(recorder != null){
//        if(bluetoothRecorder != null){
            recorder.stop();
//            bluetoothRecorder.stop();
            recorder.release();
//            bluetoothRecorder.release();
            recorder = null;
//            bluetoothRecorder = null;
        }
        if (mA2dpService != null) {
            mA2dpService.stopBluetooth();
        }
    }

    @Override
    public void startListening() {
        if(recorder == null){
//        if(bluetoothRecorder == null){
            recorder = new RawAudioRecorder(AUDIO_RATE);
//            bluetoothRecorder = new BluetoothAudioRecorder(AUDIO_RATE);
        }
        recorder.start();
//        bluetoothRecorder.start();
        alexaManager.sendAudioRequest(requestBody, getRequestCallback());
    }

    private boolean checkRecordPermission()
    {
        String permission = Manifest.permission.RECORD_AUDIO;
        int res = getContext().checkCallingOrSelfPermission(permission);
        return (res == PackageManager.PERMISSION_GRANTED);
    }

    private DataRequestBody requestBody = new DataRequestBody() {
        @Override
        public void writeTo(BufferedSink sink) throws IOException {
            while (recorder != null && recorder.getState() != AudioRecorder.State.ERROR && !recorder.isPausing()) {
                if(recorder != null) {
                    final float rmsdb = recorder.getRmsdb();
                    if(recorderView != null) {
                        recorderView.post(new Runnable() {
                            @Override
                            public void run() {
                                recorderView.setRmsdbLevel(rmsdb);
                            }
                        });
                    }
                    if(sink != null && recorder != null) {
                        sink.write(recorder.consumeRecording());
                    }
                    if(BuildConfig.DEBUG){
                        Log.i(TAG, "Received audio");
                        Log.i(TAG, "RMSDB: " + rmsdb);
                    }
                }

                try {
                    Thread.sleep(25);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            stopListening();
        }

    };

//    private DataRequestBody requestBody = new DataRequestBody() {
//        @Override
//        public void writeTo(BufferedSink sink) throws IOException {
//            while (bluetoothRecorder != null && bluetoothRecorder.getState() != AudioRecorder.State.ERROR && !bluetoothRecorder.isPausing()) {
//                if(bluetoothRecorder != null) {
//                    final float rmsdb = recorder.getRmsdb();
//                    if(recorderView != null) {
//                        recorderView.post(new Runnable() {
//                            @Override
//                            public void run() {
//                                recorderView.setRmsdbLevel(rmsdb);
//                            }
//                        });
//                    }
//                    if(sink != null && bluetoothRecorder != null) {
//                        sink.write(bluetoothRecorder.consumeRecording());
//                    }
//                    if(BuildConfig.DEBUG){
//                        Log.i(TAG, "Received audio");
//                        Log.i(TAG, "RMSDB: " + rmsdb);
//                    }
//                }
//
//                try {
//                    Thread.sleep(25);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//            stopListening();
//        }
//
//    };

    private void stopListening(){
        if(recorder != null) {
            recorder.stop();
            recorder.release();
            recorder = null;
        }
    }

//    private void stopListening(){
//        if(bluetoothRecorder != null) {
//            bluetoothRecorder.stop();
//            bluetoothRecorder.release();
//            bluetoothRecorder = null;
//        }
//    }

    @Override
    protected String getTitle() {
        return getString(R.string.fragment_action_send_audio);
    }

    @Override
    protected int getRawCode() {
        return R.raw.code_audio;
    }
}
