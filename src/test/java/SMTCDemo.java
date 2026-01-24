import me.darragh.playingapi.communicator.CommunicatorFactory;
import me.darragh.playingapi.communicator.impl.smtc.SMTCCommunicator;

import javax.swing.*;

public final class SMTCDemo {
    public static void main(String[] args) {
        SMTCCommunicator communicator = CommunicatorFactory.createSMTCCommunicator();
        SwingUtilities.invokeLater(() -> {
            new ImageViewer(communicator);
        });
        while (true) {
            System.out.println("-----------------------");
            System.out.println("Title: " + communicator.getTitle());
            System.out.println("Arist: " + communicator.getArtist());
            System.out.println("Album: " + communicator.getAlbum());
            System.out.println("Played Seconds: " + communicator.getPlayedSeconds());
            System.out.println("Duration Seconds: " + communicator.getDurationSeconds());
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException ignored) {
            }
        }
    }
}
