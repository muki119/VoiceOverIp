import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * CMPC3M06 Coursework 1
 *
 *  VoIPLayer class is designed to process VOIP packets
 *
 * Author: Sibtain Syed
 */

public class VoIPLayer {
    private AtomicInteger sequenceNumber = new AtomicInteger(0);
    private SecurityLayer securityLayer;

    public VoIPLayer(SecurityLayer securityLayer) throws Exception {
        this.securityLayer = securityLayer;
    }

    public byte[] processVoipPacket(byte[] packet) {
        int nextSequenceNumber = sequenceNumber.getAndIncrement();
        byte[] encryptedBlock = securityLayer.encryptAndAddAuthenticationToken(packet);
        ByteBuffer packetBuffer = ByteBuffer.allocate(encryptedBlock.length + 4);
        packetBuffer.putInt(nextSequenceNumber);
        packetBuffer.put(encryptedBlock);
        return packetBuffer.array();
    }

    public byte[] receivePacket(byte[] receivePacket) throws Exception {
        ByteBuffer packetBuffer = ByteBuffer.wrap(receivePacket);
        int sequenceNumber = packetBuffer.getInt();
        byte[] rawData = new byte[receivePacket.length - 4];
        packetBuffer.get(rawData);

        return rawData;

    }

}
