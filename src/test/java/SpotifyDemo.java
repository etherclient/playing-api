import me.darragh.playingapi.communicator.CommunicatorFactory;
import me.darragh.playingapi.communicator.impl.spotify.SimpleSpotifyPageHandler;
import me.darragh.playingapi.communicator.impl.spotify.SpotifyCommunicator;
import me.darragh.playingapi.communicator.impl.spotify.SpotifyServerHandler;
import me.darragh.playingapi.communicator.impl.spotify.SpotifyServerResponseState;

import javax.swing.*;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A demo class for {@link SpotifyCommunicator}.
 *
 * @author darraghd493
 * @since 1.0.0
 */
public final class SpotifyDemo {
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    public static void main(String[] args) throws IOException {
        SpotifyCommunicator communicator = CommunicatorFactory.createSpotifyCommunicator("029c432099274f84aff54b58ac280cf6", "http://127.0.0.1:4375");
        SpotifyServerHandler handler = new SpotifyServerHandler(
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
                                //noinspection InfiniteLoopStatement
                                while (true) {
                                    System.out.println("-----------------------");
                                    System.out.println("Title: " + communicator.getTitle());
                                    System.out.println("Arist: " + communicator.getArtist());
                                    System.out.println("Album: " + communicator.getAlbum());
                                    try {
                                        //noinspection BusyWait
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
        handler.start();
        System.out.println("Authenticate: " + communicator.getCodeUri());
    }
}
