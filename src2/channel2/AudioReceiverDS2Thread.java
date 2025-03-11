package channel2;
/**
 * CMPC3M06 Coursework 1
 *
 *  This class is designed to receive audio message, decrypt message and playback
 *
 * @author Sibtain Syed
 */

import CMPC3M06.AudioPlayer;
import channel.SecurityLayer;
import channel.VoIPLayer;
import uk.ac.uea.cmp.voip.DatagramSocket2;

import java.net.DatagramPacket;
import java.net.SocketException;

public class AudioReceiverDS2Thread implements Runnable {

    static DatagramSocket2 receivingSocket;

    public void start() {
        Thread thread = new Thread(this);
        thread.start();
    }

    public void run() {

        //***************************************************
        //Port to open socket on
        int PORT = 55555;
        //***************************************************

        //***************************************************
        //Open a socket to receive from on port PORT

        //DatagramSocket receiving_socket;
        try {
            receivingSocket = new DatagramSocket2(PORT);
        } catch (SocketException e) {
            System.out.println("ERROR: TextReceiver: Could not open UDP socket to receive from.");
            e.printStackTrace();
            System.exit(0);
        }
        //***************************************************

        //***************************************************
        //channel.Main loop.
        try {
            boolean running = true;
            AudioPlayer player = new AudioPlayer();
            SecurityLayer securityLayer = new SecurityLayer();
            VoIPLayer voIPLayer = new VoIPLayer(securityLayer);
            System.out.println("Listening for audio...");

            while (running) {
                //Receive a Audio packet (32ms/512bytes each)
                byte[] voiceData = new byte[518];
                DatagramPacket packet = new DatagramPacket(voiceData, voiceData.length);
                receivingSocket.receive(packet);

//call the security layer to decrypt the packet
                voiceData = securityLayer.decryptReceivePacket(voiceData);

                //call the voip layer to process the decrypted packet
                voiceData = voIPLayer.processReceivePacket(voiceData);

                if (voiceData != null) {
                    player.playBlock(voiceData);
                }
            }
        } catch (Exception e) {
            System.out.println("ERROR: AudioReceiver: Exception occured!");
            e.printStackTrace();
        }
        //Close the socket
        receivingSocket.close();
        //***************************************************
    }
}
