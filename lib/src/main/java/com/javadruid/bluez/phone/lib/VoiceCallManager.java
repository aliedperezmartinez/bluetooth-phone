package com.javadruid.bluez.phone.lib;

import com.javadruid.bluez.phone.lib.interfaces.VoiceCallManager.CallAdded;
import com.javadruid.bluez.phone.lib.interfaces.VoiceCallManager.CallRemoved;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.freedesktop.dbus.ObjectPath;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.interfaces.DBusSigHandler;

import static java.util.stream.Collectors.toMap;

public class VoiceCallManager extends AbstractDBusSupport {
    // Methods
    static final String HANGUP_ALL = "HangupAll";
    static final String HANGUP_MULTIPARTY = "HangupMultiparty";
    static final String DIAL = "Dial";
    static final String DIAL_LAST = "DialLast";
    static final String TRANSFER = "Transfer";
    static final String HOLD_AND_ANSWER = "HoldAndAnswer";
    static final String CREATE_MULTIPARTY = "CreateMultiparty";
    static final String RELEASE_AND_SWAP = "ReleaseAndSwap";
    static final String SEND_TONES = "SendTones";
    static final String GET_CALLS = "GetCalls";
    static final String PRIVATE_CHAT = "PrivateChat";
    static final String EMERGENCY_NUMBERS = "EmergencyNumbers";
    static final String RELEASE_AND_ANSWER = "ReleaseAndAnswer";
    static final String SWAP_CALLS = "SwapCalls";
    static final String DIAL_MEMORY = "DialMemory";


    private DBusSigHandler<CallAdded> callAddedHandler;
    private DBusSigHandler<CallRemoved> callRemovedHandler;

    VoiceCallManager(DBusConnection connection, String objectPath) {
        super(connection, objectPath, Ofono.BUS_NAME, com.javadruid.bluez.phone.lib.interfaces.VoiceCallManager.class);
    }

    public String[] createMultiparty(){
        return Arrays.stream((ObjectPath[]) callMethodReturn(remoteObject, CREATE_MULTIPARTY))
            .map(ObjectPath::getPath)
            .toArray(String[]::new);
    }

    public String dial(String number){
        return dial(number, HideCallerId.DEFAULT);
    }

    public String dial(String number, HideCallerId hide_callerid){
        return ((ObjectPath)callMethodReturn(remoteObject, DIAL, number, hide_callerid.getText())).getPath();
    }

    public void dialLast(){
        callObjectMethod(DIAL_LAST);
    }

    public void dialMemory(int memory_location) {
        callObjectMethod(DIAL_MEMORY, memory_location);
    }

    public Map<String, Object> getCalls(){
        return (Map<String, Object>) callMethodReturn(remoteObject, GET_CALLS);
    }

    public void hangupAll(){
        callObjectMethod(HANGUP_ALL);
    }

    public void hangupMultiparty(){
        callObjectMethod(HANGUP_MULTIPARTY);
    }

    public void holdAndAnswer(){
        callObjectMethod(HOLD_AND_ANSWER);
    }

    public String privateChat(String[] calls){
        return (String) callMethodReturn(remoteObject, PRIVATE_CHAT, (Object[]) calls);
    }

    public void releaseAndAnswer(){
        callObjectMethod(RELEASE_AND_ANSWER);
    }

    public void releaseAndSwap(){
        callObjectMethod(RELEASE_AND_SWAP);
    }

    public void sendTones(String tones){
        callObjectMethod(SEND_TONES, tones);
    }

    public void swapCalls(){
        callObjectMethod(SWAP_CALLS);
    }

    public void transfer(){
        callObjectMethod(TRANSFER);
    }

    public String[] getEmergencyNumbers() {
        return getSingleProperty(EMERGENCY_NUMBERS, List.class)
            .map(l -> (List<String>)l)
            .map(l -> l.toArray(String[]::new))
            .orElseGet(VoiceCallManager::newEmptyArray);
    }

    public void onCallAdded(Consumer<Map.Entry<String, Map<String, Object>>> handler) {
        callAddedHandler = onSignal(CallAdded.class, remoteObject, callAddedHandler, s -> handleCallAdded(handler, s));
    }

    public void removeCallAdded() {
        removeSigHandler(CallAdded.class, remoteObject, callAddedHandler);
    }

    public void onCallRemoved(Consumer<String> handler) {
        callRemovedHandler = onSignal(CallRemoved.class, remoteObject, callRemovedHandler, s -> handleCallRemoved(handler, s));
    }

    public void removeCallRemoved() throws RuntimeException {
        removeSigHandler(CallRemoved.class, remoteObject, callRemovedHandler);
    }

    public static enum HideCallerId {
        DEFAULT("default"),
        ENABLED("enabled"),
        DISABLED("disabled");

        private final String text;

        HideCallerId(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }

    }

    private static void handleCallAdded(Consumer<Map.Entry<String, Map<String, Object>>> handler, CallAdded s) {
        handler.accept(
            Map.entry(
                s.getCallPath().getPath(),
                s.getProperties().entrySet().stream()
                    .map(e -> Map.entry(e.getKey(), getValue(e.getValue())))
                    .collect(toMap(Map.Entry::getKey, Map.Entry::getValue))));
    }

    private static void handleCallRemoved(Consumer<String> handler, CallRemoved s) {
        handler.accept(s.getCallPath().getPath());
    }

    private static String[] newEmptyArray() {
        return new String[]{};
    }

}
