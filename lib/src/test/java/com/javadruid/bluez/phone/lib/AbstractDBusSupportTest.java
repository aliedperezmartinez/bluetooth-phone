package com.javadruid.bluez.phone.lib;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.freedesktop.dbus.DBusAsyncReply;
import org.freedesktop.dbus.DBusMap;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.interfaces.DBusSigHandler;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.interfaces.Properties.PropertiesChanged;
import org.freedesktop.dbus.messages.Message;
import org.freedesktop.dbus.messages.MethodCall;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.javadruid.bluez.phone.lib.AbstractDBusSupport.GET;
import static com.javadruid.bluez.phone.lib.AbstractDBusSupport.GET_PROPERTIES;
import static com.javadruid.bluez.phone.lib.AbstractDBusSupport.SET;
import static com.javadruid.bluez.phone.lib.Ofono.BUS_NAME;
import static com.javadruid.bluez.phone.lib.OfonoTests.INTERFACE;
import static com.javadruid.bluez.phone.lib.OfonoTests.KEY;
import static com.javadruid.bluez.phone.lib.OfonoTests.PATH;
import static java.util.stream.Collectors.toMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AbstractDBusSupportTest {

    @Mock
    private DBusConnection connection;
    @Mock
    private DBusInterface remoteObject;
    @Mock
    private Properties properties;
    @Mock
    private DBusAsyncReply reply;
    @Mock
    private MethodCall call;
    @Mock
    private Message message;

    @Test
    public void testConstructorException() throws DBusException {
        when(connection.getRemoteObject(BUS_NAME, PATH, DBusInterface.class))
            .thenThrow(DBusException.class);
        assertThrows(RuntimeException.class, () -> new TestDBusSupport(connection));
    }

    @Test
    public void testGetProperties() throws DBusException {
        when(connection.callMethodAsync(remoteObject, GET_PROPERTIES))
            .thenReturn(reply);
        when(reply.getCall()).thenReturn(call);
        when(call.getReply()).thenReturn(message);
        when(message.getParameters())
            .thenReturn(new Object[]{
                new DBusMap<>(
                    new Object[][]{{KEY, new Variant<>(new Object(), "s")}}
                )
            });

        final AbstractDBusSupport instance = newTestInstance();

        final Map<String, Object> result = instance.getProperties()
            .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
        assertTrue(result.containsKey(KEY));
    }

    @Test
    public void testGetSingleProperty() throws DBusException {
        when(connection.callMethodAsync(remoteObject, GET_PROPERTIES))
            .thenReturn(reply);
        when(reply.getCall()).thenReturn(call);
        when(call.getReply()).thenReturn(message);
        final String value = "value";
        when(message.getParameters())
            .thenReturn(new Object[]{
                new DBusMap<>(
                    new Object[][]{{KEY, new Variant<>(value)}}
                )
            });

        final AbstractDBusSupport instance = newTestInstance();

        final String result = instance.getSingleProperty(KEY, String.class).get();
        assertEquals(value, result);
    }

    @Test
    public void testOnPropertyChange() throws DBusException {
        final AbstractDBusSupport instance = newTestInstance();
        final Consumer<Map.Entry<String, Object>> handler = mock(Consumer.class);
        final String value = "value";
        final String propertyName = "propertyChanged";
        doAnswer(i -> {
            final DBusSigHandler<PropertiesChanged> signalhandler = i.getArgument(2);
            signalhandler.handle(new PropertiesChanged(
                PATH, INTERFACE, Map.of(propertyName, new Variant<>(value)), List.of("propertyRemoved")));
            return null;
        })
        .when(connection)
            .addSigHandler(same(PropertiesChanged.class), eq(properties), any(DBusSigHandler.class));
        instance.onPropertyChange(handler);

        verify(connection).addSigHandler(same(PropertiesChanged.class), eq(properties), any(DBusSigHandler.class));
        verify(connection, never()).removeSigHandler(same(PropertiesChanged.class), eq(properties), any(DBusSigHandler.class));
        verify(handler).accept(Map.entry(propertyName, value));
    }

    @Test
    public void testOnPropertyChangeException() throws DBusException {
        final AbstractDBusSupport instance = newTestInstance();
        doThrow(DBusException.class)
            .when(connection).addSigHandler(same(PropertiesChanged.class), eq(properties), any(DBusSigHandler.class));

        assertThrows(RuntimeException.class, () -> instance.onPropertyChange(System.out::println));
    }

    @Test
    public void testRemovePropertyChange() throws DBusException {
        final InOrder inOrder = inOrder(connection);
        final AbstractDBusSupport instance = newTestInstance();

        instance.removePropertyChange();
        instance.onPropertyChange(System.out::println);
        instance.removePropertyChange();

        inOrder.verify(connection).addSigHandler(same(PropertiesChanged.class), eq(properties), any(DBusSigHandler.class));
        inOrder.verify(connection).removeSigHandler(same(PropertiesChanged.class), eq(properties), any(DBusSigHandler.class));
    }

    @Test
    public void testRemovePropertyChangeException() throws DBusException {
        final AbstractDBusSupport instance = newTestInstance();
        doThrow(DBusException.class)
            .when(connection).removeSigHandler(same(PropertiesChanged.class), eq(properties), any(DBusSigHandler.class));

        instance.onPropertyChange(System.out::println);
        assertThrows(RuntimeException.class, () -> instance.removePropertyChange());
    }

    @Test
    public void testRemovePropertyChangeExistingListener() throws Exception {
        InOrder inOrder = inOrder(connection);
        final AbstractDBusSupport instance = newTestInstance();

        instance.onPropertyChange(System.out::println);
        instance.onPropertyChange(System.out::println);

        inOrder.verify(connection).removeSigHandler(same(PropertiesChanged.class), eq(properties), any(DBusSigHandler.class));
        inOrder.verify(connection).addSigHandler(same(PropertiesChanged.class), eq(properties), any(DBusSigHandler.class));
    }

    @Test
    public void testToString() throws DBusException {
        final AbstractDBusSupport instance = newTestInstance();

        final String result = instance.toString();
        assertEquals(PATH, result);
    }

    @Test
    public void testGetPath() throws DBusException {
        final AbstractDBusSupport instance = newTestInstance();

        final String result = instance.getPath();
        assertEquals(PATH, result);
    }

    @Test
    public void testCallObjectMethod() throws DBusException {
        final String methodName = "methodName";
        mockObjectCall(methodName, null, null, remoteObject);
        final AbstractDBusSupport instance = newTestInstance();

        instance.callObjectMethod(methodName);

        verify(connection).callMethodAsync(remoteObject, methodName);
    }

    @Test
    public void testGetProperty() throws DBusException {
        final String propertyName = "propertyName";
        final String value = "result";
        final String interfaceName = "interface";
        mockObjectCall(GET, value, null, properties, interfaceName, propertyName);
        final AbstractDBusSupport instance = newTestInstance();

        final Object result = instance.getProperty(propertyName, INTERFACE);

        verify(connection).callMethodAsync(properties, GET, interfaceName, propertyName);
        assertEquals(value, result);
    }

    @Test
    public void testSetProperty() throws DBusException {
        final String propertyName = "propertyName";
        final String value = "result";
        final String interfaceName = "interface";
        mockObjectCall(SET, null, null, properties, interfaceName, propertyName, value);
        final AbstractDBusSupport instance = newTestInstance();

        instance.setProperty(propertyName, value, INTERFACE);

        verify(connection).callMethodAsync(properties, SET, interfaceName, propertyName, value);
    }

    @Test
    public void testCallMethodReturn() throws DBusException {
        final String methodName = "methodName";
        final String value = "result";
        final String parameter = "parameter";
        mockObjectCall(methodName, value, null, remoteObject, parameter);
        final AbstractDBusSupport instance = newTestInstance();

        final Object result = instance.callMethodReturn(remoteObject, methodName, parameter);

        verify(connection).callMethodAsync(remoteObject, methodName, parameter);
        assertEquals(value, result);
    }

    @Test
    public void testCallMethodReturnError() throws DBusException {
        final String methodName = "methodName";
        final String parameter = "parameter";
        mockObjectCall(remoteObject, methodName, parameter);
        when(call.getReply()).thenReturn(new org.freedesktop.dbus.errors.Error());
        final AbstractDBusSupport instance = newTestInstance();

        final Object result = instance.callMethodReturn(remoteObject, methodName, parameter);

        verify(connection).callMethodAsync(remoteObject, methodName, parameter);
        assertNull(result);
    }

    @Test
    public void testCallMethodReturnException() throws DBusException {
        final String methodName = "methodName";
        final String parameter = "parameter";
        mockObjectCall(remoteObject, methodName, parameter);
        when(call.getReply()).thenReturn(message);
        when(message.getParameters()).thenThrow(DBusException.class);
        final AbstractDBusSupport instance = newTestInstance();

        final Object result = instance.callMethodReturn(remoteObject, methodName, parameter);

        verify(connection).callMethodAsync(remoteObject, methodName, parameter);
        assertNull(result);
    }

    @Test
    public void testCallMethodReturnReplyTimeout() throws DBusException {
        final String methodName = "methodName";
        final String parameter = "parameter";
        mockObjectCall(remoteObject, methodName, parameter);
        final AbstractDBusSupport instance = newTestInstance();

        final Object result = instance.callMethodReturn(remoteObject, methodName, parameter);

        verify(connection).callMethodAsync(remoteObject, methodName, parameter);
        assertNull(result);
    }

    @Test
    public void testGetValue() {
        final Object value = new Object();
        final Object result = AbstractDBusSupport.getValue(new Variant<>(value, "s"));

        assertEquals(value, result);
    }

    @Test
    public void testGetValueNull() {
        assertNull(AbstractDBusSupport.getValue(null));
    }

    @Test
    public void testGetInt() {
        final int result = AbstractDBusSupport.getInt(new Variant<>(new UInt32(42L)));

        assertEquals(42, result);
    }

    @Test
    public void testGetIntNull() {
        assertEquals(0, AbstractDBusSupport.getInt(null));
    }

    @Test
    public void testGetLong() {
        final long result = AbstractDBusSupport.getLong(new Variant<>(new UInt32(42L)));

        assertEquals(42, result);
    }

    @Test
    public void testGetLongNull() {
        assertEquals(0, AbstractDBusSupport.getLong(null));
    }

    private TestDBusSupport newTestInstance() throws DBusException {
        when(connection.getRemoteObject(BUS_NAME, PATH, DBusInterface.class))
            .thenReturn(remoteObject);
        when(connection.getRemoteObject(BUS_NAME, PATH, Properties.class))
            .thenReturn(properties);
        return new TestDBusSupport(connection);
    }

    private void mockObjectCall(final String methodName, Object result, Type type,
            DBusInterface remoteObject, Object... parameters) throws DBusException {
        mockObjectCall(remoteObject, methodName, parameters);
        when(call.getReply()).thenReturn(message);
        if (result != null) {
            final Variant<?> variant = type != null? new Variant<>(result, type): new Variant<>(result);
            when(message.getParameters()).thenReturn(new Object[]{variant});
        }
    }

    private void mockObjectCall(DBusInterface remoteObject, final String methodName, Object... parameters) {
        when(connection.callMethodAsync(remoteObject, methodName, parameters)).thenReturn(reply);
        when(reply.getCall()).thenReturn(call);
    }

    private static class TestDBusSupport extends AbstractDBusSupport {

        public TestDBusSupport(DBusConnection connection) {
            super(connection, PATH, BUS_NAME, DBusInterface.class);
        }
    }

}
