import uk.ac.uea.cmp.voip.DatagramSocket3;

import java.io.IOException;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.function.Consumer;

/**
 * CMPC3M06 Coursework 1
 * <p>
 * ConnectionDS3 class that will use DatagramSocket3 for UDP packet transmission
 * It uses static key for encryption
 * <p>
 * Author: Sibtain Syed
 */

public class ConnectionDS3 {
    private DatagramSocket3 socket;
    private InetAddress ip;
    private int port; // the port to send to
    private int portToBind; // the port to listen to
    private boolean listening = false;
    private boolean acknowledged = false;
    private SecurityLayer securityLayer = new SecurityLayer();

    public ConnectionDS3(String ip, int port) {
        try {
            this.ip = InetAddress.getByName(ip);
            this.port = port;
            this.portToBind = port;
            this.socket = new DatagramSocket3();
            System.out.println("Sending to " + this.ip + " On port " + this.port);

        } catch (UnknownHostException e) {
            System.out.println("Unknown Host \n Input a valid IP address");
            throw new RuntimeException(e);
        } catch (SocketException e) {
            System.out.println("Socket Error");
            throw new RuntimeException(e);
        }
    }

    public ConnectionDS3(String ip, int port, int portToBind) {
        this(ip, port);
        this.portToBind = portToBind;
    }

    public boolean isListening() {
        return listening;
    }

    public void sendEncrypted(byte[] data) {
        if (!securityLayer.hasSharedSecretKey()) {
            throw new RuntimeException("Connection Not Established");
        }
        byte[] encryptedData = securityLayer.encrypt(data);
        this.sendData(encryptedData);
    }

    public void sendData(byte[] data) {
        // go through processes to send - full layers
        try {
            byte[] authenticatedPacket = securityLayer.createAuthenticatedPacket(data);
            DatagramPacket dataPacket = new DatagramPacket(authenticatedPacket, authenticatedPacket.length, this.ip, this.port);
            socket.send(dataPacket);
            //System.out.println("Sending data: "+Arrays.toString(authenticatedPacket));
        } catch (IOException e) {
            System.out.println("Error sending data");
            e.printStackTrace();
        }
    }

    public ConnectionDS3 listen(Consumer<byte[]> callback) { // for listening to incoming audio
        try {
            this.socket = new DatagramSocket3(this.portToBind);
            Thread socketListenerThread = new Thread(() -> {
                System.out.println("Listening on port " + this.portToBind);
                while (this.listening) {
                    try {
                        byte[] buffer = new byte[1024];
                        DatagramPacket incomingPacket = new DatagramPacket(buffer, buffer.length);
                        socket.receive(incomingPacket);

                        byte[] trimmedData = Arrays.copyOfRange(incomingPacket.getData(), 0, incomingPacket.getLength());
                        byte[] authenticatedData = securityLayer.authenticate(trimmedData);
                        //System.out.println("trimmedData " + trimmedData.length);

                        if (authenticatedData == null) {
                            System.out.println("Connection DS3 - Invalid data received");
                            continue;
                        }

                        //System.out.println("authenticatedData " + authenticatedData.length);
                        byte[] plainTextData = securityLayer.decrypt(authenticatedData);// put through security layer
                        callback.accept(plainTextData); // pass to callback


                    } catch (IOException e) {
                        System.out.println("Error receiving data");
                        e.printStackTrace();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }

            });
            handshake();
            if (this.securityLayer.hasSharedSecretKey()) { // if security Layer can perform encryption/ decryption then allow incoming audio traffic
                this.listening = true;
                socketListenerThread.start();
            }
            return this;
        } catch (SocketException e) {
            System.out.println("Socket Error");
            throw new RuntimeException(e);
        }

    }

    public void listenOnce(String event, Runnable onEvent) {
        byte[] eventBytes = event.getBytes();
        byte[] buffer = new byte[eventBytes.length];
        while (true) {
            try {
                DatagramPacket incomingPacket = new DatagramPacket(buffer, buffer.length);
                socket.receive(incomingPacket);
                String incomingEvent = new String(incomingPacket.getData());
                if (incomingEvent.equals(event)) {
                    onEvent.run();
                    return;
                }

            } catch (IOException e) {
                System.out.println("Error receiving data");
                e.printStackTrace();
            }
        }

    }

    public void listenFor(String event, Runnable func) {
        if (!this.socket.isBound()) {
            System.out.println("Socket is not bound!!");
            return;
        }
        byte[] eventBytes = event.getBytes();
        byte[] buffer = new byte[eventBytes.length];
        new Thread(() -> {
            while (true) {
                try {
                    DatagramPacket incomingPacket = new DatagramPacket(buffer, buffer.length);
                    socket.receive(incomingPacket);
                    String incomingEvent = new String(incomingPacket.getData());
                    if (incomingEvent.equals(event)) {
                        func.run();
                    }

                } catch (IOException e) {
                    System.out.println("Error receiving data");
                    e.printStackTrace();
                }
            }
        }).start();

    }

    private void handshake() {
        new Thread(() -> {
            while (!this.acknowledged) {
                sendData("HELLO".getBytes());
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
        Thread handshakeThread = new Thread(() -> {
            while (!this.acknowledged) {
                byte[] buffer = new byte[40];
                try {
                    DatagramPacket incomingPacket = new DatagramPacket(buffer, buffer.length);
                    socket.receive(incomingPacket);

                    byte[] trimmedBuffer = Arrays.copyOf(incomingPacket.getData(), incomingPacket.getLength());
                    byte[] authenticatedData = securityLayer.authenticate(trimmedBuffer);
                    if (authenticatedData == null) {
                        continue;
                    }
                    String incomingEvent = new String(authenticatedData);
                    if (incomingEvent.equals("HELLO")) { // if the other side says hello - you are now peer B
                        this.acknowledged = true;
                        sendData("ACK".getBytes());
                        System.out.println("Received HELLO , client is now peer B");
                    } else if (incomingEvent.equals("ACK")) { // if the other side acknowledges - you are now peer A
                        this.acknowledged = true;
                        System.out.println("Received ACK , client is now peer a ");
                        //listen for incoming other public key
                    }
                    if (this.acknowledged) {
                        try {
                            //using static key because of loss packets
                            securityLayer.generateStaticPrivateKey();
                            byte[] clientPublicKey = securityLayer.createClientPublicKey().toByteArray();
                            securityLayer.createSharedSecret(new BigInteger(clientPublicKey)); // saves our shared secret.
                            if (securityLayer.hasSharedSecretKey()) {
                                System.out.println("Handshake complete");
                            } else {
                                System.out.println("Handshake failed");

                            }

                        } catch (Exception e) {
                            System.out.println("Error creating shared secret");
                            e.printStackTrace();
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Error receiving data");
                    e.printStackTrace();
                }


            }
        });
        handshakeThread.start();
        try {
            handshakeThread.join();
        } catch (InterruptedException e) {
            System.out.println("Error waiting for handshake thread");
            e.printStackTrace();
        }


    }
}