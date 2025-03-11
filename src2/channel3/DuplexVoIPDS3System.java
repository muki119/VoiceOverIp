package channel3;

/**
 * CMPC3M06 Coursework 1
 *
 *  VOIP system test by starting the threads
 *
 * @author Sibtain Syed
 */

public class DuplexVoIPDS3System {

    public static void main(String[] args) {
        Thread senderThread = new Thread(new AudioSenderDS3Thread());
        Thread receiverThread = new Thread(new AudioReceiverDS3Thread());

        senderThread.start();
        receiverThread.start();

    }


}
