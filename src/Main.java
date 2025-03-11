import java.lang.reflect.Array;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello world!");
        Connection testConnection = new Connection("localhost",2556,6000);
        //instantiate voip layer
        testConnection.listen((plainTextData)->{ // when the
            //everytime audio comes in - use instance of voip layer to add data to audio buffer and play it.
        });
        while(testConnection.isListening()){
            //record audio block and send it on sendEncrypted(bytes)
            String text = new Scanner(System.in).nextLine()+'\n';
            testConnection.sendEncrypted(text.getBytes());
        }
    }
}
