package me.darragh.playingapi.communicator.impl.smtc;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import me.darragh.playingapi.communicator.Communicator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A communicator implementation that interacts with the System Media Transport Controls (SMTC) on Windows.
 *
 * @see <a href="https://learn.microsoft.com/en-us/uwp/api/windows.media.systemmediatransportcontrols?view=winrt-26100">WinRT docs</a>
 * @see SMTCBridge
 * @author darraghd493
 * @since 1.0.0
 */
public final class SMTCCommunicator implements Communicator {
    private final SMTCBridge bridge;
    private final long pollingRate;

    private ScheduledExecutorService scheduler;

    //region Caching
    private volatile @NotNull String cachedTitle = "", cachedArtist = "", cachedAlbum = "";
    private volatile int cachedDurationSeconds = 0, cachedPlayedSeconds = 0;

    private volatile boolean cachedAlbumImageAvailable = false;
    private volatile @Nullable BufferedImage cachedAlbumImage = null;
    
    private volatile boolean cachedPaused = false;
    //endregion

    //region Tracking
    private @Nullable String lastProcessedTitle = null, lastProcessedAlbum = null;
    //endregion

    public SMTCCommunicator() {
        this(500L);
    }

    public SMTCCommunicator(long pollingRate) {
        this.bridge = SMTCBridge.INSTANCE;
        this.pollingRate = pollingRate;
    }

    private void updateNativeData() {
        try {
            String currentTitle = this.bridge.getTitleString(),
                    currentAlbum = this.bridge.getAlbumString();

            this.cachedTitle = currentTitle != null ? currentTitle : "";
            this.cachedArtist = this.bridge.getArtistString() != null ? this.bridge.getArtistString() : "";
            this.cachedAlbum = currentAlbum != null ? currentAlbum : "";
            this.cachedDurationSeconds = this.bridge.getDurationSeconds();
            this.cachedPlayedSeconds = this.bridge.getPlayedSeconds();
            this.cachedPaused = this.bridge.isPaused();

            if (!Objects.equals(this.lastProcessedTitle, currentTitle) || !Objects.equals(this.lastProcessedAlbum, currentAlbum)) {
                this.lastProcessedTitle = currentTitle;
                this.lastProcessedAlbum = currentAlbum;

                this.cachedAlbumImageAvailable = this.bridge.isAlbumImageAvailable();
                if (this.cachedAlbumImageAvailable) {
                    IntByReference sizeRef = new IntByReference();
                    Pointer pointer = this.bridge.getAlbumImage(sizeRef);
                    this.cachedAlbumImage = this.getBufferedImage(sizeRef, pointer);
                } else {
                    this.cachedAlbumImage = null;
                }
            }
        } catch (Throwable t) { // TODO: Better handle this
            t.printStackTrace();
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

    @Override
    public int getPlayedSeconds() {
        return this.cachedPlayedSeconds;
    }

    @Override
    public boolean isPaused() {
        return this.cachedPaused;
    }

    @Override
    public @Nullable BufferedImage getArtistImageData() {
        // hardcoded - unsupported
        return null;
    }

    @Override
    public boolean isArtistImageDataAvailable() {
        // hardcoded - unsupported
        return false;
    }

    @Override
    public @Nullable BufferedImage getAlbumImageData() {
        return this.cachedAlbumImage;
    }

    @Override
    public boolean isAlbumImageDataAvailable() {
        return this.cachedAlbumImageAvailable;
    }

    @Override
    public void start() {
        if (this.scheduler == null || this.scheduler.isShutdown()) {
            this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "SMTC-Polling-Thread");
                thread.setDaemon(true); // allow jvm shutdown
                return thread;
            });
            this.scheduler.scheduleAtFixedRate(this::updateNativeData, 0, this.pollingRate, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void stop() {
        if (this.scheduler != null && !this.scheduler.isShutdown()) {
            this.scheduler.shutdownNow();
            this.scheduler = null;
        }
    }

    @Nullable
    private BufferedImage getBufferedImage(IntByReference sizeRef, Pointer p) {
        if (p == null) return null;

        int size = sizeRef.getValue();
        if (size <= 0) return null;

        try {
            byte[] bytes = p.getByteArray(0, size);
            this.bridge.freeMemory(p);
            return ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (IOException | Error e) {
            return null;
        }
    }

    @Override
    public void playMedia() {
        this.bridge.playMedia();
    }

    @Override
    public void pauseMedia() {
        this.bridge.pauseMedia();
    }

    @Override
    public void nextMedia() {
        this.bridge.nextMedia();
    }

    @Override
    public void previousMedia() {
        this.bridge.previousMedia();
    }
}