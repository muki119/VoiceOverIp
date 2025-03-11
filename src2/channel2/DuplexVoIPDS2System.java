package channel2;

/**
 * CMPC3M06 Coursework 1
 *
 *  VOIP system test by starting the threads
 *
 * @author Sibtain Syed
 */

public class DuplexVoIPDS2System {

    public static void main(String[] args) {
        Thread senderThread = new Thread(new AudioSenderDS2Thread());
        Thread receiverThread = new Thread(new AudioReceiverDS2Thread());

        senderThread.start();
        receiverThread.start();

    }


}
