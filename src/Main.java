import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello world!");
        connection testConnection = new connection("localhost",25565).listen();
        while(true){
            String text = new Scanner(System.in).nextLine()+'\n';
            testConnection.sendData(text.getBytes());

        }
    }
}