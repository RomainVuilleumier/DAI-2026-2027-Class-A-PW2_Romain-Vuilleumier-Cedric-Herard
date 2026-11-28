package Dactilo.Client;

import Dactilo.Client.ui.TerminalUi;

/**
 * Gère la logique d'une manche de frappe côté client et assure la coordination entre
 * l'interface terminale, le calculateur de statistiques, la gestion clavier et la couche réseau.
 */
public class GameLogic {

    private final String roomName;
    private final String username;
    private final String targetText;
    private final TypeCalculator calculator;
    private final TerminalUi ui;
    private final KeyHandler keyHandler;

    /**
     * Crée une instance de {@code GameLogic} pour une manche donnée et initialise l'interface
     * ainsi que le gestionnaire de touches associés.
     *
     * @param roomName      nom de la salle ou de la session affichée dans l'interface
     * @param username      pseudo du joueur local
     * @param targetText    texte cible à saisir pendant la course
     * @param onExitNetwork action exécutée lors d'une demande de fermeture / sortie réseau (peut être {@code null})
     */
    public GameLogic(String roomName, String username, String targetText, Runnable onExitNetwork) {
        this.roomName = roomName;
        this.username = username;
        this.targetText = targetText;
        this.calculator = new TypeCalculator(targetText);
        this.ui = new TerminalUi(roomName, username, calculator);

        this.keyHandler = new KeyHandler(
                calculator,
                this::handleInputUpdated,
                () -> {
                    ui.close();
                    if (onExitNetwork != null) {
                        onExitNetwork.run();
                    }
                }
        );
        this.ui.setKeyHandler(keyHandler);
    }

    /**
     * Bascule l'interface en phase d'attente ({@link TerminalUi.GamePhase#Lobby}).
     */
    public void showLobby() {
        ui.setPhase(TerminalUi.GamePhase.Lobby);
    }

    /**
     * Démarre la phase de course en passant l'interface en {@link TerminalUi.GamePhase#Game}.
     */
    public void startGame() {
        ui.setPhase(TerminalUi.GamePhase.Game);
    }

    /**
     * Enregistre le résultat final du joueur avec sa position et bascule l'interface
     * en phase de fin ({@link TerminalUi.GamePhase#Finish}).
     *
     * @param position classement final du joueur retourné par le serveur (1 = premier)
     */
    public void finishGame(int position) {
        ui.setLocalResult(new TerminalUi.LocalResult(
                position,
                calculator.getWpm(),
                calculator.getAccuracyPercent(),
                calculator.getElapsedSeconds()
        ));
        ui.setPhase(TerminalUi.GamePhase.Finish);
    }

    /**
     * Retourne le texte actuellement saisi par le joueur via le {@link KeyHandler}.
     *
     * @return texte tapé par l'utilisateur
     */
    public String getTypedText() {
        return keyHandler.getTypedText();
    }
    /**
     * Retourne le calculateur de statistiques associé à la manche courante.
     *
     * @return moteur {@link TypeCalculator}
     */
    public TypeCalculator getCalculator() {
        return calculator;
    }
    /**
     * Retourne l'instance de l'interface terminale associée à cette logique de jeu.
     *
     * @return interface {@link TerminalUi}
     */
    public TerminalUi getUi() {
        return ui;
    }

    /**
     * Réagit à chaque mise à jour de la saisie clavier et déclenche la transition
     * vers l'écran de fin lorsque le texte cible est entièrement complété.
     */
    private void handleInputUpdated() {
        if (calculator.isFinished()) {
            finishGame(1);
        }
    }
}