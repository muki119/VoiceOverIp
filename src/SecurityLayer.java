
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SecurityLayer {

    private byte[] xorEncryptionKey = new byte[8];



    SecurityLayer(){
        Random rand = new Random();
        rand.nextBytes(xorEncryptionKey);
    }

    private byte[] xorFunction(byte[] data){
        byte[] decryptedDataArray = new byte[data.length];

        int encryptionKeyLength = xorEncryptionKey.length;
        for (int x=0; x<data.length; x++){
            byte incomingData = data[x];
            byte key = xorEncryptionKey[x%encryptionKeyLength];
            byte decryptedData = (byte) (incomingData^key);
            decryptedDataArray[x] = decryptedData;
        }
        return decryptedDataArray;
    }
    public byte[] encrypt(byte[]decryptedData){
        return (xorFunction(decryptedData));
    }
    // the incoming data is an array containing the audio to be played.
    public byte[] decrypt(byte[]encryptedData){
        return (xorFunction(encryptedData));
    }
    public void authenticate(){

    }
    public void handshake(){
        // send to
    }
}

