package channel;

/**
 * CMPC3M06 Coursework 1
 *
 *  VOIP system test by starting the threads
 *
 * @author Sibtain Syed
 */

public class DuplexVoIPSystem {

    public static void main(String[] args) {
        Thread senderThread = new Thread(new AudioSenderThread());
        Thread receiverThread = new Thread(new AudioReceiverThread());

        senderThread.start();
        receiverThread.start();

    }


}
