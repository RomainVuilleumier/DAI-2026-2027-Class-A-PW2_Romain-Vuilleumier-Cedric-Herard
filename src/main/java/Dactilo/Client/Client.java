package Dactilo.Client;

import Dactilo.Client.ui.TerminalUi;

import java.io.IOException;

class Client {

    public void start(String roomName, String username) {
        String TargetText = "Pack my box with five dozen liquor jugs! 0123456789 @#$%^&*()-_=+[]{}|;:'\",.<>/?~` \\ Quick brown fox jumps over the lazy dog.";
        TerminalUi ui = new TerminalUi(roomName, username, new TypeCalculator(TargetText), new TerminalUi.UiCallbacks() {
            @Override
            public void onLocalStatsUpdated(double progressPercent, double wpm, double accuracyPercent, boolean finished) {

            }

            @Override
            public void onExitRequested() {

            }
        });

        try {
            ui.start();
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors du démarrage de l'interface Lanterna", e);
        }
    }
}