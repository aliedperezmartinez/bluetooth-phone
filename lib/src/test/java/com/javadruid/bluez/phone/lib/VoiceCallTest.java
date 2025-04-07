package com.javadruid.bluez.phone.lib;

import com.javadruid.bluez.phone.lib.VoiceCall.VoiceCallState;
import com.javadruid.bluez.phone.lib.interfaces.VoiceCall.DisconnectReason;
import java.time.Instant;
import java.util.Map;
import java.util.function.Consumer;
import org.freedesktop.dbus.DBusAsyncReply;
import org.freedesktop.dbus.DBusMap;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusSigHandler;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.messages.Message;
import org.freedesktop.dbus.messages.MethodCall;
import org.freedesktop.dbus.types.Variant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.javadruid.bluez.phone.lib.AbstractDBusSupport.GET_PROPERTIES;
import static com.javadruid.bluez.phone.lib.Ofono.BUS_NAME;
import static com.javadruid.bluez.phone.lib.OfonoTests.PATH;
import static com.javadruid.bluez.phone.lib.VoiceCall.START_TIME;
import static com.javadruid.bluez.phone.lib.VoiceCall.STATE;
import static com.javadruid.bluez.phone.lib.VoiceCall.VoiceCallState.ACTIVE;
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
public class VoiceCallTest {

    @Mock
    private DBusConnection connection;
    @Mock
    private com.javadruid.bluez.phone.lib.interfaces.VoiceCall remoteObject;
    @Mock
    private Properties properties;
    @Mock
    private DBusAsyncReply reply;
    @Mock
    private MethodCall call;
    @Mock
    private Message message;

    @Test
    public void testGetState() throws DBusException {
        when(connection.callMethodAsync(remoteObject, GET_PROPERTIES))
            .thenReturn(reply);
        when(reply.getCall()).thenReturn(call);
        when(call.getReply()).thenReturn(message);
        when(message.getParameters())
            .thenReturn(new Object[]{
                new DBusMap<>(
                    new Object[][]{
                        {STATE, new Variant<>(ACTIVE.getState())}
                    }
                )
            });
        final VoiceCall instance = newTestInstance();

        final VoiceCallState result = instance.getState();

        assertEquals(ACTIVE, result);
    }

    @Test
    public void testGetStartTime() throws DBusException {
        when(connection.callMethodAsync(remoteObject, GET_PROPERTIES))
            .thenReturn(reply);
        final Instant now = Instant.now();
        when(reply.getCall()).thenReturn(call);
        when(call.getReply()).thenReturn(message);
        when(message.getParameters())
            .thenReturn(new Object[]{
                new DBusMap<>(
                    new Object[][]{
                        {START_TIME, new Variant<>(now.toString())}
                    }
                )
            });
        final VoiceCall instance = newTestInstance();

        final Instant result = instance.getStartTime().get();

        assertEquals(now, result);
    }

    @Test
    public void testOnDisconnectReason() throws DBusException {
        final VoiceCall instance = newTestInstance();
        final Consumer<Map.Entry<String, String>> handler = mock(Consumer.class);
        final String reason = "reason";
        doAnswer(i -> {
            final DBusSigHandler<DisconnectReason> signalhandler = i.getArgument(2);
            signalhandler.handle(new com.javadruid.bluez.phone.lib.interfaces.VoiceCall.DisconnectReason(PATH, reason));
            return null;
        })
        .when(connection)
            .addSigHandler(same(DisconnectReason.class), eq(remoteObject), any(DBusSigHandler.class));
        instance.onDisconnectReason(handler);

        verify(connection).addSigHandler(same(DisconnectReason.class), eq(remoteObject), any(DBusSigHandler.class));
        verify(connection, never()).removeSigHandler(same(DisconnectReason.class), eq(remoteObject), any(DBusSigHandler.class));
        verify(handler).accept(Map.entry(PATH, reason));
    }

    @Test
    public void testRemoveCallAdded() throws DBusException {
        final InOrder inOrder = inOrder(connection);
        final VoiceCall instance = newTestInstance();

        instance.removeDisconnectReason();
        instance.onDisconnectReason(System.out::println);
        instance.removeDisconnectReason();

        inOrder.verify(connection).addSigHandler(same(DisconnectReason.class), eq(remoteObject), any(DBusSigHandler.class));
        inOrder.verify(connection).removeSigHandler(same(DisconnectReason.class), eq(remoteObject), any(DBusSigHandler.class));
    }

    private VoiceCall newTestInstance() throws DBusException {
        when(connection.getRemoteObject(BUS_NAME, PATH, com.javadruid.bluez.phone.lib.interfaces.VoiceCall.class))
            .thenReturn(remoteObject);
        when(connection.getRemoteObject(BUS_NAME, PATH, Properties.class))
            .thenReturn(properties);
        return new VoiceCall(connection, PATH);
    }

}
