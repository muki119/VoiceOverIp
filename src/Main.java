import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello world!");
        Connection testConnection = new Connection("localhost",6000,2556).listen();
//        SecurityLayer testLayer = new SecurityLayer();
//        byte[] testBytes = new byte[512];
//        Random rand = new Random();
//        rand.nextBytes(testBytes);
//        byte[] encryptedBytes = testLayer.encrypt(testBytes);
//        byte[] decryptedBytes = testLayer.decrypt(encryptedBytes);
//        boolean isWorking = Arrays.equals(decryptedBytes, testBytes);
//        System.out.println(isWorking);
        while(testConnection.isListening()){
            String text = new Scanner(System.in).nextLine()+'\n';
            testConnection.sendData(text.getBytes());
        }
    }
}