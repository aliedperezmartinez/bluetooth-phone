package com.javadruid.bluez.phone.lib;

import com.javadruid.bluez.phone.lib.interfaces.VoiceCallManager.CallAdded;
import com.javadruid.bluez.phone.lib.interfaces.VoiceCallManager.CallRemoved;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.freedesktop.dbus.DBusAsyncReply;
import org.freedesktop.dbus.DBusMap;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.ObjectPath;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.interfaces.DBusSigHandler;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.messages.Message;
import org.freedesktop.dbus.messages.MethodCall;
import org.freedesktop.dbus.types.DBusListType;
import org.freedesktop.dbus.types.Variant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.javadruid.bluez.phone.lib.AbstractDBusSupport.GET_PROPERTIES;
import static com.javadruid.bluez.phone.lib.Ofono.BUS_NAME;
import static com.javadruid.bluez.phone.lib.OfonoTests.PATH;
import static com.javadruid.bluez.phone.lib.VoiceCallManager.EMERGENCY_NUMBERS;
import static com.javadruid.bluez.phone.lib.VoiceCallManager.HideCallerId.DEFAULT;
import static com.javadruid.bluez.phone.lib.VoiceCallManager.HideCallerId.ENABLED;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class VoiceCallManagerTest {

    @Mock
    private DBusConnection connection;
    @Mock
    private com.javadruid.bluez.phone.lib.interfaces.VoiceCallManager remoteObject;
    @Mock
    private Properties properties;
    @Mock
    private DBusAsyncReply reply;
    @Mock
    private MethodCall call;
    @Mock
    private Message message;
    @Mock
    private DBusSigHandler<CallAdded> callAddedhandler;
    @Mock
    private DBusSigHandler<CallRemoved> callRemovedhandler;

    @Test
    public void testCreateMultiparty() throws DBusException {
        final VoiceCallManager instance = newTestInstance();
        mockObjectCall(VoiceCallManager.CREATE_MULTIPARTY, new ObjectPath[]{new ObjectPath("SYSTEM", PATH)}, null, remoteObject);
        final String[] result = instance.createMultiparty();

        assertArrayEquals(new String[]{PATH}, result);
    }

    @Test
    public void testDialDefault() throws DBusException {
        final VoiceCallManager instance = newTestInstance();
        final String phoneNumber = "1234567890";
        mockObjectCall(VoiceCallManager.DIAL, new ObjectPath("SYSTEM", PATH), null, remoteObject,
            phoneNumber, DEFAULT.getText());
        final String result = instance.dial(phoneNumber);

        assertEquals(PATH, result);
    }

    @Test
    public void testDial() throws DBusException {
        final VoiceCallManager instance = newTestInstance();
        final String phoneNumber = "1234567890";
        mockObjectCall(VoiceCallManager.DIAL, new ObjectPath("SYSTEM", PATH), null, remoteObject,
            phoneNumber, ENABLED.getText());
        final String result = instance.dial(phoneNumber, ENABLED);

        assertEquals(PATH, result);
    }

    @Test
    public void testGetEmergencyNumbers() throws DBusException {
        when(connection.callMethodAsync(remoteObject, GET_PROPERTIES))
            .thenReturn(reply);
        when(reply.getCall()).thenReturn(call);
        when(call.getReply()).thenReturn(message);
        when(message.getParameters())
            .thenReturn(new Object[]{
                new DBusMap<>(
                    new Object[][]{
                        {EMERGENCY_NUMBERS, new Variant<>(List.of("999", "911"), new DBusListType(String.class))}
                    }
                )
            });
        final VoiceCallManager instance = newTestInstance();

        final String[] result = instance.getEmergencyNumbers();

        assertEquals(2, result.length);
        assertEquals("999", result[0]);
        assertEquals("911", result[1]);
    }

    @Test
    public void testGetEmergencyNumbersMissing() throws DBusException {
        when(connection.callMethodAsync(remoteObject, GET_PROPERTIES))
            .thenReturn(reply);
        when(reply.getCall()).thenReturn(call);
        when(call.getReply()).thenReturn(message);
        when(message.getParameters())
            .thenReturn(new Object[]{
                new DBusMap<>(
                    new Object[][]{}
                )
            });
        final VoiceCallManager instance = newTestInstance();

        final String[] result = instance.getEmergencyNumbers();

        assertEquals(0, result.length);
    }

    @Test
    public void testOnCallAdded() throws DBusException {
        final VoiceCallManager instance = newTestInstance();
        final Consumer<Map.Entry<String, Map<String, Object>>> handler = mock(Consumer.class);
        final String value = "value";
        final String propertyName = "propertyChanged";
        doAnswer(i -> {
            final DBusSigHandler<CallAdded> signalhandler = i.getArgument(2);
            signalhandler.handle(new CallAdded(PATH, new DBusPath(PATH), Map.of(propertyName, new Variant<>(value))));
            return null;
        })
        .when(connection)
            .addSigHandler(same(CallAdded.class), eq(remoteObject), any(DBusSigHandler.class));
        instance.onCallAdded(handler);

        verify(connection).addSigHandler(same(CallAdded.class), eq(remoteObject), any(DBusSigHandler.class));
        verify(connection, never()).removeSigHandler(same(CallAdded.class), eq(remoteObject), any(DBusSigHandler.class));
        verify(handler).accept(Map.entry(PATH, Map.of(propertyName, value)));
    }

    @Test
    public void testRemoveCallAdded() throws DBusException {
        final InOrder inOrder = inOrder(connection);
        final VoiceCallManager instance = newTestInstance();

        instance.removeCallAdded();
        instance.onCallAdded(System.out::println);
        instance.removeCallAdded();

        inOrder.verify(connection).addSigHandler(same(CallAdded.class), eq(remoteObject), any(DBusSigHandler.class));
        inOrder.verify(connection).removeSigHandler(same(CallAdded.class), eq(remoteObject), any(DBusSigHandler.class));
    }

    @Test
    public void testOnCallRemoved() throws DBusException {
        final VoiceCallManager instance = newTestInstance();
        final Consumer<String> handler = mock(Consumer.class);
        final String value = "value";
        final String propertyName = "propertyChanged";
        doAnswer(i -> {
            final DBusSigHandler<CallRemoved> signalhandler = i.getArgument(2);
            signalhandler.handle(new CallRemoved(PATH, new DBusPath(PATH)));
            return null;
        })
        .when(connection)
            .addSigHandler(same(CallRemoved.class), eq(remoteObject), any(DBusSigHandler.class));
        instance.onCallRemoved(handler);

        verify(connection).addSigHandler(same(CallRemoved.class), eq(remoteObject), any(DBusSigHandler.class));
        verify(connection, never()).removeSigHandler(same(CallRemoved.class), eq(remoteObject), any(DBusSigHandler.class));
        verify(handler).accept(PATH);
    }

    @Test
    public void testRemoveCallRemoved() throws DBusException {
        final InOrder inOrder = inOrder(connection);
        final VoiceCallManager instance = newTestInstance();

        instance.removeCallRemoved();
        instance.onCallRemoved(System.out::println);
        instance.removeCallRemoved();

        inOrder.verify(connection).addSigHandler(same(CallRemoved.class), eq(remoteObject), any(DBusSigHandler.class));
        inOrder.verify(connection).removeSigHandler(same(CallRemoved.class), eq(remoteObject), any(DBusSigHandler.class));
    }

    private VoiceCallManager newTestInstance() throws DBusException {
        when(connection.getRemoteObject(BUS_NAME, PATH, com.javadruid.bluez.phone.lib.interfaces.VoiceCallManager.class))
            .thenReturn(remoteObject);
        when(connection.getRemoteObject(BUS_NAME, PATH, Properties.class))
            .thenReturn(properties);
        return new VoiceCallManager(connection, PATH);
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

}
