import java.io.IOException;
import java.math.BigInteger;
import java.net.*;
import java.util.Arrays;
import java.util.concurrent.Callable;

public class Connection {
    private DatagramSocket socket;
    private InetAddress ip;
    private int port;
    private int portToBind;
    private boolean listening = false;
    private boolean acknowledged = false;
    private SecurityLayer securityLayer = new SecurityLayer();

    public Connection(String ip, int port) {
        try{
            this.ip = InetAddress.getByName(ip);
            this.port = port;
            this.portToBind = port;
            this.socket = new DatagramSocket();
            System.out.println("Sending to " + this.ip+" On port "+this.port);

        }catch (UnknownHostException e){
            System.out.println("Unknown Host \n Input a valid IP address");
            throw new RuntimeException(e);
        }catch (SocketException e){
            System.out.println("Socket Error");
            throw new RuntimeException(e);
        }
    }
    public Connection(String ip, int port, int portToBind) {
        this(ip, port);
        this.portToBind = portToBind;
    }
    public boolean isListening(){
        return listening;
    }

    public void sendData(byte[] data){
        // go through processes to send - full layers
        new Thread(()->{
            try{
                DatagramPacket dataPacket = new DatagramPacket(data, data.length, this.ip, this.port);
                socket.send(dataPacket);
            }catch ( IOException e){
                System.out.println("Error sending data");
                e.printStackTrace();
            }
        }).start();
    }
    public Connection listen(){
        // listen to incoming
        try{
            this.socket = new DatagramSocket(this.portToBind);
            Thread socketListenerThread = new Thread(()->{
                System.out.println("Listening on port "+this.portToBind);
                while (this.listening){
                    try{
                        DatagramPacket incomingPacket = new DatagramPacket(new byte[40], 40);
                        socket.receive(incomingPacket);
                        // put through security layer
                        System.out.println(new String(incomingPacket.getData()).trim());
                    }catch (IOException e){
                        System.out.println("Error receiving data");
                        e.printStackTrace();
                    }
                }
                
            });
            handshake();
            if (this.securityLayer.hasSharedSecretKey()){
                this.listening = true;
                socketListenerThread.start();
            }
            return this;
        }catch (SocketException e){
            System.out.println("Socket Error");
            System.out.println(e);
            return null;
        }

    }

    public void listenOnce(String event, Runnable onEvent){
        byte[] eventBytes = event.getBytes();
        byte[] buffer = new byte[eventBytes.length];
        while(true) {
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

    public void listenFor(String event, Runnable func){
        if(!this.socket.isBound()){
            System.out.println("Socket is not bound!!");
            return;
        }
        byte[] eventBytes = event.getBytes();
        byte[] buffer = new byte[eventBytes.length];
        new Thread(()-> {
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

    private void handshake(){
        new Thread(()->{
            while(!this.acknowledged){
                sendData("HELLO".getBytes());
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
        Thread handshakeThread  = new Thread(()->{
            while(!this.acknowledged){
                byte[] buffer = new byte[40];
                try{
                    DatagramPacket incomingPacket = new DatagramPacket(buffer, buffer.length);
                    socket.receive(incomingPacket);
                    String incomingEvent = new String(incomingPacket.getData()).trim();

                    if (incomingEvent.equals("HELLO")) { // if the other side says hello - you are now peer B
                        this.acknowledged = true;
                        sendData("ACK".getBytes());
                        System.out.println("Received HELLO , client is now peer B");
                    }else if(incomingEvent.equals("ACK")){ // if the other side acknowledges - you are now peer A
                        this.acknowledged = true;
                        System.out.println("Received ACK , client is now peer a ");
                        //listen for incoming other public key
                    }
                    if (this.acknowledged){
                        try{

                            securityLayer.generatePrivateKey();
                            byte[] clientPublicKey= securityLayer.createClientPublicKey().toByteArray();

                            new Thread(()->{
                                while(!securityLayer.hasSharedSecretKey()){ // while a shared secret key hasnt been created
                                    sendData(clientPublicKey);// sends client public key
                                    try {
                                        Thread.sleep(200);
                                    } catch (InterruptedException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                            }).start();

                            byte[] otherPublicKeyBuffer = new byte[400];
                            DatagramPacket otherPublicKeyPacket = new DatagramPacket(otherPublicKeyBuffer, otherPublicKeyBuffer.length); // next thing to be recieved is the other peers public key
                            socket.receive(otherPublicKeyPacket);// wait for key to be arrived
                            byte[] trimmedOtherPublicKeyBuffer = Arrays.copyOf(otherPublicKeyPacket.getData(), otherPublicKeyPacket.getLength());
                            securityLayer.createSharedSecret(new BigInteger(trimmedOtherPublicKeyBuffer)); // saves our shared secret.
                            if (securityLayer.hasSharedSecretKey()){System.out.println("Handshake complete");}

                        }catch (Exception e){
                            System.out.println("Error creating shared secret");
                            e.printStackTrace();
                        }
                    }
                }catch (IOException e){
                    System.out.println("Error receiving data");
                    e.printStackTrace();
                }


            }
        });
        handshakeThread.start();
        try {
            handshakeThread.join();
        }catch (InterruptedException e){
            System.out.println("Error waiting for handshake thread");
            e.printStackTrace();
        }






    }
}
