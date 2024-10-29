package com.javadruid.bluez.phone.ui;

import com.javadruid.bluez.phone.lib.VoiceCall;
import com.javadruid.bluez.phone.lib.VoiceCallManager;
import com.javadruid.bluez.phone.lib.VoiceCalls;
import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyEvent;

public class PrimaryController implements Closeable {

    private final VoiceCalls voiceCalls = new VoiceCalls();

    @FXML
    TextArea phonenumber;
    @FXML
    ChoiceBox<VoiceCallManager> phones;
    @FXML
    Button answer;
    @FXML
    Button hang;
    @FXML
    Label info;

    public void initialize() {
        final ObservableList<VoiceCallManager> items = phones.getItems();
        voiceCalls.getVoiceCallManagers()
            .peek(vcm -> {
                vcm.onCallAdded(this::onCallAdded);
                vcm.onCallRemoved(this::onCallRemoved);
            })
            .forEach(items::add);
        if(!items.isEmpty()) phones.getSelectionModel().selectFirst();
    }

    @FXML
    void btnClick(ActionEvent actionEvent) {
        enterDigit(((Button) actionEvent.getSource()).getText());
    }

    @FXML
    void onBackspace(ActionEvent actionEvent) {
        enterBackspace();
    }

    @FXML
    void onCall(ActionEvent actionEvent) {
        phones.getSelectionModel().getSelectedItem().dial(phonenumber.getText());
    }

    @FXML
    void onAnswer(ActionEvent actionEvent) {
        final VoiceCall call = (VoiceCall)((Node)actionEvent.getSource()).getUserData();
        call.answer();
    }

    @FXML
    void onHang(ActionEvent actionEvent) {
        ((VoiceCall)((Node)actionEvent.getSource()).getUserData()).hangup();
    }

    public void enterKey(KeyEvent e) {
        switch (e.getCode()) {
            case DIGIT0, DIGIT1, DIGIT2, DIGIT3, DIGIT4, DIGIT5, DIGIT6, DIGIT7, DIGIT8, DIGIT9,
                NUMPAD0, NUMPAD1, NUMPAD2, NUMPAD3, NUMPAD4, NUMPAD5, NUMPAD6, NUMPAD7, NUMPAD8, NUMPAD9 -> enterDigit(e.getText());
            case BACK_SPACE -> enterBackspace();
        }
    }

    @Override
    public void close() throws IOException {
        voiceCalls.close();
    }

    private void enterBackspace() {
        final String text = phonenumber.getText();
        if (!text.isEmpty()) {
            phonenumber.setText(text.substring(0, text.length() - 1));
        }
    }

    private void enterDigit(final String text) {
        phonenumber.setText(phonenumber.getText().concat(text));
    }

    private void onCallAdded(Map.Entry<String, Map<String, Object>> e) {
        final VoiceCall voiceCall = voiceCalls.voiceCall(e.getKey());
        Platform.runLater(() -> {
            info.setText(voiceCall.getLineIdentification());
            answer.setDisable(false);
            hang.setDisable(false);
            answer.setUserData(voiceCall);
            hang.setUserData(voiceCall);
        });
        voiceCall.onPropertyChange(p -> onVoiceCallPropertyChange(p, voiceCall));
    }

    private void onCallRemoved(String path) {
        Platform.runLater(() -> {
            answer.setUserData(null);
            hang.setUserData(null);
            answer.setDisable(true);
            hang.setDisable(true);
            info.setText("");
        });
    }

    private void onVoiceCallPropertyChange(Map.Entry<String, Object> e, VoiceCall voiceCall) {
        Platform.runLater(() -> {
            switch (e.getKey()) {
                case VoiceCall.STATE -> info.setText(e.getValue() + " voiceCall from \n" + voiceCall.getLineIdentification());
            }
        });
    }

}
