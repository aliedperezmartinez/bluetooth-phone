package com.javadruid.bluez.phone.lib.interfaces;

import org.freedesktop.dbus.DBusMap;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.types.Variant;

/**
 * @see https://github.com/rilmodem/ofono/blob/master/doc/voicecall-api.txt
 */
@DBusInterfaceName(VoiceCall.DBUS_INTERFACE_NAME)
public interface VoiceCall extends DBusInterface {

    static final String DBUS_INTERFACE_NAME = "org.ofono.VoiceCall";

    // Methods
    void Answer();

    void Deflect(String number);

    DBusMap<String, Variant<?>> GetProperties();

    void Hangup();

    // Properties
    String LineIdentification();

    String IncomingLine();

    String Name();

    boolean Multiparty();

    String State();

    String StartTime();

    String Information();

    byte Icon();

    boolean Emergency();

    boolean RemoteHeld();

    boolean RemoteMultiparty();

    // Signals
    public static class DisconnectReason extends DBusSignal {

        private final String reason;

        public DisconnectReason(String objectpath, String reason) throws DBusException {
            super(objectpath, reason);
            this.reason = reason;
        }

        public String getReason() {
            return reason;
        }
    }

}
