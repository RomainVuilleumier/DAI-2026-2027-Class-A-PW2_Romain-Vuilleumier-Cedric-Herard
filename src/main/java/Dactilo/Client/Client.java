package Dactilo.Client;

import Dactilo.Client.ui.TerminalUi;

import java.io.IOException;

/**
 * Orchestre la logique du jeu et les transitions entre Lobby, Game et Finish.
 */
class Client {

    public void start(String roomName, String username) {
        String targetText = "Pack my box with five dozen liquor jugs! 0123456789 @#$%^&*()-_=+[]{}|;:'\",.<>/?~` \\ Quick brown fox jumps over the lazy dog.";
        TypeCalculator calculator = new TypeCalculator(targetText);
        TerminalUi ui = new TerminalUi(roomName, username, calculator);

        KeyHandler keyHandler = new KeyHandler(
                calculator,
                () -> onInputUpdated(ui, calculator),
                ui::close
        );
        ui.setKeyHandler(keyHandler);

        Thread lobbyTimer = new Thread(() -> {
            try {
                Thread.sleep(5000);
                if (ui.getPhase() == TerminalUi.GamePhase.Lobby) {
                    ui.setPhase(TerminalUi.GamePhase.Game);
                }
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        });
        lobbyTimer.setDaemon(true);
        lobbyTimer.start();

        try {
            ui.start();
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors du démarrage de l'interface Lanterna", e);
        } finally {
            lobbyTimer.interrupt();
        }
    }

    private void onInputUpdated(TerminalUi ui, TypeCalculator calculator) {
        if (calculator.isFinished()) {
            ui.setLocalResult(new TerminalUi.LocalResult(
                    1,
                    calculator.getWpm(),
                    calculator.getAccuracyPercent(),
                    calculator.getElapsedSeconds()
            ));
            ui.setPhase(TerminalUi.GamePhase.Finish);
        }
    }
}