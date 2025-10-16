package chat.audio;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class AudioPlayback {
    private static final int SAMPLE_RATE = 44100;
    private static final int SAMPLE_SIZE_BITS = 16;
    private static final int CHANNELS = 1;
    private static final boolean SIGNED = true;
    private static final boolean BIG_ENDIAN = true;
    
    private SourceDataLine speaker;
    private AudioFormat format;
    private boolean isPlaying = false;
    
    public AudioPlayback() {
        this.format = new AudioFormat(SAMPLE_RATE, SAMPLE_SIZE_BITS, CHANNELS, SIGNED, BIG_ENDIAN);
    }
    
    public void startPlayback() throws LineUnavailableException {
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        
        if (!AudioSystem.isLineSupported(info)) {
            throw new LineUnavailableException("Linea de audio no soportada");
        }
        
        speaker = (SourceDataLine) AudioSystem.getLine(info);
        speaker.open(format);
        speaker.start();
        isPlaying = true;
        
        System.out.println("Reproduccion de audio iniciada");
    }
    
    public void playChunk(byte[] audioData) {
        if (!isPlaying || speaker == null || audioData == null || audioData.length == 0) {
            return;
        }
        
        speaker.write(audioData, 0, audioData.length);
    }
    
    public void playAudio(byte[] audioData) throws LineUnavailableException {
        if (audioData == null || audioData.length == 0) {
            return;
        }
        
        startPlayback();
        
        int offset = 0;
        int chunkSize = 1024;
        
        while (offset < audioData.length) {
            int length = Math.min(chunkSize, audioData.length - offset);
            speaker.write(audioData, offset, length);
            offset += length;
        }
        
        stopPlayback();
    }
    
    public void stopPlayback() {
        isPlaying = false;
        if (speaker != null) {
            speaker.drain();
            speaker.stop();
            speaker.close();
            System.out.println("Reproduccion de audio detenida");
        }
    }
    
    public boolean isPlaying() {
        return isPlaying;
    }
    
    public AudioFormat getFormat() {
        return format;
    }
}
