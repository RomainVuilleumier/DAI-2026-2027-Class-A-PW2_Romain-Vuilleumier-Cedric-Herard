package Dactilo.Client;

import Dactilo.Client.ui.TerminalUi;

import java.io.IOException;

class Client {

    public void start(String roomName, String username) {
        String TargetText = "Pack my box with five dozen liquor jugs! 0123456789 @#$%^&*()-_=+[]{}|;:'\",.<>/?~` \\ Quick brown fox jumps over the lazy dog.";
        TypeCalculator calculator = new TypeCalculator(TargetText);
        final TerminalUi[] uiRef = new TerminalUi[1];
        uiRef[0] = new TerminalUi(roomName, username, calculator, new TerminalUi.UiCallbacks() {
            @Override
            public void onLocalStatsUpdated(double progressPercent, double wpm, double accuracyPercent, boolean finished) {
                if (finished && uiRef[0] != null) {
                    uiRef[0].setLocalResult(new TerminalUi.LocalResult(1, wpm, accuracyPercent, calculator.getElapsedSeconds()));
                    uiRef[0].setPhase(TerminalUi.GamePhase.Finish);
                }
            }

            @Override
            public void onExitRequested() {
            }
        });

        new Thread(() -> {
            try {
                Thread.sleep(5000);
                uiRef[0].setPhase(TerminalUi.GamePhase.Game);
            } catch (InterruptedException ignored) {
            }
        }).start();

        try {
            uiRef[0].start();
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors du démarrage de l'interface Lanterna", e);
        }
    }
}