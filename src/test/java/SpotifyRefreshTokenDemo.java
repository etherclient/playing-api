import me.darragh.playingapi.communicator.CommunicatorFactory;
import me.darragh.playingapi.communicator.impl.spotify.SimpleSpotifyPageHandler;
import me.darragh.playingapi.communicator.impl.spotify.SpotifyCommunicator;
import me.darragh.playingapi.communicator.impl.spotify.SpotifyServerHandler;
import me.darragh.playingapi.communicator.impl.spotify.SpotifyServerResponseState;
import org.apache.hc.core5.http.ParseException;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A demo class for {@link SpotifyCommunicator} and refresh tokens.
 * The code is quite bad, but it works for demonstration purposes.
 * Note that you must consistently store the refresh token - it updates quite often.
 *
 * @author darraghd493
 * @since 1.0.0
 */
public final class SpotifyRefreshTokenDemo {
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    public static void main(String[] args) throws IOException, ParseException, SpotifyWebApiException {
        // attempt to read token.txt
        String refreshToken = null;
        Path tokenPath = Paths.get("token.txt");
        if (tokenPath.toFile().exists()) {
            refreshToken = Files.readString(tokenPath);
        }

        if (refreshToken == null) {
            SpotifyCommunicator communicator = CommunicatorFactory.createSpotifyCommunicator("029c432099274f84aff54b58ac280cf6", "http://127.0.0.1:4375");
            communicator.setOnRefreshTokenUpdated(newRefreshToken -> {
                try {
                    saveRefreshToken(newRefreshToken);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            SpotifyServerHandler handler = new SpotifyServerHandler(
                    new SimpleSpotifyPageHandler(),
                    exchange -> {
                        String query = exchange.getRequestURI().getQuery();
                        if (query != null && query.contains("code=")) {
                            String code = query.split("code=")[1].split("&")[0];
//                            System.out.println("Authorisation Code: " + code);
                            System.out.println("Not using refresh token yet... restart to demo.");
                            try {
                                communicator.handleAuthorisationCode(code);
                                return SpotifyServerResponseState.SUCCESS;
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                        return SpotifyServerResponseState.FAILURE;
                    },
                    4375
            );
            handler.start();
            System.out.println("Authenticate: " + communicator.getCodeUri());
        } else {
            demo(refreshToken);
        }
    }

    public static void demo(String refreshToken) throws IOException, ParseException, SpotifyWebApiException {
//        System.out.println("Using refresh token: " + refreshToken);
        SpotifyCommunicator communicator = CommunicatorFactory.createSpotifyCommunicator("029c432099274f84aff54b58ac280cf6", "http://127.0.0.1:4375");
        communicator.setOnRefreshTokenUpdated(newRefreshToken -> {
            try {
                saveRefreshToken(newRefreshToken);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        communicator.handleRefreshToken(refreshToken);
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
    }

    private static void saveRefreshToken(String refreshToken) throws IOException {
//        System.out.println("New refresh token: " + refreshToken);
        Path tokenPath = Paths.get("token.txt");
        Files.writeString(tokenPath, refreshToken);
    }
}
