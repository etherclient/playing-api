package me.darragh.playingapi.communicator.impl.mpris;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.interfaces.DBusInterface;

/**
 * An interface representing the MPRIS Player DBus interface.
 * 
 * @author darraghd493
 * @since 1.0.6
 */
@DBusInterfaceName("org.mpris.MediaPlayer2.Player")
public interface MPRISPlayer extends DBusInterface {
    void Play();

    void Pause();

    void Next();

    void Previous();
}
