import CMPC3M06.AudioPlayer;
import CMPC3M06.AudioRecorder;

import javax.sound.sampled.LineUnavailableException;
import java.io.IOException;

/**
 * CMPC3M06 Coursework 1
 * <p>
 * MainDS3 class that will use ConnectionDS3 to test DatagramSocket3 channel
 * <p>
 * Author: Sibtain Syed
 */

public class MainDS3 {
    public static void main(String[] args) throws LineUnavailableException, IOException {
        System.out.println("VOIP System Initializing - DS3");
        ConnectionDS3 testConnection = new ConnectionDS3("localhost",2556,6000);
        AudioPlayer player = new AudioPlayer();
        AudioRecorder recorder = new AudioRecorder();
        VoIPLayer voIPLayer = new VoIPLayer();

        //instantiate voip layer
        testConnection.listen((voiceData)->{ // when the
            //everytime audio comes in - use instance of voip layer to add data to audio buffer and play it.
            try {
                //call the voip layer method that handles DatagramSocket3 issue
                byte[] voipPacket = voIPLayer.processDS3ReceivePacket(voiceData);
                if (voipPacket != null) {
                    player.playBlock(voipPacket);
                }
            } catch (IOException  e) {
                throw new RuntimeException(e);
            }
        });
        System.out.println("testConnection.isListening() " + testConnection.isListening());
        while(testConnection.isListening()){
            //record audio block and send it on sendEncrypted(bytes)
            byte[] block = recorder.getBlock(); // Capture audio block
            byte[] voipPacket = voIPLayer.processSendPacket(block);
            testConnection.sendEncrypted(voipPacket);
        }
    }
}