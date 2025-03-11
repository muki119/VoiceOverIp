package channel;

import channel.SecurityLayer;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * CMPC3M06 Coursework 1
 * <p>
 * channel.VoIPLayer class is designed to process VOIP packets
 * <p>
 * Author: Sibtain Syed & Jay Groom
 */

public class VoIPLayer {
    private AtomicInteger sequenceNumber = new AtomicInteger(0);
    private SecurityLayer securityLayer;

    public VoIPLayer(SecurityLayer securityLayer) throws Exception {
        this.securityLayer = securityLayer;
    }

    // Encrypts and prepares the VoIP packet for transmission
    public byte[] processSendPacket(byte[] packet) {
        int nextSequenceNumber = sequenceNumber.getAndIncrement();
        //System.out.println("nextSequenceNumber " + nextSequenceNumber);
        // encrypt and authenticate
        byte[] encryptedBlock = securityLayer.encryptAndAddAuthenticationToken(packet);

        // create ByteBuffer for packet (4 byte for sequence + encrypted data)
        ByteBuffer packetBuffer = ByteBuffer.allocate(encryptedBlock.length + 4);
        packetBuffer.putInt(nextSequenceNumber);
        packetBuffer.put(encryptedBlock);

        return packetBuffer.array();
    }

    // receives and processes an incoming VoIP packet
    public byte[] processReceivePacket(byte[] receivePacket) throws Exception {
        ByteBuffer packetBuffer = ByteBuffer.wrap(receivePacket);

        // extract sequence number
        int receivedSequenceNumber = packetBuffer.getInt();
        System.out.println("receivedSequenceNumber " + receivedSequenceNumber);

        // Extract and decrypt raw data
        byte[] receivedBlock = new byte[receivePacket.length - 4];
        packetBuffer.get(receivedBlock);

        return receivedBlock;
    }

}
