import me.darragh.playingapi.communicator.CommunicatorFactory;
import me.darragh.playingapi.communicator.impl.smtc.SMTCCommunicator;

import javax.swing.*;

/**
 * A demo class for {@link SMTCCommunicator}.
 *
 * @author darraghd493
 * @since 1.0.0
 */
public final class SMTCDemo {
    public static void main(String[] args) {
        SMTCCommunicator communicator = CommunicatorFactory.createSMTCCommunicator();
        SwingUtilities.invokeLater(() -> {
            new ImageViewer(communicator);
        });
        //noinspection InfiniteLoopStatement
        while (true) {
            System.out.println("-----------------------");
            System.out.println("Title: " + communicator.getTitle());
            System.out.println("Arist: " + communicator.getArtist());
            System.out.println("Album: " + communicator.getAlbum());
            System.out.println("Played Seconds: " + communicator.getPlayedSeconds());
            System.out.println("Duration Seconds: " + communicator.getDurationSeconds());
            try {
                //noinspection BusyWait
                Thread.sleep(1000L);
            } catch (InterruptedException ignored) {
            }
        }
    }
}
