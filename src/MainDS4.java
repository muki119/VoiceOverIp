import CMPC3M06.AudioPlayer;
import CMPC3M06.AudioRecorder;

import javax.sound.sampled.LineUnavailableException;
import java.io.IOException;

/**
 * CMPC3M06 Coursework 1
 * <p>
 * MainDS4 class that will use ConnectionDS4 to test DatagramSocket4 channel
 * <p>
 * Author: Sibtain Syed
 */

public class MainDS4 {
    public static void main(String[] args) throws LineUnavailableException, IOException {
        System.out.println("VOIP System Initializing - DS3");
        ConnectionDS4 testConnection = new ConnectionDS4("localhost", 2556, 6000);
        AudioPlayer player = new AudioPlayer();
        AudioRecorder recorder = new AudioRecorder();
        VoIPLayer voIPLayer = new VoIPLayer();

        //instantiate voip layer
        testConnection.listen((voiceData) -> { // when the
            //everytime audio comes in - use instance of voip layer to add data to audio buffer and play it.
            try {
                //call the voip layer method that handles DatagramSocket4 issue
                byte[] voipPacket = voIPLayer.processDS4ReceivePacket(voiceData);
                if (voipPacket != null) {
                    player.playBlock(voipPacket);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        System.out.println("testConnection.isListening() " + testConnection.isListening());
        while (testConnection.isListening()) {
            //record audio block and send it on sendEncrypted(bytes)
            byte[] block = recorder.getBlock(); // Capture audio block
            byte[] voipPacket = voIPLayer.processDS4SendPacket(block);
            testConnection.sendEncrypted(voipPacket);
        }
    }
}