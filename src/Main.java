import CMPC3M06.AudioPlayer;
import CMPC3M06.AudioRecorder;

import javax.sound.sampled.LineUnavailableException;
import java.io.IOException;


public class Main {
    public static void main(String[] args) throws LineUnavailableException {
        System.out.println("Hello world!");
        AudioRecorder audioRecorder = new AudioRecorder();
        AudioPlayer audioPlayer = new AudioPlayer();
        Connection testConnection = new Connection("localhost",2000,1000);
        //instantiate voip layer
        testConnection.listen((plainTextData)->{
            try{
                audioPlayer.playBlock(plainTextData);
            } catch (IOException e) {
                System.out.println(e);
            }
            // when the
            //everytime audio comes in - use instance of voip layer to add data to audio buffer and play it.
        });
        while(testConnection.isListening()){
            //record audio block and send it on sendEncrypted(bytes)
//            String text = new Scanner(System.in).nextLine()+'\n';
            try {
                testConnection.sendEncrypted(audioRecorder.getBlock());
            } catch (IOException e) {
                System.out.println(e);
            }
//            System.out.println(Arrays.toString(bs));
//            System.out.println(Arrays.toString(text.getBytes()));
//            System.out.println(Arrays.toString(testConnection.testDecrypt(bs)));
        }
    }
}
