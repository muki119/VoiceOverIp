import java.io.IOException;
import java.net.*;
import java.util.Arrays;

public class connection {
    private DatagramSocket socket;
    private InetAddress ip;
    private int port;
    public boolean listening = false;

    public connection(String ip, int port) {
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


    public void sendData(byte[] data){
        // go through processes to send - full layers
        try{
            //transform data through layers.
            System.out.println("Sending to " + this.ip+" On port "+this.port+" "+new String(data));
            DatagramPacket dataPacket = new DatagramPacket(data, data.length, this.ip, this.port);
            socket.send(dataPacket);
        }catch ( IOException e){
            System.out.println("Error sending data");
            e.printStackTrace();
        }

    }
    public connection listen(){
        // listen to incoming
        try{
            this.socket = new DatagramSocket(port);
            this.listening = true;
            System.out.println("Listening on port "+port);
            new Thread(()->{
                while (this.listening){
                    try{
                        DatagramPacket incomingPacket = new DatagramPacket(new byte[40], 40);
                        socket.receive(incomingPacket);
                        System.out.println("Received data from "+incomingPacket.getAddress());
                        System.out.println(new String(incomingPacket.getData()));
                    }catch (IOException e){
                        System.out.println("Error receiving data");
                        e.printStackTrace();
                    }
                }
                
            }).start();
            return this;
        }catch (SocketException e){
            System.out.println("Socket Error");
            System.out.println(e);
            return null;
        }

    }
    private void handshake(){
        // test if otherside available
        //need ack bytes.
        //try to send the numbers
        //do diffie hellman
        //then send the aes key for encryption.
    }
}
