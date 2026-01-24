import me.darragh.playingapi.communicator.CommunicatorFactory;
import me.darragh.playingapi.communicator.impl.spotify.SimpleSpotifyPageHandler;
import me.darragh.playingapi.communicator.impl.spotify.SpotifyCommunicator;
import me.darragh.playingapi.communicator.impl.spotify.SpotifyServerHandler;
import me.darragh.playingapi.communicator.impl.spotify.SpotifyServerResponseState;

import javax.swing.*;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SpotifyDemo {
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    public static void main(String[] args) {
        SpotifyCommunicator communicator = CommunicatorFactory.createSpotifyCommunicator("029c432099274f84aff54b58ac280cf6", "http://127.0.0.1:4375");
        SpotifyServerHandler handler = new SpotifyServerHandler(
                communicator,
                new SimpleSpotifyPageHandler(),
                exchange -> {
                    String query = exchange.getRequestURI().getQuery();
                    if (query != null && query.contains("code=")) {
                        String code = query.split("code=")[1].split("&")[0];
                        System.out.println("Authorisation Code: " + code);
                        try {
                            communicator.handleAuthorisationCode(code);

                            SwingUtilities.invokeLater(() ->
                                    new ImageViewer(communicator)
                            );

                            EXECUTOR.execute(() -> {
                                while (true) {
                                    System.out.println("-----------------------");
                                    System.out.println("Title: " + communicator.getTitle());
                                    System.out.println("Arist: " + communicator.getArtist());
                                    System.out.println("Album: " + communicator.getAlbum());
                                    try {
                                        Thread.sleep(1000L);
                                    } catch (InterruptedException ignored) {
                                    }
                                }
                            });

                             return SpotifyServerResponseState.SUCCESS;
                        } catch (Exception e) {
                            e.printStackTrace(); // for debugging
                            return SpotifyServerResponseState.INVALID;
                        }
                    }
                    return SpotifyServerResponseState.FAILURE;
                },
                4375
        );
        try {
            handler.start();
            System.out.println("Authenticate: " + communicator.getCodeUri());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
