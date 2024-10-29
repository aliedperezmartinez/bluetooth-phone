package com.javadruid.bluez.phone.lib;

import com.javadruid.bluez.phone.lib.interfaces.VoiceCall.DisconnectReason;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.interfaces.DBusSigHandler;


public class VoiceCall extends AbstractDBusSupport {

    // Method names
    static final String ANSWER = "Answer";
    static final String DIAL_MEMORY = "DialMemory";
    static final String HANGUP = "Hangup";
    // Property names
    public static final String LINE_IDENTIFICATION = "LineIdentification";
    public static final String INCOMING_LINE = "IncomingLine";
    public static final String NAME = "Name";
    public static final String MULTIPARTY = "Multiparty";
    public static final String STATE = "State";
    public static final String START_TIME = "StartTime";
    public static final String INFORMATION = "Information";
    public static final String ICON = "Icon";
    public static final String EMERGENCY = "Emergency";
    public static final String REMOTE_HELD = "RemoteHeld";
    public static final String REMOTE_MULTIPARTY = "RemoteMultiparty";

    private DBusSigHandler<DisconnectReason> disconnectReasonHandler;

    public VoiceCall(DBusConnection connection, String objectPath) {
        super(connection, objectPath, Ofono.BUS_NAME,
            com.javadruid.bluez.phone.lib.interfaces.VoiceCall.class);
    }

    public void answer() {
        callObjectMethod(ANSWER);
    }

    public void deflect(String number) {
        callObjectMethod(DIAL_MEMORY, number);
    }

    public void hangup() {
        callObjectMethod(HANGUP);
    }

    // Properties
    public String getLineIdentification() {
        return getSingleProperty(LINE_IDENTIFICATION, String.class).get();
    }

    public Optional<String> getIncomingLine() {
        return getSingleProperty(INCOMING_LINE, String.class);
    }

    public String getName() {
        return getSingleProperty(NAME, String.class).get();
    }

    public boolean isMultiparty() {
        return getSingleProperty(MULTIPARTY, Boolean.class).get();
    }

    public VoiceCallState getState() {
        return getSingleProperty(STATE, String.class).
            map(VoiceCallState::get)
            .get();
    }

    public Optional<Instant> getStartTime() {
        return getSingleProperty(START_TIME, String.class)
            .map(Instant::parse);
    }

    public Optional<String> getInformation() {
        return getSingleProperty(INFORMATION, String.class);
    }

    public Optional<Byte> getIcon() {
        return getSingleProperty(ICON, Byte.class);
    }

    public boolean isEmergency() {
        return getSingleProperty(EMERGENCY, Boolean.class).get();
    }

    public boolean isRemoteHeld() {
        return getSingleProperty(REMOTE_HELD, Boolean.class).get();
    }

    public boolean isRemoteMultiparty() {
        return getSingleProperty(REMOTE_MULTIPARTY, Boolean.class).get();
    }

    public void onDisconnectReason(Consumer<Map.Entry<String, String>> handler){
        disconnectReasonHandler = onSignal(DisconnectReason.class, remoteObject, disconnectReasonHandler, dr -> {
            handler.accept(Map.entry(dr.getPath(), dr.getReason()));
        });
    }

    public void removeDisconnectReason() {
        removeSigHandler(DisconnectReason.class, remoteObject, disconnectReasonHandler);
    }

    /**
     * Contains the state of the current call.
     * @see https://github.com/rilmodem/ofono/blob/master/doc/voicecall-api.txt
     */
    public enum VoiceCallState {
        /**
         * The call is active
         */
        ACTIVE("active"),
        /**
         * The call is on hold
         */
        HELD("held"),
        /**
         * The call is being dialed
         */
        DIALING("dialing"),
        /**
         * The remote party is being alerted
         */
        ALERTING("alerting"),
        /**
         * Incoming call in progress
         */
        INCOMING("incoming"),
        /**
         * Call is waiting
         */
        WAITING("waiting"),
        /**
         * No further use of this object is allowed, it will be destroyed shortly
         */
        DISCONNECTED("disconnected");

        private final String state;

        private static final Map<String, VoiceCallState> TEXT_TO_STATE = Map.of(
            "active", ACTIVE,
            "held", HELD,
            "dialing", DIALING,
            "alerting", ALERTING,
            "incoming", INCOMING,
            "waiting", WAITING,
            "disconnected", DISCONNECTED
        );

        private VoiceCallState(String state) {
            this.state = state;
        }

        public String getState() {
            return state;
        }

        public static VoiceCallState get(String text) {
            return TEXT_TO_STATE.get(text);
        }
    }

}
