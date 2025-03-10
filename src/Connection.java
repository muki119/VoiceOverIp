import java.io.IOException;
import java.math.BigInteger;
import java.net.*;
import java.util.Arrays;
import java.util.concurrent.Callable;

public class Connection {
    private DatagramSocket socket;
    private InetAddress ip;
    private int port;
    private boolean listening = false;
    private boolean acknowledged = false;
    private SecurityLayer securityLayer = new SecurityLayer();

    public Connection(String ip, int port) {
        try{
            this.ip = InetAddress.getByName(ip);
            this.port = port;
            this.socket = new DatagramSocket();
            System.out.println("Sending to " + this.ip+" On port "+this.port);

        }catch (UnknownHostException e){
            System.out.println("Unknown Host \n Input a valid IP address");
        }catch (SocketException e){
            System.out.println("Socket Error");
            System.out.println(e);
        }
    }
    public boolean isListening(){
        return listening;
    }

    public void sendData(byte[] data){
        // go through processes to send - full layers
        new Thread(()->{
            try{
                //transform data through layers.
//                System.out.println("Sending to " + this.ip+" On port "+this.port+" "+new String(data));
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
            this.socket = new DatagramSocket(port);
            Thread socketListenerThread = new Thread(()->{
                System.out.println("Listening on port "+port);
                while (this.listening){
                    try{
                        DatagramPacket incomingPacket = new DatagramPacket(new byte[40], 40);
                        socket.receive(incomingPacket);
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
                        System.out.println("Recived HELLO , client is now peer B");

                    }else if(incomingEvent.equals("ACK")){ // if the other side acknowledges - you are now peer A
                        this.acknowledged = true;
                        System.out.println("Recieved ACK , client is now peer a ");
                        //listen for incoming otherpublic key
                    }
                    if (this.acknowledged){
                        try{
                            securityLayer.generatePrivateKey();
                            byte[] clientPublicKey= securityLayer.createClientPublicKey().toByteArray();
                            new Thread(()->{
                                while(!securityLayer.hasSharedSecretKey()){ // while a shared secret key hasnt been created
                                    sendData(clientPublicKey);// sends client public key
                                }
                            }).start();
                            byte[] otherPublicKeyBuffer = new byte[400];
                            DatagramPacket otherPublicKeyPacket = new DatagramPacket(otherPublicKeyBuffer, otherPublicKeyBuffer.length); // next thing to be recieved is the other peers public key
                            socket.receive(otherPublicKeyPacket);
                            securityLayer.createSharedSecret(new BigInteger(otherPublicKeyBuffer)); // finds and saves our shared secret.
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
