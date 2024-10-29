package com.javadruid.bluez.phone.lib;

import com.javadruid.bluez.phone.lib.interfaces.Manager;
import java.util.Collections;
import java.util.List;
import org.freedesktop.dbus.DBusAsyncReply;
import org.freedesktop.dbus.DBusMap;
import org.freedesktop.dbus.ObjectPath;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.messages.Message;
import org.freedesktop.dbus.messages.MethodCall;
import org.freedesktop.dbus.types.Variant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.javadruid.bluez.phone.lib.VoiceCalls.GET_MODEMS;
import static com.javadruid.bluez.phone.lib.OfonoTests.PATH;
import static java.lang.Boolean.TRUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class VoiceCallsTest {

    @Mock
    private DBusConnection connection;
    @Mock
    private Manager remoteObject;
    @Mock
    private DBusAsyncReply reply;
    @Mock
    private MethodCall call;
    @Mock
    private Message message;

    @Test
    public void testConnectionThrowsException() throws DBusException {
        when(connection.getRemoteObject(Ofono.BUS_NAME, "/", Manager.class))
            .thenThrow(DBusException.class);
        assertThrows(RuntimeException.class, () -> new VoiceCalls(connection));
    }

    @Test
    public void testGetVoiceCallManagers() throws DBusException {
        when(connection.callMethodAsync(remoteObject, GET_MODEMS))
            .thenReturn(reply);
        when(reply.getCall()).thenReturn(call);
        when(call.getReply()).thenReturn(message);
        when(message.getParameters())
            .thenReturn(new Object[]{
                Collections.singletonList(new Object[]{
                    new ObjectPath("SYSTEM", PATH), new DBusMap<String, Variant<?>>(
                        new Object[][]{
                            {"Online", new Variant<>(TRUE)}
                    })
                })
            });
        final VoiceCalls instance = newInstance();

        final List<VoiceCallManager> result = instance.getVoiceCallManagers().toList();

        assertEquals(PATH, result.get(0).toString());
    }

    @Test
    public void testGetVoiceCallManagerThrowsException() throws DBusException {
        when(connection.callMethodAsync(remoteObject, GET_MODEMS))
            .thenReturn(reply);
        when(reply.getCall()).thenReturn(call);
        when(call.getReply()).thenReturn(message);
        when(message.getParameters()).thenThrow(DBusException.class);
        final VoiceCalls instance = newInstance();

        assertThrows(RuntimeException.class, () -> instance.getVoiceCallManagers());
    }

    @Test
    public void testVoiceCall() throws DBusException {
        final VoiceCalls instance = newInstance();

        final VoiceCall result = instance.voiceCall(PATH);

        assertEquals(PATH, result.toString());
    }

    @Test
    public void testClose() throws Exception {
        final VoiceCalls instance = newInstance();

        instance.close();

        verify(connection).close();
    }

    private VoiceCalls newInstance() throws DBusException {
        when(connection.getRemoteObject(Ofono.BUS_NAME, "/", Manager.class))
            .thenReturn(remoteObject);
        return new VoiceCalls(connection);
    }

}
