package com.innovavn.echo;

import android.app.Instrumentation;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import com.innovavn.echo.action.BaseListenerFragment;
import com.willblaschko.android.alexa.AlexaManager;
import com.willblaschko.android.alexa.audioplayer.AlexaAudioPlayer;
import com.willblaschko.android.alexa.callbacks.AsyncCallback;
import com.willblaschko.android.alexa.interfaces.AvsItem;
import com.willblaschko.android.alexa.interfaces.AvsResponse;

import com.innovavn.echo.global.Constants;
import com.willblaschko.android.alexa.interfaces.audioplayer.AvsPlayAudioItem;
import com.willblaschko.android.alexa.interfaces.audioplayer.AvsPlayContentItem;
import com.willblaschko.android.alexa.interfaces.audioplayer.AvsPlayRemoteItem;
import com.willblaschko.android.alexa.interfaces.errors.AvsResponseException;
import com.willblaschko.android.alexa.interfaces.playbackcontrol.AvsMediaNextCommandItem;
import com.willblaschko.android.alexa.interfaces.playbackcontrol.AvsMediaPauseCommandItem;
import com.willblaschko.android.alexa.interfaces.playbackcontrol.AvsMediaPlayCommandItem;
import com.willblaschko.android.alexa.interfaces.playbackcontrol.AvsReplaceAllItem;
import com.willblaschko.android.alexa.interfaces.playbackcontrol.AvsReplaceEnqueuedItem;
import com.willblaschko.android.alexa.interfaces.playbackcontrol.AvsStopItem;
import com.willblaschko.android.alexa.interfaces.speaker.AvsAdjustVolumeItem;
import com.willblaschko.android.alexa.interfaces.speaker.AvsSetMuteItem;
import com.willblaschko.android.alexa.interfaces.speaker.AvsSetVolumeItem;
import com.willblaschko.android.alexa.interfaces.speechrecognizer.AvsExpectSpeechItem;
import com.willblaschko.android.alexa.interfaces.speechsynthesizer.AvsSpeakItem;

import java.util.ArrayList;
import java.util.List;

import static com.innovavn.echo.global.Constants.PRODUCT_ID;

public abstract class BaseActivity extends AppCompatActivity implements BaseListenerFragment.AvsListenerInterface{
    private static final String TAG = "BaseActivity";

    public static final int STATE_LISTENING = 1;
    public static final int STATE_PROCESSING = 2;
    public static final int STATE_SPEAKING = 3;
    public static final int STATE_PROMPTING = 4;
    public static final int STATE_FINISHED = 0;

    private AlexaManager alexaManager;
    private AlexaAudioPlayer alexaAudioPlayer;
    private List<AvsItem> avsQueue = new ArrayList<>();

    private long startTimeMs = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initAlexaAndroid();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (alexaAudioPlayer != null) {
            alexaAudioPlayer.stop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (alexaAudioPlayer != null) {
            // remove callback to avoid memory leaks
            alexaAudioPlayer.removeCallback(alexaAudioPlayerCallback);
            alexaAudioPlayer.release();
        }
    }

    @Override
    public AsyncCallback<AvsResponse, Exception> getRequestCallback() {
        return requestCallback;
    }

    private void initAlexaAndroid() {
        // get our AlexaManager instance for convenience
        alexaManager = AlexaManager.getInstance(this, PRODUCT_ID);

        // instantiate our audio player
        alexaAudioPlayer = AlexaAudioPlayer.getInstance(this);

        // Remove current item and check for more items once we've finished playing
        alexaAudioPlayer.addCallback(alexaAudioPlayerCallback);

        //open our downchannel
        //alexaManager.sendOpenDownchannelDirective(requestCallback);


        //synchronize our device
        //alexaManager.sendSynchronizeStateEvent(requestCallback);
    }

    //Our callback that deals with removing played items in our media player and then checking to see if more items exist
    private AlexaAudioPlayer.Callback alexaAudioPlayerCallback = new AlexaAudioPlayer.Callback() {
        private boolean blIsDoneFired = false;
        private boolean blIsPlaybackStartedFired = false;

        @Override
        public void playerPrepared(AvsItem completedItem) {
            blIsDoneFired = false;
            blIsPlaybackStartedFired = false;
            avsQueue.remove(completedItem);
            checkQueue();
            if (completedItem instanceof AvsPlayContentItem || completedItem == null) {
                return;
            }
            if (BuildConfig.DEBUG) {
                Log.i(TAG, "Complete " + completedItem.getToken() + " fired");
            }
            sendPlaybackFinishedEvent(completedItem);
        }

        @Override
        public boolean playerError(AvsItem item, int what, int extra) {
            return false;
        }

        @Override
        public void dataError(AvsItem item, Exception e) {
            e.printStackTrace();
        }

        @Override
        public void itemComplete(AvsItem completedItem) {

        }

        @Override
        public void playerProgress(AvsItem currentItem, long offsetInMilliseconds, float percent) {
            if (BuildConfig.DEBUG) {
//                Log.i(TAG, "player percent: " + percent);
            }
            if (currentItem instanceof AvsPlayContentItem || currentItem == null) {
                return;
            }

            if (!blIsPlaybackStartedFired) {
                if (BuildConfig.DEBUG) {
                    Log.i(TAG, "playbackStarted " + currentItem.getToken() + " fired: " + percent);
                }
                blIsPlaybackStartedFired = true;
                sendPlaybackStartedEvent(currentItem, offsetInMilliseconds);
            }
            if (!blIsDoneFired && percent > .8f) {
                if (BuildConfig.DEBUG) {
                    Log.i(TAG, "Almost Done " + currentItem.getToken() + "fired: " + percent);
                }
                blIsDoneFired = true;
                if (currentItem instanceof AvsPlayAudioItem) {
                    sendPlaybackNearlyFinishedEvent((AvsPlayAudioItem) currentItem, offsetInMilliseconds);
                }
            }
        }
    };

    /**
     * Send an event back to Alexa that we're nearly done with our current playback event, this should supply us with the next item
     * https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/reference/audioplayer#PlaybackNearlyFinished Event
     */
    private void sendPlaybackNearlyFinishedEvent(AvsPlayAudioItem item, long offsetInMilliseconds){
        if (item != null) {
            alexaManager.sendPlaybackNearlyFinishedEvent(item, offsetInMilliseconds, requestCallback);
            Log.i(TAG, "Sending PlaybackNearlyFinishedEvent");
        }
    }

    /**
     * Send an event back to Alexa that we're starting a speech event
     * https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/reference/audioplayer#PlaybackNearlyFinished Event
     */
    private void sendPlaybackStartedEvent(AvsItem item, long milliseconds){
        alexaManager.sendPlaybackStartedEvent(item, milliseconds, null);
        Log.i(TAG, "Sending SpeechStartedEvent");
    }

    /**
     * Send an event back to Alexa that we're done with our current speech event, this should supply us with the next item
     * https://developer.amazon.com/public/solutions/alexa/alexa-voice-service/reference/audioplayer#PlaybackNearlyFinished Event
     */
    private void sendPlaybackFinishedEvent(AvsItem item){
        if (item != null) {
            alexaManager.sendPlaybackFinishedEvent(item, null);
            Log.i(TAG, "Sending PlaybackFinishedEvent");
        }
    }

    //async callback for commands sent to Alexa Voice
    private AsyncCallback<AvsResponse, Exception> requestCallback = new AsyncCallback<AvsResponse, Exception>() {
        @Override
        public void start() {
            startTimeMs = System.currentTimeMillis();
            Log.i(TAG, "Event start");
            setState(STATE_PROCESSING);
        }

        @Override
        public void success(AvsResponse result) {
            Log.i(TAG, "Event success");
            handleResponse(result);
        }

        @Override
        public void failure(Exception error) {
            error.printStackTrace();
            Log.i(TAG, "Event error");
            setState(STATE_FINISHED);
        }

        @Override
        public void complete() {
            Log.i(TAG, "Event complete");
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    long totalTimeMs = System.currentTimeMillis() - startTimeMs;
                    Toast.makeText(BaseActivity.this, "Total request time: " + totalTimeMs + " ms", Toast.LENGTH_LONG).show();
                }
            });
        }
    };

        /**
         * Handle the response sent back from Alexa's parsing of the Intent, these can be any of the AvsItem types (play, speak, stop, clear, listen)
         * @param response a List<AvsItem> returned from the mAlexaManager.sendTextRequest() call in sendVoiceToAlexa()
         */
        private void handleResponse(AvsResponse response) {
            boolean blIsCheckAfter = (avsQueue.size() == 0);
            if (response != null) {
                //if we have a clear queue item in the list, we need to clear the current queue before proceeding
                //iterate backwards to avoid changing our array positions and getting all the nasty errors that come
                //from doing that
                for (int i = response.size() - 1; i >= 0; i--) {
                    if (response.get(i) instanceof AvsReplaceAllItem || response.get(i) instanceof AvsReplaceEnqueuedItem) {
                        // Clear our queue
                        avsQueue.clear();
                        // Remove Item
                        response.remove(i);
                    }
                }

                Log.i(TAG, "Adding " + response.size() + " items to our queue");
                if (BuildConfig.DEBUG) {
                    for (int i = 0; i < response.size(); i++) {
                        Log.i(TAG, "\tAdding: " + response.get(i).getToken());
                    }
                }

                avsQueue.addAll(response);
            }
            if (blIsCheckAfter) {
                checkQueue();
            }
        }

        /**
         * Check our current queue of items, and if we have more to parse (once we've reached a play or listen callback) then proceed to the
         * next item in our list.
         *
         * We're handling the AvsReplaceAllItem in handleResponse() because it needs to clear everything currently in the queue, before
         * the new items are added to the list, it should have no function here.
         */
        private void checkQueue() {
            // If we're out of things, hang up the phone and move on
            if (avsQueue.size() == 0) {
                setState(STATE_FINISHED);
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        long totalTimeMs = System.currentTimeMillis() - startTimeMs;
                        Toast.makeText(BaseActivity.this, "Total interaction time: " + totalTimeMs + " ms", Toast.LENGTH_LONG).show();
                        Log.i(TAG, "Total interaction time: " + totalTimeMs + " ms");
                    }
                });
                return;
            }

            final AvsItem current = avsQueue.get(0);

            Log.i(TAG, "Item type " + current.getClass().getName());

            if (current instanceof AvsPlayRemoteItem) {
                // play an URL
                if (!alexaAudioPlayer.isPlaying()) {
                    alexaAudioPlayer.playItem((AvsPlayRemoteItem) current);
                }
            } else if (current instanceof AvsPlayContentItem) {
                // play an URL
                if (!alexaAudioPlayer.isPlaying()) {
                    alexaAudioPlayer.playItem((AvsPlayContentItem) current);
                }
            } else if (current instanceof AvsSpeakItem) {
                // play a sound file
                if (!alexaAudioPlayer.isPlaying()) {
                    alexaAudioPlayer.playItem((AvsSpeakItem) current);
                }
                setState(STATE_SPEAKING);
            } else if (current instanceof AvsStopItem) {
                // Stop our play
                alexaAudioPlayer.stop();
                avsQueue.remove(current);
            } else if (current instanceof AvsReplaceAllItem) {
                // Clear all items
                alexaAudioPlayer.stop();
                avsQueue.remove(current);
            } else if (current instanceof AvsReplaceEnqueuedItem) {
                //clear all items
                //mAvsItemQueue.clear();
                avsQueue.remove(current);
            } else if (current instanceof AvsExpectSpeechItem) {

                //listen for user input
                alexaAudioPlayer.stop();
                avsQueue.clear();
                startListening();
            } else if (current instanceof AvsSetVolumeItem) {
                //set our volume
                setVolume(((AvsSetVolumeItem) current).getVolume());
                avsQueue.remove(current);
            } else if(current instanceof AvsAdjustVolumeItem){
                //adjust the volume
                adjustVolume(((AvsAdjustVolumeItem) current).getAdjustment());
                avsQueue.remove(current);
            } else if(current instanceof AvsSetMuteItem){
                //mute/unmute the device
                setMute(((AvsSetMuteItem) current).isMute());
                avsQueue.remove(current);
            } else if(current instanceof AvsMediaPlayCommandItem){
                //fake a hardware "play" press
                sendMediaButton(this, KeyEvent.KEYCODE_MEDIA_PLAY);
                Log.i(TAG, "Media play command issued");
                avsQueue.remove(current);
            } else if(current instanceof AvsMediaPauseCommandItem){
                //fake a hardware "pause" press
                sendMediaButton(this, KeyEvent.KEYCODE_MEDIA_PAUSE);
                Log.i(TAG, "Media pause command issued");
                avsQueue.remove(current);
            }else if(current instanceof AvsMediaNextCommandItem){
                //fake a hardware "next" press
                sendMediaButton(this, KeyEvent.KEYCODE_MEDIA_NEXT);
                Log.i(TAG, "Media next command issued");
                avsQueue.remove(current);
            } else if(current instanceof AvsMediaPauseCommandItem){
                //fake a hardware "previous" press
                sendMediaButton(this, KeyEvent.KEYCODE_MEDIA_PREVIOUS);
                Log.i(TAG, "Media previous command issued");
                avsQueue.remove(current);
            } else if(current instanceof AvsResponseException){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        new AlertDialog.Builder(BaseActivity.this)
                                .setTitle("Error")
                                .setMessage(((AvsResponseException) current).getDirective().getPayload().getDescription())
                                .setPositiveButton(R.string.ok, null)
                                .show();
                    }
                });

                avsQueue.remove(current);
                checkQueue();
            }
        }

        protected abstract void startListening();

        private void adjustVolume(long adjust){
            setVolume(adjust, true);
        }
        private void setVolume(long volume){
            setVolume(volume, false);
        }
        private void setVolume(final long volume, final boolean adjust){
            AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
            final int max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            long vol= am.getStreamVolume(AudioManager.STREAM_MUSIC);
            if(adjust){
                vol += volume * max / 100;
            }else{
                vol = volume * max / 100;
            }
            am.setStreamVolume(AudioManager.STREAM_MUSIC, (int) vol, AudioManager.FLAG_VIBRATE);

            alexaManager.sendVolumeChangedEvent(volume, vol == 0, requestCallback);

            Log.i(TAG, "Volume set to : " + vol +"/"+max+" ("+volume+")");

            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    if(adjust) {
                        Toast.makeText(BaseActivity.this, "Volume adjusted.", Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(BaseActivity.this, "Volume set to: " + (volume / 10), Toast.LENGTH_SHORT).show();
                    }
                }
            });

        }
        private void setMute(final boolean isMute){
            AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
            am.setStreamMute(AudioManager.STREAM_MUSIC, isMute);

            alexaManager.sendMutedEvent(isMute, requestCallback);

            Log.i(TAG, "Mute set to : "+isMute);

            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(BaseActivity.this, "Volume " + (isMute ? "muted" : "unmuted"), Toast.LENGTH_SHORT).show();
                }
            });
        }

        /**
         * Force the device to think that a hardware button has been pressed, this is used for Play/Pause/Previous/Next Media commands
         * @param context
         * @param keyCode keycode for the hardware button we're emulating
         */
        private static void sendMediaButton(Context context, int keyCode) {
            Instrumentation inst = new Instrumentation();
            inst.sendKeyDownUpSync(keyCode);
        }

        private void setState(final int state) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    switch (state) {
                        case STATE_LISTENING:
                            stateListening();
                            break;
                        case STATE_PROCESSING:
                            stateProcessing();
                            break;
                        case STATE_SPEAKING:
                            stateSpeaking();
                            break;
                        case STATE_FINISHED:
                            stateFinished();
                            break;
                        case(STATE_PROMPTING):
                            statePrompting();
                            break;
                            default:
                                stateNone();
                                break;
                    }
                }
            });
        }

    protected abstract void stateListening();
    protected abstract void stateProcessing();
    protected abstract void stateSpeaking();
    protected abstract void stateFinished();
    protected abstract void statePrompting();
    protected abstract void stateNone();
}
