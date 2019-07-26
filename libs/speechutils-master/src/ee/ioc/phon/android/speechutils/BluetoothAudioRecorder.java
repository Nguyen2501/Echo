package ee.ioc.phon.android.speechutils;

import android.media.MediaRecorder;

public class BluetoothAudioRecorder extends AbstractAudioRecorder {
    private static final int BLUETOOTH_HANDSFREE_AUDIO_SOURCE = MediaRecorder.AudioSource.VOICE_CALL;
    /**
     * <p>Instantiates a new recorder and sets the state to INITIALIZING.
     * In case of errors, no exception is thrown, but the state is set to ERROR.</p>
     * <p/>
     * <p>Android docs say: 44100Hz is currently the only rate that is guaranteed to work on all devices,
     * but other rates such as 22050, 16000, and 11025 may work on some devices.</p>
     *
     * @param audioSource Identifier of the audio source (e.g. microphone)
     * @param sampleRate  Sample rate (e.g. 16000)
     */
    public BluetoothAudioRecorder(int audioSource, int sampleRate) {
        super(audioSource, sampleRate);
        try {
            int bufferSize = getBufferSize();
            int framePeriod = bufferSize / (2 * RESOLUTION_IN_BYTES * CHANNELS);
            createRecorder(audioSource, sampleRate, bufferSize);
            createBuffer(framePeriod);
            setState(State.READY);
        } catch (Exception e) {
            if (e.getMessage() == null) {
                handleError("Unknown error occurred while initializing recorder");
            } else {
                handleError(e.getMessage());
            }
        }
    }

    public BluetoothAudioRecorder(int sampleRate) {
        this(BLUETOOTH_HANDSFREE_AUDIO_SOURCE, sampleRate);
    }

    public BluetoothAudioRecorder() {
        this(BLUETOOTH_HANDSFREE_AUDIO_SOURCE, DEFAULT_SAMPLE_RATE);
    }

    public String getWsArgs() {
        return "?content-type=audio/x-raw,+layout=(string)interleaved,+rate=(int)" + getSampleRate() + ",+format=(string)S16LE,+channels=(int)1";
    }
}
