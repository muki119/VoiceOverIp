import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.concurrent.Callable;

public class Connection {
    private DatagramSocket socket;
    private InetAddress ip;
    private int port;
    private boolean listening = false;
    private boolean acknowledged = false;

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
                System.out.println("Sending to " + this.ip+" On port "+this.port+" "+new String(data));
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
            boolean acknowledged = false;
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
        new Thread(()->{
            while(!this.acknowledged){
                byte[] buffer = new byte[40];
                try{
                    DatagramPacket incomingPacket = new DatagramPacket(buffer, buffer.length);
                    socket.receive(incomingPacket);
                    String incomingEvent = new String(incomingPacket.getData()).trim();
                    if (incomingEvent.equals("HELLO")) { // if the other side says hello - you are now peer B
                        this.acknowledged = true;
                        sendData("ACK".getBytes());

                        //generate your own key 

                        System.out.println("HELLO");
                    }else if(incomingEvent.equals("ACK")){ // if the other side acknowledges - you are now peer A
                        this.acknowledged = true;
                        //do diffie helman
                        System.out.println("ACK");
                    }
                }catch (IOException e){
                    System.out.println("Error receiving data");
                    e.printStackTrace();
                }


            }
        }).start();




    }
}
