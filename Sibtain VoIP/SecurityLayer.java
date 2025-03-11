import java.nio.ByteBuffer;

/**
 * CMPC3M06 Coursework 1
 *
 *  SecurityLayer class implement the security layer functionality
 *
 * Author: Sibtain Syed
 */
public class SecurityLayer {

    public static final int KEY = 1357;
    public static final short AUTHENTICATION_KEY = 10;
    private VoIPLayer voIPLayer;

    public SecurityLayer() throws Exception{
        this.voIPLayer = new VoIPLayer(this);
    }

    public byte[] encryptAndAddAuthenticationToken(byte[] block){
        byte[] encryptedBlock = encrypt(block);
        return setAuthenticationKey(encryptedBlock);
    }

    public byte[] decryptReceivePacket(byte[] receivePacket) throws Exception{
        ByteBuffer packetBuffer = ByteBuffer.wrap(receivePacket);
        int sequenceNumber = packetBuffer.getInt();
        //remove the sequence number to get authentication data
        byte[] dataWithAuthentication = new byte[receivePacket.length - 4];
        packetBuffer.get(dataWithAuthentication);

        byte[] encryptedBlock = authenticateAndExtractEncryptedPacket(dataWithAuthentication);
        if (encryptedBlock != null){
            byte[] decryptedBlock = decrypt(encryptedBlock);
            ByteBuffer packet = ByteBuffer.allocate(decryptedBlock.length + 4);
            packet.putInt(sequenceNumber);
            packet.put(decryptedBlock);
            return voIPLayer.receivePacket(packet.array());
        }

        return null;
    }

    // XOR Encryption Method
    private byte[] encrypt(byte[] block) {
        ByteBuffer plainText = ByteBuffer.wrap(block);
        ByteBuffer unwrapEncrypt = ByteBuffer.allocate(block.length);
        for (int j = 0; j < block.length / 4; j++) {
            int fourByte = plainText.getInt();
            fourByte = fourByte ^ KEY; // XOR operation with key
            unwrapEncrypt.putInt(fourByte);
        }
        return unwrapEncrypt.array();
    }

    // XOR decrypt Method
    private byte[] decrypt(byte[] encryptedBlock) {
        ByteBuffer cipherText = ByteBuffer.wrap(encryptedBlock);
        ByteBuffer unwrapDecrypt = ByteBuffer.allocate(encryptedBlock.length);
        for (int j = 0; j < encryptedBlock.length / 4; j++) {
            int fourByte = cipherText.getInt();
            fourByte = fourByte ^ KEY; // XOR decrypt (same key)
            unwrapDecrypt.putInt(fourByte);
        }
        return unwrapDecrypt.array();
    }

    public static byte[] setAuthenticationKey(byte[] block) {
        ByteBuffer buffer = ByteBuffer.allocate(block.length + 2);
        buffer.putShort(AUTHENTICATION_KEY);
        buffer.put(block);
        return buffer.array();
    }

    private byte[] authenticateAndExtractEncryptedPacket(byte[] block) {
        ByteBuffer buffer = ByteBuffer.wrap(block);
        short authKey = buffer.getShort();
        if (authKey != AUTHENTICATION_KEY) {
            System.out.println("ERROR: Discarding - Invalid Packet Received");
            return null;
        }

        //remove the auth key bytes
        byte[] encryptedBlock = new byte[block.length - 2];
        buffer.get(encryptedBlock);
        return encryptedBlock;
    }


}
