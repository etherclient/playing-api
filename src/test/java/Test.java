import me.darragh.playingapi.communicator.impl.smtc.SMTCCommunicator;

public class Test {
    public static void main(String[] args) {
        SMTCCommunicator communicator = new SMTCCommunicator();
        System.out.println(communicator.getTitle());
        System.out.println(communicator.getArtist());
        System.out.println(communicator.getAlbum());
    }
}
