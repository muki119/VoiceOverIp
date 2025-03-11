
import javax.swing.*;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SecurityLayer {
    //shared static secret key for authentication
    public static final String SHARED_SECRET = "SecureVoIPKey";

    private byte[] xorEncryptionKey = new byte[8];
    private final String hexPrime = "FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD1" +
            "29024E088A67CC74020BBEA63B139B22514A08798E3404DD" +
            "EF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245" +
            "E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7ED" +
            "EE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3D" +
            "C2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F" +
            "83655D23DCA3AD961C62F356208552BB9ED529077096966D" +
            "670C354E4ABC9804F1746C08CA18217C32905E462E36CE3B" +
            "E39E772C180E86039B2783A2EC07A28FB5C55DF06F4C52C9" +
            "DE2BCBF6955817183995497CEA956AE515D2261898FA0510" +
            "15728E5A8AAAC42DAD33170D04507A33A85521ABDF1CBA64" +
            "ECFB850458DBEF0A8AEA71575D060C7DB3970F85A6E1E4C7" +
            "ABF5AE8CDB0933D71E8C94E04A25619DCEE3D2261AD2EE6B" +
            "F12FFA06D98A0864D87602733EC86A64521F2B18177B200C" +
            "BBE117577A615D6C770988C0BAD946E208E24FA074E5AB31" +
            "43DB5BFCE0FD108E4B82D120A93AD2CAFFFFFFFFFFFFFFFF";

    private final BigInteger primeNumber = new BigInteger(hexPrime,16); // prime number
    private final short generator = 2; // primitive root
    private BigInteger clientPrivateKey; // your private key youre going to create your public key with
    private BigInteger sharedSecretKey; // the final key that will be used for encryption



    SecurityLayer(){
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

    public void generatePrivateKey(){
        Random rand = new Random();
        this.clientPrivateKey = new BigInteger(256,rand); // generate a 256bit integer.
    }
    public BigInteger createClientPublicKey(){ // Creates public key to be shared to other peer
        return BigInteger.valueOf(generator).modPow(this.clientPrivateKey,primeNumber);
    }

    public void createSharedSecret(BigInteger otherPublicKey){
        this.sharedSecretKey = otherPublicKey.modPow(this.clientPrivateKey,primeNumber);
    }
    public boolean hasSharedSecretKey(){
        return this.sharedSecretKey != null;
    }
    public boolean authenticate(String receivedKey){
        return SHARED_SECRET.equals(receivedKey);
    }
    
}

