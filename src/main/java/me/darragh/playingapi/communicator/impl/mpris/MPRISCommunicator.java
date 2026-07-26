package me.darragh.playingapi.communicator.impl.mpris;

import lombok.RequiredArgsConstructor;
import me.darragh.playingapi.communicator.Communicator;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBus;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.types.Variant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URI;
import java.net.URL;
import javax.imageio.ImageIO;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * A communicator implementation that interacts with the D-Bus (MPRIS spec) on Linux.
 *
 * @see <a href="https://specifications.freedesktop.org/mpris/latest/">specification</a>
 * @author darraghd493
 * @since 1.0.0
 */
@RequiredArgsConstructor
public class MPRISCommunicator implements Communicator {
    private static final long POSITION_FETCH_DELAY_MS = 1000L; // dbus calls are expensive - this might lag behind, but it is good enough xd

    private static final String PLAYING_STATUS = "Playing";
    private static final String MPRIS_PREFIX = "org.mpris.MediaPlayer2.";
    private static final String MPRIS_PLAYER_PREFIX = "org.mpris.MediaPlayer2.Player";

    private DBusConnection connection;
    private Properties playerProperties;
    private String currentPlayer = null;

    //region Caching
    private volatile String cachedTitle = "", cachedArtist = "", cachedAlbum = "", cachedArtUrl = "";
    private volatile int cachedDurationSeconds = 0;
    private volatile boolean cachedPaused = true;

    private volatile int cachedPositionSeconds = 0;
    private volatile long lastPositionFetchMs = 0;
    private volatile boolean fetchingPosition = false;
    //endregion

    private final @Nullable String mprisBusName;

    @Override
    public void start() {
        try {
            this.connection = DBusConnectionBuilder.forSessionBus().build();

            if (this.mprisBusName != null) {
                this.switchPlayer(this.mprisBusName);
                return;
            }

            DBus dbus = this.connection.getRemoteObject("org.freedesktop.DBus", "/org/freedesktop/DBus", DBus.class);
            this.selectInitialPlayer(dbus);

            this.connection.addSigHandler(DBus.NameOwnerChanged.class, sig -> {
                if (sig.getName().startsWith(MPRIS_PREFIX)) {
                    this.refreshPlayerList(dbus);
                }
            });

            this.connection.addSigHandler(Properties.PropertiesChanged.class, sig -> {
                if (!MPRIS_PLAYER_PREFIX.equals(sig.getInterfaceName())) return;

                Map<String, Variant<?>> changed = sig.getPropertiesChanged();

                if (changed.containsKey("PlaybackStatus")) {
                    String status = unwrap(changed.get("PlaybackStatus")).toString();

                    boolean isPlaying = PLAYING_STATUS.equalsIgnoreCase(status);
                    this.cachedPaused = !isPlaying;

                    if (isPlaying && !this.isSameSender(sig.getSource(), this.currentPlayer)) {
                        this.switchPlayer(sig.getSource());
                    }
                }

                if (changed.containsKey("Metadata")) {
                    this.updateMetadataCache(changed.get("Metadata"));
                }
            });
        } catch (DBusException e) {
            throw new RuntimeException("Failed to init MPRIS", e);
        }
    }

    @Override
    public void stop() {
        if (this.connection != null) {
            this.connection.disconnect();
        }
    }

    @Override
    public @NotNull String getTitle() {
        return this.cachedTitle;
    }

    @Override
    public @NotNull String getArtist() {
        return this.cachedArtist;
    }

    @Override
    public @NotNull String getAlbum() {
        return this.cachedAlbum;
    }

    @Override
    public int getDurationSeconds() {
        return this.cachedDurationSeconds;
    }

    //region getPlayedSeconds
    @Override
    public int getPlayedSeconds() {
        long now = System.currentTimeMillis();

        if (now - this.lastPositionFetchMs > POSITION_FETCH_DELAY_MS && !this.fetchingPosition && this.playerProperties != null) {
            this.fetchingPosition = true;
            CompletableFuture.runAsync(this::fetchPosition);
        }

        return this.cachedPositionSeconds;
    }

    private void fetchPosition() {
        try {
            if (this.playerProperties != null) {
                Object position = this.playerProperties.Get(MPRIS_PLAYER_PREFIX, "Position");
                Object unwrapped = this.unwrap(position);
                if (unwrapped instanceof Number n) {
                    this.cachedPositionSeconds = (int) (n.longValue() / 1_000_000);
                    this.lastPositionFetchMs = System.currentTimeMillis();
                }
            }
        } catch (Exception ignored) {
        } finally {
            this.fetchingPosition = false;
        }
    }
    //endregion

    @Override
    public boolean isPaused() {
        return this.cachedPaused;
    }

    @Override
    public void playMedia() {
        CompletableFuture.runAsync(() -> {
            MPRISPlayer player = this.getPlayer();
            if (player != null) player.Play();
        });
    }

    @Override
    public void pauseMedia() {
        CompletableFuture.runAsync(() -> {
            MPRISPlayer player = this.getPlayer();
            if (player != null) player.Pause();
        });
    }

    @Override
    public void nextMedia() {
        CompletableFuture.runAsync(() -> {
            MPRISPlayer player = this.getPlayer();
            if (player != null) player.Next();
        });
    }

    @Override
    public void previousMedia() {
        CompletableFuture.runAsync(() -> {
            MPRISPlayer player = this.getPlayer();
            if (player != null) player.Previous();
        });
    }

    @Override
    public @Nullable BufferedImage getArtistImageData() {
        return null;
    }

    @Override
    public boolean isArtistImageDataAvailable() {
        return false;
    }

    @Override
    public @Nullable BufferedImage getAlbumImageData() {
        return getImageFromUrl(this.cachedArtUrl);
    }

    @Override
    public boolean isAlbumImageDataAvailable() {
        return !this.cachedArtUrl.isEmpty();
    }

    //region Metadata Handling
    /**
     * Recursively unwraps D-Bus Variants to get the underlying value.
     *
     * @return The unwrapped object.
     */
    private Object unwrap(Object object) {
        while (object instanceof Variant<?> v) {
            object = v.getValue();
        }
        return object;
    }

    /**
     * Pre-parses the D-Bus metadata ONCE on the D-Bus worker thread when signals arrive.
     *
     * @param rawMetadata The raw metadata object.
     */
    private void updateMetadataCache(Object rawMetadata) {
        Object unwrapped = unwrap(rawMetadata);
        if (!(unwrapped instanceof Map<?, ?> map)) return;

        String title = "", artist = "", album = "", artUrl = "";
        int duration = 0;

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = String.valueOf(unwrap(entry.getKey()));
            Object value = unwrap(entry.getValue());

            switch (key) {
                case "xesam:title" -> title = parseMetadataStringValue(value);
                case "xesam:artist" -> artist = parseMetadataStringValue(value);
                case "xesam:album" -> album = parseMetadataStringValue(value);
                case "mpris:artUrl" -> artUrl = parseMetadataStringValue(value);
                case "mpris:length" -> {
                    if (value instanceof Number n) {
                        duration = (int) (n.longValue() / 1_000_000);
                    }
                }
            }
        }

        this.cachedTitle = title;
        this.cachedArtist = artist;
        this.cachedAlbum = album;
        this.cachedArtUrl = artUrl;
        this.cachedDurationSeconds = duration;
    }

    /**
     * Converts a raw metadata value to a string representation.
     * <ul>
     *     <li>unmodified for individual strings</li>
     *     <li>comma deliminated for lists/or arrays of objects</li>
     * </ul>
     *
     * @param value The raw metadata value to parse.
     * @return The string representation of the value.
     */
    private String parseMetadataStringValue(Object value) {
        if (value == null) return "";
        if (value instanceof List<?> list) {
            StringBuilder builder = new StringBuilder();
            for (Object item : list) {
                Object unwrapped = unwrap(item);
                if (unwrapped != null && !unwrapped.toString().isEmpty()) {
                    if (!builder.isEmpty()) builder.append(", ");
                    builder.append(unwrapped);
                }
            }
            return builder.toString();
        }
        if (value.getClass().isArray()) {
            StringBuilder builder = new StringBuilder();
            for (Object item : (Object[]) value) {
                Object unwrapped = unwrap(item);
                if (unwrapped != null && !unwrapped.toString().isEmpty()) {
                    if (!builder.isEmpty()) builder.append(", ");
                    builder.append(unwrapped);
                }
            }
            return builder.toString();
        }
        return String.valueOf(value);
    }
    //endregion

    //region Player Handling
    private void selectInitialPlayer(DBus dbus) {
        try {
            String selected = null;
            for (String name : dbus.ListNames()) {
                if (!name.startsWith(MPRIS_PREFIX)) continue;
                if (this.isPlaying(name)) {
                    selected = name;
                    break;
                }
                if (selected == null) selected = name;
            }
            if (selected != null && !selected.equals(this.currentPlayer)) {
                this.switchPlayer(selected);
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Refreshes the list of available MPRIS players and switches to a new one if necessary.
     *
     * @param dbus The D-Bus interface.
     */
    private void refreshPlayerList(DBus dbus) {
        try {
            String playingPlayer = null, firstFound = null;

            for (String name : dbus.ListNames()) {
                if (!name.startsWith(MPRIS_PREFIX)) continue;

                if (this.isPlaying(name)) {
                    playingPlayer = name;
                    break; // priority: find anyone currently playing
                }
                if (firstFound == null) firstFound = name;
            }

            String toSelect = (playingPlayer != null) ? playingPlayer : firstFound;
            if (toSelect != null && !toSelect.equals(currentPlayer)) {
                this.switchPlayer(toSelect);
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Switches the current player to the specified bus name.
     *
     * @param bus The bus name of the new player.
     */
    private void switchPlayer(String bus) {
        try {
            this.playerProperties = this.connection.getRemoteObject(bus, "/org/mpris/MediaPlayer2", Properties.class);
            this.currentPlayer = bus;

            Object rawMetadata = this.playerProperties.Get(MPRIS_PLAYER_PREFIX, "Metadata");
            this.updateMetadataCache(rawMetadata);

            Object status = this.playerProperties.Get(MPRIS_PLAYER_PREFIX, "PlaybackStatus");
            if (status != null) {
                this.cachedPaused = !PLAYING_STATUS.equalsIgnoreCase(unwrap(status).toString());
            }

            this.fetchPosition();
        } catch (Exception ignored) {
        }
    }

    /**
     * Checks if the specified player is currently playing.
     *
     * @param bus The bus name of the player.
     * @return True if the player is playing, false otherwise.
     */
    private boolean isPlaying(String bus) {
        try {
            Properties properties = this.connection.getRemoteObject(bus, "/org/mpris/MediaPlayer2", Properties.class);
            Object status = this.unwrap(properties.Get(MPRIS_PLAYER_PREFIX, "PlaybackStatus"));
            return status != null && PLAYING_STATUS.equalsIgnoreCase(status.toString());
        } catch (Exception ignored) {
        }
        return false;
    }

    /**
     * Compares a signal source (Unique Name) with the current player (Well-Known Name).
     *
     * @param signalSource The signal source unique name.
     * @param currentBusName The current player well-known name.
     * @return True if they refer to the same sender, false otherwise.
     */
    private boolean isSameSender(String signalSource, String currentBusName) {
        if (signalSource == null || currentBusName == null) return false;
        if (signalSource.equals(currentBusName)) return true;

        try {
            DBus dbus = this.connection.getRemoteObject("org.freedesktop.DBus", "/org/freedesktop/DBus", DBus.class);
            String uniqueName = dbus.GetNameOwner(currentBusName);
            return signalSource.equals(uniqueName);
        } catch (Exception e) {
            return false;
        }
    }
    //endregion

    /**
     * Retrieves an image from a URL or file path.
     *
     * @param url The URL or file path.
     * @return The image, or null if not found.
     */
    private static @Nullable BufferedImage getImageFromUrl(String url) {
        if (url == null || url.isEmpty()) return null;
        try {
            if (url.startsWith("file://")) {
                return ImageIO.read(new File(new URI(url)));
            }
            //noinspection deprecation
            return ImageIO.read(new URL(url));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Gets the {@link MPRISPlayer} interface for the current player.
     *
     * @return The {@link MPRISPlayer} interface, or null if not available.
     */
    private @Nullable MPRISPlayer getPlayer() {
        if (this.connection == null || this.currentPlayer == null) return null;
        try {
            return this.connection.getRemoteObject(this.currentPlayer, "/org/mpris/MediaPlayer2", MPRISPlayer.class);
        } catch (DBusException e) {
            return null;
        }
    }
}