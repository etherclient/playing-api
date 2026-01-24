import me.darragh.playingapi.communicator.CommunicatorFactory;
import me.darragh.playingapi.communicator.impl.smtc.SMTCCommunicator;

public class SMTCDemo {
    public static void main(String[] args) {
        SMTCCommunicator communicator = CommunicatorFactory.createSMTCCommunicator();
        System.out.println(communicator.getTitle());
        System.out.println(communicator.getArtist());
        System.out.println(communicator.getAlbum());
    }
}
