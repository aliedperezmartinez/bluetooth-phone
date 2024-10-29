package com.javadruid.bluez.phone.lib;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.errors.Error;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.interfaces.DBusSigHandler;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.interfaces.Properties.PropertiesChanged;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.messages.Message;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractDBusSupport {

    static final String GET_PROPERTIES = "GetProperties";
    static final String GET = "Get";
    static final String SET = "Set";

    private static final Logger logger = LoggerFactory.getLogger(AbstractDBusSupport.class);

    protected final String objectPath;
    protected final DBusInterface remoteObject;
    protected final DBusConnection connection;
    private final Properties properties;
    private DBusSigHandler<PropertiesChanged> propertyChangeHandler;

    public AbstractDBusSupport(DBusConnection connection, String objectPath, String busName,
            Class<? extends DBusInterface> dbusClass) {
        this.connection = connection;
        this.objectPath = objectPath;
        try {
            this.remoteObject = connection.getRemoteObject(busName, objectPath, dbusClass);
            this.properties = connection.getRemoteObject(busName, objectPath, Properties.class);
        } catch (DBusException ex) {
            logger.error("Could not get remote object", ex);
            throw new RuntimeException(ex);
        }
    }

    public Stream<Map.Entry<String, Object>> getProperties() {
        return ((Map<String, Variant<?>>) callMethodReturn(remoteObject, GET_PROPERTIES))
            .entrySet().stream()
            .map(AbstractDBusSupport::toObjectEntry);
    }

    // Listeners
    public void onPropertyChange(Consumer<Map.Entry<String, Object>> handler) {
        propertyChangeHandler = onSignal(
            PropertiesChanged.class, properties, propertyChangeHandler,
            s -> handlePropertyChange(s, handler));
    }

    public void removePropertyChange() {
        removeSigHandler(PropertiesChanged.class, properties, propertyChangeHandler);
    }

    public String getPath() {
        return objectPath;
    }

    @Override
    public String toString() {
        return defaultName();
    }

    protected void callObjectMethod(String methodName, Object... parameters) {
        callMethod(remoteObject, methodName, parameters);
    }

    protected Object getProperty(String propertyName, String interfaceName) {
        return callMethodReturn(properties, GET, interfaceName, propertyName);
    }

    protected void setProperty(String propertyName, Object value, String interfaceName) {
        callMethod(properties, SET, interfaceName, propertyName, value);
    }

    protected Object callMethodReturn(DBusInterface object, String methodName, Object... parameters) {
        try {
            final Message property = callMethod(object, methodName, parameters);
            if (property != null) {
                if (property instanceof Error error) {
                    logger.warn("Error whilst calling method", error.getException());
                    return null;
                } else {
                    final Object result = property.getParameters()[0];
                    if (result != null) {
                        if(result instanceof Variant<?> v)
                            return v.getValue();
                        return result;
                    }
                }
            }
        } catch (DBusException ex) {
            logger.warn("Could not call {} with parameters {}", methodName, parameters);
            logger.warn("Could not retrieve property", ex);
            return null;
        }
        return null;
    }

    protected <T> Optional<T> getSingleProperty(String propertyName, final Class<T> aClass) {
        return getProperties()
            .filter(e -> propertyName.equals(e.getKey()))
            .map(Map.Entry::getValue)
            .map(aClass::cast)
            .findFirst();
    }

    protected String defaultName() {
        return objectPath;
    }

    protected static int getInt(Variant<?> r) {
        final Object result = getValue(r);
        return (result == null) ? 0 : ((UInt32) result).intValue();
    }

    protected static long getLong(Variant<?> r) {
        final Object result = getValue(r);
        return (result == null) ? 0 : ((UInt32) result).longValue();
    }

    protected static Object getValue(Variant<?> r) {
        return r != null ? r.getValue() : null;
    }

    protected <T extends DBusSignal> DBusSigHandler<T> onSignal(
            Class<T> clazz, final DBusInterface object,
            final DBusSigHandler<T> signalhandler, final DBusSigHandler<T> newSignalhandler) {
        removeSigHandler(clazz, object, signalhandler);
        return addSigHandler(clazz, object, newSignalhandler);
    }

    protected <T extends DBusSignal> DBusSigHandler<T> addSigHandler(
            Class<T> clazz, DBusInterface object, DBusSigHandler<T> signalHandler) {
        try {
            connection.addSigHandler(clazz, object, signalHandler);
            return signalHandler;
        } catch (DBusException ex) {
            logger.error("Error adding new listener", ex);
            throw new RuntimeException(ex);
        }
    }

    protected <T extends DBusSignal> void removeSigHandler(
            Class<T> clazz, DBusInterface object, DBusSigHandler<T> signalhandler) {
        if (signalhandler != null) {
            try {
                connection.removeSigHandler(clazz, object, signalhandler);
            } catch (DBusException ex) {
                logger.error("Error removing listener", ex);
                throw new RuntimeException(ex);
            }
        }
    }

    private Message callMethod(DBusInterface object, String methodName, Object... parameters) {
        return connection.callMethodAsync(object, methodName, parameters)
            .getCall()
            .getReply();
    }

    private static void handlePropertyChange(PropertiesChanged s, Consumer<Map.Entry<String, Object>> handler) {
        s.getPropertiesChanged().entrySet().stream()
            .map(AbstractDBusSupport::toObjectEntry)
            .forEach(handler::accept);
        logger.info("signal received: {}", s);
    }

    private static Map.Entry<String, Object> toObjectEntry(Map.Entry<String, Variant<?>> e) {
        return Map.entry(e.getKey(), e.getValue().getValue());
    }

}
