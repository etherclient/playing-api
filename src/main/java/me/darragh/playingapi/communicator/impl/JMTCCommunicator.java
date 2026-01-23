package me.darragh.playingapi.communicator.impl;

import me.darragh.playingapi.communicator.Communicator;
import org.jetbrains.annotations.NotNull;

import java.awt.image.BufferedImage;

/**
 * A {@link Communicator} implementation for Windows media integration, using SystemMediaTransportControls.
 *
 * @author darraghd493
 * @since 1.0.0
 */
public class JMTCCommunicator implements Communicator {
    // stub
    @Override
    public native @NotNull String getTitle();

    @Override
    public native @NotNull String getAuthor();

    @Override
    public native @NotNull String getAlbum();

    @Override
    public native int getDurationSeconds();

    @Override
    public native int getPlayedSeconds();

    @Override
    public native BufferedImage getAuthorImageData();

    @Override
    public native boolean isAuthorImageDataAvailable();

    @Override
    public native BufferedImage getAlbumImageData();

    @Override
    public native boolean isAlbumImageDataAvailable();

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }
}
