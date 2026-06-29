package me.darragh.playingapi.communicator.impl.spotify;

import lombok.Getter;
import lombok.Setter;
import me.darragh.playingapi.communicator.Communicator;
import org.apache.hc.core5.http.ParseException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.SpotifyHttpManager;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.model_objects.miscellaneous.CurrentlyPlayingContext;
import se.michaelthelin.spotify.model_objects.specification.Artist;
import se.michaelthelin.spotify.model_objects.specification.ArtistSimplified;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;
import se.michaelthelin.spotify.requests.authorization.authorization_code.pkce.AuthorizationCodePKCERequest;
import se.michaelthelin.spotify.requests.data.player.GetInformationAboutUsersCurrentPlaybackRequest;
import se.michaelthelin.spotify.requests.data.tracks.GetTrackRequest;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * A {@link Communicator} implementation for Spotify.
 *
 * @author darraghd493
 * @since 1.0.0
 */
public class SpotifyCommunicator implements Communicator {
    private static final String CODE_VERIFIER_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
    private static final int CURRENTLY_PLAYING_POLL_INTERVAL_MS = 500;

    @Setter
    public static @Nullable Consumer<Exception> onExecutorException = null;

    private final SpotifyApi api;
    private final AuthorizationCodeUriRequest codeUri;

    private final String codeVerifier = generateCodeVerifier();

    @Getter
    private @Nullable Track currentTrack;
    @Getter
    private @Nullable CurrentlyPlayingContext currentPlayingContext;

    @Getter
    private boolean active;
    @Getter
    private boolean authenticated;

    @Getter
    @Setter
    private int tokenRefreshInterval = 2;

    @Getter
    @Setter
    private @Nullable Consumer<String> onRefreshTokenUpdated = null;

    private volatile BufferedImage cachedAlbumImage = null;
    private volatile String cachedAlbumId = null;

    private volatile BufferedImage cachedAuthorImage = null;
    private volatile String cachedAuthorId = null;

    private final ConcurrentHashMap<String, CompletableFuture<BufferedImage>> albumPending = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CompletableFuture<BufferedImage>> authorPending = new ConcurrentHashMap<>();

    private volatile boolean albumLoading = false;
    private volatile String albumDownloadForId = null;

    private volatile boolean authorLoading = false;
    private volatile String authorDownloadForId = null;

    public SpotifyCommunicator(@NotNull String clientId, @NotNull String redirectUri) {
        this.api = new SpotifyApi.Builder()
                .setClientId(clientId)
                .setRedirectUri(SpotifyHttpManager.makeUri(redirectUri))
                .build();
        String challengeHash = produceChallengeHash(this.codeVerifier);
        this.codeUri = this.api.authorizationCodePKCEUri(challengeHash)
                .scope("user-read-playback-state user-read-playback-position user-modify-playback-state user-read-currently-playing")
                .build();
    }

    @Override
    public @NotNull String getTitle() {
        return this.currentTrack != null ? this.currentTrack.getName() : UNKNOWN_STRING;
    }

    @Override
    public @NotNull String getArtist() {
        return this.currentTrack != null && this.currentTrack.getArtists().length > 0 ?
                String.join(", ", Arrays.stream(this.currentTrack.getArtists())
                        .map(ArtistSimplified::getName)
                        .toArray(String[]::new)) : UNKNOWN_STRING;
    }

    @Override
    public @NotNull String getAlbum() {
        return this.currentTrack != null && this.currentTrack.getAlbum() != null ?
                this.currentTrack.getAlbum().getName() : UNKNOWN_STRING;
    }

    @Override
    public int getDurationSeconds() {
        return this.currentTrack != null ? this.currentTrack.getDurationMs() / 1000 : UNKNOWN_DURATION;
    }

    @Override
    public int getPlayedSeconds() {
        return this.currentPlayingContext != null && this.currentPlayingContext.getProgress_ms() != null ?
                this.currentPlayingContext.getProgress_ms() / 1000 : UNKNOWN_DURATION;
    }

    @Override
    public boolean isPaused() {
        return this.currentPlayingContext == null || this.currentPlayingContext.getIs_playing() == null || !this.currentPlayingContext.getIs_playing();
    }

    @Override
    public @Nullable BufferedImage getArtistImageData() {
        Track track = this.currentTrack;
        if (track == null || track.getArtists() == null || track.getArtists().length == 0) return null;

        String artistId = track.getArtists()[0].getId();
        if (artistId == null) return null;

        if (artistId.equals(this.cachedAuthorId) && this.cachedAuthorImage != null) {
            return this.cachedAuthorImage;
        }

        if (artistId.equals(this.authorDownloadForId) && this.authorLoading) {
            return null;
        }

        synchronized (this) {
            if (artistId.equals(this.cachedAuthorId) && this.cachedAuthorImage != null) return this.cachedAuthorImage;
            if (artistId.equals(this.authorDownloadForId) && this.authorLoading) return null;

            this.authorDownloadForId = artistId;
            this.authorLoading = true;

            EXECUTOR.execute(() -> {
                try {
                    Artist artist = this.api.getArtist(artistId).build().execute();
                    if (artist != null && artist.getImages() != null && artist.getImages().length > 0) {
                        String url = artist.getImages()[0].getUrl(); // pick the first (usually largest)
                        if (url != null && !url.isEmpty()) {
                            BufferedImage image = ImageIO.read(URI.create(url).toURL());
                            if (image != null) {
                                this.cachedAuthorImage = image;
                                this.cachedAuthorId = artistId;
                                CompletableFuture<BufferedImage> future = authorPending.remove(artistId);
                                if (future != null) future.complete(image);
                            }
                        }
                    }
                } catch (Exception e) {
                    if (onExecutorException != null) onExecutorException.accept(e);
                    CompletableFuture<BufferedImage> future = authorPending.remove(artistId);
                    if (future != null) future.completeExceptionally(e);
                } finally {
                    if (artistId.equals(this.authorDownloadForId)) {
                        this.authorLoading = false;
                        this.authorDownloadForId = null;
                    }
                }
            });

            return null;
        }
    }

    @Override
    public boolean isArtistImageDataAvailable() {
        Track track = this.currentTrack;
        if (track == null || track.getArtists() == null || track.getArtists().length == 0) return false;

        String artistId = track.getArtists()[0].getId();
        if (artistId == null) return false;

        if (artistId.equals(this.cachedAuthorId) && this.cachedAuthorImage != null) return true;
        if (artistId.equals(this.authorDownloadForId) && this.authorLoading) return false;

        try {
            Artist artist = this.api.getArtist(artistId).build().execute();
            if (artist != null && artist.getImages() != null && artist.getImages().length > 0) {
                String url = artist.getImages()[0].getUrl();
                return url != null && !url.isEmpty();
            }
        } catch (Exception ignored) {
        }

        return false;
    }

    @Override
    public @Nullable BufferedImage getAlbumImageData() {
        Track track = this.currentTrack;
        if (track == null || track.getAlbum() == null || track.getAlbum().getId() == null) return null;

        String albumId = track.getAlbum().getId();
        if (albumId.equals(this.cachedAlbumId) && this.cachedAlbumImage != null) {
            return this.cachedAlbumImage;
        }

        if (albumId.equals(this.albumDownloadForId) && this.albumLoading) return null;

        synchronized (this) {
            if (albumId.equals(this.cachedAlbumId) && this.cachedAlbumImage != null) return this.cachedAlbumImage;
            if (albumId.equals(this.albumDownloadForId) && this.albumLoading) return null;

            this.albumDownloadForId = albumId;
            this.albumLoading = true;

            EXECUTOR.execute(() -> {
                try {
                    if (track.getAlbum().getImages() != null && track.getAlbum().getImages().length > 0) {
                        String url = track.getAlbum().getImages()[0].getUrl();
                        if (url != null && !url.isEmpty()) {
                            BufferedImage image = ImageIO.read(URI.create(url).toURL());
                            if (image != null) {
                                this.cachedAlbumImage = image;
                                this.cachedAlbumId = albumId;
                                CompletableFuture<BufferedImage> future = this.albumPending.remove(albumId);
                                if (future != null) future.complete(image);
                            }
                        }
                    }
                } catch (Exception e) {
                    if (onExecutorException != null) onExecutorException.accept(e);
                    CompletableFuture<BufferedImage> f = this.albumPending.remove(albumId);
                    if (f != null) f.completeExceptionally(e);
                } finally {
                    if (albumId.equals(this.albumDownloadForId)) {
                        this.albumLoading = false;
                        this.albumDownloadForId = null;
                    }
                }
            });

            return null;
        }
    }

    @Override
    public boolean isAlbumImageDataAvailable() {
        Track track = this.currentTrack;
        if (track == null || track.getAlbum() == null) return false;

        String albumId = track.getAlbum().getId();
        if (albumId != null && albumId.equals(this.cachedAlbumId) && this.cachedAlbumImage != null) return true;
        if (albumId != null && albumId.equals(this.albumDownloadForId) && this.albumLoading) return false;

        if (track.getAlbum().getImages() != null && track.getAlbum().getImages().length > 0) {
            String url = track.getAlbum().getImages()[0].getUrl();
            return url != null && !url.isEmpty();
        }

        return false;
    }

    @Override
    public void start() {
        // no-op
    }

    @Override
    public void stop() {
        this.active = false;
        this.authenticated = false;

        synchronized (this) {
            this.cachedAlbumImage = null;
            this.cachedAlbumId = null;
            this.cachedAuthorImage = null;
            this.cachedAuthorId = null;

            this.albumLoading = false;
            this.albumDownloadForId = null;
            this.authorLoading = false;
            this.authorDownloadForId = null;
        }

        Exception stopException = new IllegalStateException("Communicator stopped");
        this.albumPending.forEach((id, f) -> f.completeExceptionally(stopException));
        this.authorPending.forEach((id, f) -> f.completeExceptionally(stopException));
        this.albumPending.clear();
        this.authorPending.clear();
    }

    @Override
    public void playMedia() {
        if (!this.active) return;
        try {
            this.api.startResumeUsersPlayback().build().executeAsync();
        } catch (Exception e) {
            if (onExecutorException != null) onExecutorException.accept(e);
        }
    }

    @Override
    public void pauseMedia() {
        if (!this.active) return;
        try {
            this.api.pauseUsersPlayback().build().executeAsync();
        } catch (Exception e) {
            if (onExecutorException != null) onExecutorException.accept(e);
        }
    }

    @Override
    public void nextMedia() {
        if (!this.active) return;
        try {
            this.api.skipUsersPlaybackToNextTrack().build().executeAsync();
        } catch (Exception e) {
            if (onExecutorException != null) onExecutorException.accept(e);
        }
    }

    @Override
    public void previousMedia() {
        if (!this.active) return;
        try {
            this.api.skipUsersPlaybackToPreviousTrack().build().executeAsync();
        } catch (Exception e) {
            if (onExecutorException != null) onExecutorException.accept(e);
        }
    }

    /**
     * Gets the authorisation code URI for Spotify authentication.
     *
     * @return The authorisation code URI as a string.
     */
    public String getCodeUri() {
        return this.codeUri.execute().toString();
    }

    /**
     * Handles the provided callback code from the Spotify authorisation flow.
     *
     * @param code the authorisation code received from the Spotify callback.
     * @throws IOException The method may throw an IOException.
     * @throws ParseException The method may throw a ParseException.
     * @throws SpotifyWebApiException The method may throw a SpotifyWebApiException.
     */
    public void handleAuthorisationCode(@NotNull String code) throws IOException, ParseException, SpotifyWebApiException {
        if (this.authenticated) {
            throw new IllegalStateException("Already authenticated");
        }

        AuthorizationCodePKCERequest pkceRequest = this.api.authorizationCodePKCE(code, this.codeVerifier).build();
        AuthorizationCodeCredentials credentialsRequest = pkceRequest.execute();

        this.api.setAccessToken(credentialsRequest.getAccessToken());
        this.api.setRefreshToken(credentialsRequest.getRefreshToken());

        this.tokenRefreshInterval = credentialsRequest.getExpiresIn();
        this.active = true;
        this.authenticated = true;

        this.startTokenRefreshThread();
        this.startPlaybackThread();

        if (this.onRefreshTokenUpdated != null) {
            this.onRefreshTokenUpdated.accept(credentialsRequest.getRefreshToken());
        }
    }

    /**
     * Authenticate using an existing refresh token.
     *
     * @param refreshToken The refresh token previously obtained.
     * @throws IOException The method may throw an IOException.
     * @throws ParseException The method may throw a ParseException.
     * @throws SpotifyWebApiException The method may throw a SpotifyWebApiException.
     */
    public void handleRefreshToken(@NotNull String refreshToken) throws IOException, ParseException, SpotifyWebApiException {
        if (this.authenticated) throw new IllegalStateException("Already authenticated");

        this.api.setRefreshToken(refreshToken);

        AuthorizationCodeCredentials credentialsRequest = this.api.authorizationCodePKCERefresh().build().execute();

        this.api.setAccessToken(credentialsRequest.getAccessToken());
        this.api.setRefreshToken(credentialsRequest.getRefreshToken());
        this.tokenRefreshInterval = credentialsRequest.getExpiresIn();

        this.active = true;
        this.authenticated = true;

        this.startTokenRefreshThread();
        this.startPlaybackThread();

        if (this.onRefreshTokenUpdated != null) {
            this.onRefreshTokenUpdated.accept(credentialsRequest.getRefreshToken());
        }
    }

    /**
     * Fetches your current refresh token.
     *
     * @return The current refresh token.
     */
    public @NotNull String getRefreshToken() {
        String refreshToken = this.api.getRefreshToken();
        if (refreshToken == null) throw new IllegalStateException("Not authenticated");
        return refreshToken;
    }

    /**
     * Fetches the album image asynchronously.
     *
     * @return A {@link CompletableFuture} that will complete with the album image {@link BufferedImage}, or null if not available.
     */
    public CompletableFuture<BufferedImage> fetchAlbumImageAsync() {
        Track track = this.currentTrack;
        if (track == null || track.getAlbum() == null || track.getAlbum().getId() == null) return CompletableFuture.completedFuture(null);

        String albumId = track.getAlbum().getId();
        if (albumId.equals(this.cachedAlbumId) && this.cachedAlbumImage != null) return CompletableFuture.completedFuture(this.cachedAlbumImage);

        CompletableFuture<BufferedImage> existing = this.albumPending.get(albumId);
        if (existing != null) return existing;

        CompletableFuture<BufferedImage> future = new CompletableFuture<>();
        CompletableFuture<BufferedImage> previous = this.albumPending.putIfAbsent(albumId, future);
        if (previous != null) return previous;

        this.isAlbumImageDataAvailable();
        return future;
    }

    /**
     * Fetches the author image asynchronously.
     *
     * @return A {@link CompletableFuture} that will complete with the author image {@link BufferedImage}, or null if not available.
     */
    public CompletableFuture<BufferedImage> fetchAuthorImageAsync() {
        Track track = this.currentTrack;
        if (track == null || track.getArtists() == null || track.getArtists().length == 0) return CompletableFuture.completedFuture(null);
        String artistId = track.getArtists()[0].getId();
        if (artistId == null) return CompletableFuture.completedFuture(null);
        if (artistId.equals(this.cachedAuthorId) && this.cachedAuthorImage != null) return CompletableFuture.completedFuture(this.cachedAuthorImage);

        CompletableFuture<BufferedImage> existing = this.authorPending.get(artistId);
        if (existing != null) return existing;

        CompletableFuture<BufferedImage> future = new CompletableFuture<>();
        CompletableFuture<BufferedImage> previous = this.authorPending.putIfAbsent(artistId, future);
        if (previous != null) return previous;

        this.isArtistImageDataAvailable();
        return future;
    }

    /**
     * Starts the token refresh thread.
     */
    private void startTokenRefreshThread() {
        EXECUTOR.execute(new Thread(() -> {
            while (this.active && !Thread.currentThread().isInterrupted()) {
                try {
                    TimeUnit.SECONDS.sleep(this.tokenRefreshInterval - 2);
                    AuthorizationCodeCredentials refreshed =
                            this.api.authorizationCodePKCERefresh().build().execute();
                    this.api.setAccessToken(refreshed.getAccessToken());
                    this.api.setRefreshToken(refreshed.getRefreshToken());
                    this.tokenRefreshInterval = refreshed.getExpiresIn();
                    if (this.onRefreshTokenUpdated != null) {
                        this.onRefreshTokenUpdated.accept(refreshed.getRefreshToken());
                    }
                } catch (Exception e) {
                    if (onExecutorException != null) onExecutorException.accept(e);
                }
            }
        }, "access-token-refresh-thread"));
    }

    /**
     * Starts the playback information polling thread.
     */
    private void startPlaybackThread() {
        EXECUTOR.execute(new Thread(() -> {
            while (this.active && !Thread.currentThread().isInterrupted()) {
                try {
                    TimeUnit.MILLISECONDS.sleep(CURRENTLY_PLAYING_POLL_INTERVAL_MS);
                    GetInformationAboutUsersCurrentPlaybackRequest playbackRequest =
                            this.api.getInformationAboutUsersCurrentPlayback().build();
                    CurrentlyPlayingContext currentlyPlaying = playbackRequest.execute();

                    String trackId = currentlyPlaying.getItem().getId();
                    Track freshlyFetched = this.api.getTrack(trackId).build().execute();

                    if (!Objects.equals(
                            freshlyFetched != null ? freshlyFetched.getId() : null,
                            this.currentTrack != null ? this.currentTrack.getId() : null)) {
                        this.onTrackChanged(freshlyFetched);
                    }

                    this.currentTrack = freshlyFetched;
                    this.currentPlayingContext = currentlyPlaying;
                } catch (Exception e) {
                    if (onExecutorException != null) onExecutorException.accept(e);
                }
            }
        }, "current-playback-info-thread"));
    }

    /**
     * Handles track change events.
     *
     * @param newTrack The new track that has been changed to.
     */
    private synchronized void onTrackChanged(Track newTrack) {
        String newAlbumId = newTrack != null && newTrack.getAlbum() != null ? newTrack.getAlbum().getId() : null;
        String newArtistId = null;
        if (newTrack != null && newTrack.getArtists() != null && newTrack.getArtists().length > 0) newArtistId = newTrack.getArtists()[0].getId();

        if (this.cachedAlbumId != null && !Objects.equals(this.cachedAlbumId, newAlbumId)) {
            this.cachedAlbumImage = null;
            String old = this.cachedAlbumId;
            this.cachedAlbumId = null;
            CompletableFuture<BufferedImage> future = this.albumPending.remove(old);
            if (future != null) future.complete(null);
        }

        if (this.cachedAuthorId != null && !Objects.equals(this.cachedAuthorId, newArtistId)) {
            this.cachedAuthorImage = null;
            String old = this.cachedAuthorId;
            this.cachedAuthorId = null;
            CompletableFuture<BufferedImage> future = this.authorPending.remove(old);
            if (future != null) future.complete(null);
        }
    }

    /**
     * Generates a random code verifier string.
     *
     * @return A randomly generated code verifier.
     */
    private static @NotNull String generateCodeVerifier() {
        return new Random().ints(43, 0, CODE_VERIFIER_CHARS.length())
                .mapToObj(i -> String.valueOf(CODE_VERIFIER_CHARS.charAt(i)))
                .reduce("", String::concat);
    }

    /**
     * Produces a challenge hash from the provided code verifier.
     *
     * @param codeVerifier The code verifier to hash.
     * @return The resulting challenge hash.
     */
    private static @NotNull String produceChallengeHash(@NotNull String codeVerifier) {
        byte[] bytes = codeVerifier.getBytes(StandardCharsets.UTF_8);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getUrlEncoder().encodeToString(digest.digest(bytes)).replace("=", "");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
}
