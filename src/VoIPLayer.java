import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * CMPC3M06 Coursework 1
 * <p>
 * VoIPLayer class is designed to process VOIP packets and apply different packet compensations
 * <p>
 * Author: Sibtain Syed
 */

public class VoIPLayer {
    private final AtomicInteger sequenceNumber = new AtomicInteger(0);

    private ConcurrentHashMap<Integer, byte[]> receivedPacketsMap = new ConcurrentHashMap<>();
    private int expectedSequence = 0;
    //Skips lost packets up to PACKET_LOSS_THRESHOLD
    private static final int MAX_MISSED_PACKETS = 100;
    private static final int MAX_PACKET_SIZE = 516;
    private final PriorityQueue<Integer> receivedPacketsBufferQueue = new PriorityQueue<>();
    private static final int INTERLEAVING_BLOCK_SIZE = 4;
    private static final int REPETITION_THRESHOLD = 3; // Repetition applied after 3 missing packets

    // Add sequence number to the block and return
    public byte[] processSendPacket(byte[] packet) {
        int nextSequenceNumber = sequenceNumber.getAndIncrement();
        //System.out.println("nextSequenceNumber " + nextSequenceNumber);

        // create ByteBuffer for packet (4 byte for sequence + encrypted data)
        ByteBuffer packetBuffer = ByteBuffer.allocate(packet.length + 4);
        packetBuffer.putInt(nextSequenceNumber);
        packetBuffer.put(packet);

        return packetBuffer.array();
    }

    // Remove the sequence number and return the audio block for playback
    public byte[] processReceivePacket(byte[] receivePacket) {

        ByteBuffer packetBuffer = ByteBuffer.wrap(receivePacket);

        // extract sequence number
        int receivedSequenceNumber = packetBuffer.getInt();
        //System.out.println("receivedSequenceNumber " + receivedSequenceNumber);

        // Extract and decrypt raw data
        byte[] receivedBlock = new byte[receivePacket.length - 4];
        packetBuffer.get(receivedBlock);

        return receivedBlock;
    }

    // Handles packets received from DatagramSocket2
    // Observed drop in packets, so using repetition technique to replay the previous block
    public byte[] processDS2ReceivePacket(byte[] receivePacket) {
        //we process valid packets
        if (receivePacket != null && (receivePacket.length == MAX_PACKET_SIZE)) {
            ByteBuffer packetBuffer = ByteBuffer.wrap(receivePacket);

            // extract sequence number
            int receivedSequenceNumber = packetBuffer.getInt();
            //System.out.println("DS2-receivedSequenceNumber " + receivedSequenceNumber);

            // Extract and decrypt raw data
            byte[] receivedBlock = new byte[receivePacket.length - 4];
            packetBuffer.get(receivedBlock);

            //add the sequence number and block to the map
            receivedPacketsMap.put(receivedSequenceNumber, receivedBlock);
            return handleReceivedPacketsLoss(receivedSequenceNumber);
        } else {
            System.out.println("VOIP - Invalid packet received");
        }
        return null;

    }

    //Check if block exist in the map, return the block
    //otherwise repeat the previous packet if exist to reduce the loss of packets
    private byte[] handleReceivedPacketsLoss(int sequenceNumber) {
        if (receivedPacketsMap.containsKey(sequenceNumber)) {
            return receivedPacketsMap.remove(sequenceNumber);
        } else {
            System.out.println("Observed Packet loss - Expected: " + sequenceNumber);
            byte[] repeatedPacket = repeatPreviousPacket(sequenceNumber);
            if (repeatedPacket != null) {
                System.out.println("Applying repetition for the lost packet: " + sequenceNumber);
                return repeatedPacket;
            }
        }
        return null;
    }

    // Applies repetition to reconstruct lost packets
    // get the previous packet from the map
    private byte[] repeatPreviousPacket(int sequenceNumber) {
        //get the previous packet from the map
        byte[] prevPacket = receivedPacketsMap.get(sequenceNumber - 1);
        if (prevPacket != null) {
            return Arrays.copyOf(prevPacket, prevPacket.length);
        }
        return null;
    }

    // Handles packets received from DatagramSocket3
    // Observed out of order, so fixing by re-ordering the packets
    public byte[] processDS3ReceivePacket(byte[] receivePacket) {
        //we process valid packets
        if (receivePacket != null && receivePacket.length == MAX_PACKET_SIZE) {
            ByteBuffer packetBuffer = ByteBuffer.wrap(receivePacket);

            // extract sequence number
            int receivedSequenceNumber = packetBuffer.getInt();
            //System.out.println("DS3-receivedSequenceNumber " + receivedSequenceNumber);

            // Extract and decrypt raw data
            byte[] receivedBlock = new byte[receivePacket.length - 4];
            packetBuffer.get(receivedBlock);

            //add the sequence number and block to the map
            receivedPacketsMap.put(receivedSequenceNumber, receivedBlock);

            //Add the sequence number into the priority queue
            receivedPacketsBufferQueue.add(receivedSequenceNumber);
            return handlePacketReordering();
        } else {
            System.out.println("VOIP - Invalid packet received");
        }
        return null;

    }

    // Handles out-of-order packets by reordering them before sending back
    // Uses PriorityQueue to maintain the order of packets
    private byte[] handlePacketReordering() {
        //if queue is not empty, and we have expected sequence in the queue
        while (!receivedPacketsBufferQueue.isEmpty()) {
            //get the next sequence and check we have in received map
            int nextExpected = receivedPacketsBufferQueue.poll();
            if (receivedPacketsMap.containsKey(nextExpected)) {
                //expectedSequence++;
                //removing from the map will return packet data of the sequence
                return receivedPacketsMap.remove(nextExpected);
            }
        }
        return null;
    }

    // Handles packets sent from DatagramSocket4
    // using sender based interleave compensation
    public byte[] processDS4SendPacket(byte[] packet) {
        int nextSequenceNumber = sequenceNumber.getAndIncrement();
        //System.out.println("nextSequenceNumber " + nextSequenceNumber);

        // create ByteBuffer for packet (4 byte for sequence + encrypted data)
        ByteBuffer packetBuffer = ByteBuffer.allocate(packet.length + 4);
        packetBuffer.putInt(nextSequenceNumber);

        //using d block interleaver to scramble data before transmission.
        packetBuffer.put(interleave(packet));
        return packetBuffer.array();
    }

    // Handles packets received from DatagramSocket4
    // Observed burst of packet loss, so using interleave and repetition techniques for compensation
    public byte[] processDS4ReceivePacket(byte[] receivePacket) {//we process valid packets
        if (receivePacket != null && receivePacket.length == MAX_PACKET_SIZE) {
            ByteBuffer packetBuffer = ByteBuffer.wrap(receivePacket);

            // extract sequence number
            int receivedSequenceNumber = packetBuffer.getInt();
            //System.out.println("receivedSequenceNumber " + receivedSequenceNumber);

            // Extract and decrypt raw data
            byte[] receivedBlock = new byte[receivePacket.length - 4];
            packetBuffer.get(receivedBlock);

            //add the sequence number and block to the map
            receivedPacketsMap.put(receivedSequenceNumber, receivedBlock);
            return handlePacketLoss(receivedSequenceNumber);
        } else {
            System.out.println("VOIP - Invalid packet received");
        }
        return null;

    }

    // Handles packet loss by using interleaving technique and repetition
    private byte[] handlePacketLoss(int sequenceNumber) {
        //if sequence sound in map de-interleave the packet
        if (receivedPacketsMap.containsKey(sequenceNumber)) {
            return deinterleave(receivedPacketsMap.remove(sequenceNumber));
        } else {
            System.out.println("Observed packet loss - Expected: " + sequenceNumber);
            byte[] repeatedPacket = applyRepetition(sequenceNumber);
            if (repeatedPacket != null) {
                System.out.println("Applying repetition to recover lost packet: " + sequenceNumber);
                return repeatedPacket;
            }
        }
        return null;
    }

    // Interleaves packet data to reduce burst losses
    private byte[] interleave(byte[] data) {
        byte[] interleaved = new byte[data.length];
        int blockSize = Math.min(INTERLEAVING_BLOCK_SIZE, data.length);
        for (int i = 0; i < blockSize; i++) {
            for (int j = i; j < data.length; j += blockSize) {
                interleaved[j] = data[i + (j / blockSize) * blockSize];
            }
        }
        return interleaved;
    }

    // Deinterleaves packet data to reconstruct the original order
    private byte[] deinterleave(byte[] data) {
        byte[] deinterleaved = new byte[data.length];
        int blockSize = Math.min(INTERLEAVING_BLOCK_SIZE, data.length);
        for (int i = 0; i < blockSize; i++) {
            for (int j = i; j < data.length; j += blockSize) {
                deinterleaved[i + (j / blockSize) * blockSize] = data[j];
            }
        }
        return deinterleaved;
    }

    // Applies repetition to reconstruct lost packets
    // Ensure that sequence number of packet does not exist a threshold
    private byte[] applyRepetition(int sequenceNumber) {
        byte[] lastReceivedPacket = receivedPacketsMap.get(sequenceNumber - 1);
        if (lastReceivedPacket != null && sequenceNumber - expectedSequence >= REPETITION_THRESHOLD) {
            System.out.println("Fixing repetition to recover lost packet: " + sequenceNumber);
            expectedSequence++;
            return Arrays.copyOf(lastReceivedPacket, lastReceivedPacket.length);
        }
        return null;
    }
}
