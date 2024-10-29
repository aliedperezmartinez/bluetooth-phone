package com.javadruid.bluez.phone.lib.interfaces;

import java.util.Collections;
import java.util.Map;
import org.freedesktop.dbus.DBusMap;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.ObjectPath;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.types.Variant;
/**
 * @see https://github.com/rilmodem/ofono/blob/master/doc/voicecallmanager-api.txt
 */
@DBusInterfaceName(VoiceCallManager.DBUS_INTERFACE_NAME)
public interface VoiceCallManager extends DBusInterface {

    static final String DBUS_INTERFACE_NAME = "org.ofono.VoiceCallManager";

    ObjectPath[] CreateMultiparty();

    ObjectPath Dial(String number, String hide_callerid);

    void DialLast();

    void DialMemory(int memory_location);

    DBusMap<ObjectPath, Variant<?>> GetCalls();

    DBusMap<String, Variant<?>> GetProperties();

    void HangupAll();

    void HangupMultiparty();

    void HoldAndAnswer();

    ObjectPath PrivateChat(ObjectPath[] calls);

    void ReleaseAndAnswer();

    void ReleaseAndSwap();

    void SendTones(String SendTones);

    void SwapCalls();

    void Transfer();

    public static class CallAdded extends DBusSignal {
        private final Map<String, Variant<?>> properties;
        private final DBusPath callPath;

        public CallAdded(String path, DBusPath callPath, Map<String, Variant<?>> properties) throws DBusException {
            super(path, callPath);
            this.properties = properties;
            this.callPath = callPath;
        }

        public Map<String, Variant<?>> getProperties() {
            return Collections.unmodifiableMap(properties);
        }

        public DBusPath getCallPath() {
            return callPath;
        }

    }

    public static class CallRemoved extends DBusSignal {
        private final DBusPath callPath;

        public CallRemoved(String path, DBusPath callPath) throws DBusException {
            super(path, callPath);
            this.callPath = callPath;
        }

        public DBusPath getCallPath() {
            return callPath;
        }

    }

}
