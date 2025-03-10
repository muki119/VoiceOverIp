/*
 * TextSender.java
 */

/**
 * CMPC3M06 Coursework 1
 *
 *  This class is designed to capture audio message, encrypt the packet and send
 *
 * Author: Sibtain Syed
 */

import CMPC3M06.AudioRecorder;

import java.net.*;

public class AudioSenderThread implements Runnable {

    static DatagramSocket sendingSocket;

    public void start() {
        Thread thread = new Thread(this);
        thread.start();
    }

    public void run() {

        //***************************************************
        //Port to send to
        int PORT = 55555;
        //IP ADDRESS to send to
        InetAddress clientIP = null;
        try {
            clientIP = InetAddress.getByName("localhost");  //CHANGE localhost to IP or NAME of client machine
        } catch (UnknownHostException e) {
            System.out.println("ERROR: TextSender: Could not find client IP");
            e.printStackTrace();
            System.exit(0);
        }
        //***************************************************

        //***************************************************
        //Open a socket to send from
        //We dont need to know its port number as we never send anything to it.
        //We need the try and catch block to make sure no errors occur.

        //DatagramSocket sending_socket;
        try {
            sendingSocket = new DatagramSocket();
        } catch (SocketException e) {
            System.out.println("ERROR: TextSender: Could not open UDP socket to send from.");
            e.printStackTrace();
            System.exit(0);
        }

        try {
            boolean running = true;
            AudioRecorder recorder = new AudioRecorder();
            SecurityLayer securityLayer = new SecurityLayer();
            VoIPLayer voIPLayer = new VoIPLayer(securityLayer);
            System.out.println("Starting audio transmission...");

            while (running) {
                //Read in a string from the standard input
                byte[] voiceBlock = recorder.getBlock();

                byte[] voipPacket = voIPLayer.processVoipPacket(voiceBlock);

                //Make a DatagramPacket from it, with client address and port number
                DatagramPacket packet = new DatagramPacket(voipPacket, voipPacket.length, clientIP, PORT);

                //Send it
                sendingSocket.send(packet);
            }

        } catch (Exception e) {
            System.out.println("ERROR: AudioSender: Exception occured!");
            e.printStackTrace();
        }
        //Close the socket
        sendingSocket.close();
        //***************************************************
    }
} 
