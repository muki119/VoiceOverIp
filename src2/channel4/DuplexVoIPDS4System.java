package channel4;

/**
 * CMPC3M06 Coursework 1
 *
 *  VOIP system test by starting the threads
 *
 * @author Sibtain Syed
 */

public class DuplexVoIPDS4System {

    public static void main(String[] args) {
        Thread senderThread = new Thread(new AudioSenderDS4Thread());
        Thread receiverThread = new Thread(new AudioReceiverDS4Thread());

        senderThread.start();
        receiverThread.start();

    }


}
