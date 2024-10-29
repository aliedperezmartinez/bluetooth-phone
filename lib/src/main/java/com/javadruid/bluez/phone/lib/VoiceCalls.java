package com.javadruid.bluez.phone.lib;

import com.javadruid.bluez.phone.lib.interfaces.Manager;
import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.ObjectPath;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.types.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.Boolean.FALSE;

public class VoiceCalls implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(VoiceCalls.class);
    private static final Variant<Boolean> VARIANT_FALSE = new Variant<>(FALSE);
    static final String GET_MODEMS = "GetModems";

    private final DBusConnection conn;
    private final Manager remoteObject;

    public VoiceCalls() {
        this(getConnection());
    }

    VoiceCalls(DBusConnection conn) {
        try {
            this.conn = conn;
            remoteObject = conn.getRemoteObject(Ofono.BUS_NAME, "/", Manager.class);
        } catch (DBusException ex) {
            logger.error("Error initialising Voice Calls Manager", ex);
            throw new RuntimeException(ex);
        }
    }

    public Stream<VoiceCallManager> getVoiceCallManagers() {
        try {
            return getModems()
                .map(m -> (List<Object[]>)m)
                .flatMap(List::stream)
                .map(o -> toMapEntry(o))
                .filter(o -> isOnline(o))
                .map(Map.Entry::getKey)
                .map(DBusPath::getPath)
                .map(this::voiceCallManager)
                .filter(Objects::nonNull);
        } catch (DBusException ex) {
            logger.error("Error retrieving objects", ex);
            throw new RuntimeException(ex);
        }
    }

    public VoiceCall voiceCall(String path) {
        return new VoiceCall(conn, path);
    }

    @Override
    public void close() throws IOException {
        conn.close();
    }

    private static DBusConnection getConnection() {
        try {
            return DBusConnection.getConnection(DBusConnection.DBusBusType.SYSTEM);
        } catch (DBusException ex) {
            logger.error("Error creating connection", ex);
            throw new RuntimeException(ex);
        }
    }

    private static Map.Entry<ObjectPath, Map<String, Variant<?>>>  toMapEntry(Object[] o) {
        return Map.entry((ObjectPath)o[0], (Map<String, Variant<?>>)o[1]);
    }

    private static boolean isOnline(Map.Entry<ObjectPath, Map<String, Variant<?>>> e) {
        return (boolean) e.getValue().getOrDefault("Online", VARIANT_FALSE).getValue();
    }

    private Stream<Object> getModems() throws DBusException {
    return Arrays.stream(conn.callMethodAsync(remoteObject, GET_MODEMS)
            .getCall()
            .getReply()
            .getParameters());
    }

    private VoiceCallManager voiceCallManager(String string) {
        return new VoiceCallManager(conn, string);
    }
}
