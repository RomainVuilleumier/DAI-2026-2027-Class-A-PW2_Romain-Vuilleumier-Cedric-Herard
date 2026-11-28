package Dactilo.Client;

import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;

import java.util.Objects;

/**
 * Gère les entrées clavier pendant la partie et met à jour la saisie ainsi que le calculateur.
 */
public class KeyHandler {

    /**
     * Callback invoqué lorsque la saisie ou l'état de frappe est mis à jour.
     */
    @FunctionalInterface
    public interface KeyActionListener {
        /**
         * Notifie qu'un caractère a été ajouté ou supprimé.
         */
        void onInputUpdated();
    }

    /**
     * Callback invoqué lorsque l'utilisateur demande à quitter.
     */
    @FunctionalInterface
    public interface ExitListener {
        /**
         * Déclenche la procédure de sortie.
         */
        void onExit();
    }

    private static final long MIN_KEYSTROKE_INTERVAL_NANOS = 4_000_000L;

    private final TypeCalculator calculator;
    private final KeyActionListener inputListener;
    private final ExitListener exitListener;
    private final StringBuilder typedInput = new StringBuilder();

    private long lastCharNanos = 0L;

    /**
     * Crée un gestionnaire de touches lié au calculateur et aux écouteurs d'action.
     *
     * @param calculator    moteur de suivi et de calcul de frappe (non nul)
     * @param inputListener écouteur appelé après chaque modification de saisie (non nul)
     * @param exitListener  écouteur appelé en cas de demande de sortie (non nul)
     */
    public KeyHandler(TypeCalculator calculator, KeyActionListener inputListener, ExitListener exitListener) {
        this.calculator = Objects.requireNonNull(calculator);
        this.inputListener = Objects.requireNonNull(inputListener);
        this.exitListener = Objects.requireNonNull(exitListener);
    }

    /**
     * Traite une frappe clavier reçue depuis le terminal.
     *
     * @param key        touche interceptée (peut être {@code null})
     * @param gameActive {@code true} si la phase de jeu est active, {@code false} sinon
     * @return {@code true} si la sortie a été demandée, {@code false} sinon
     */
    public boolean handleKey(KeyStroke key, boolean gameActive) {
        if (key == null) {
            return false;
        }
        if (key.getKeyType() == KeyType.EOF || key.getKeyType() == KeyType.Escape) {
            exitListener.onExit();
            return true;
        }
        if (!gameActive || calculator.isFinished()) {
            return false;
        }
        if (key.getKeyType() == KeyType.Backspace) {
            handleBackspace();
            return false;
        }
        if (key.getKeyType() == KeyType.Character && key.getCharacter() != null) {
            handleCharacter(key.getCharacter());
        }
        return false;
    }

    /**
     * Supprime le dernier caractère saisi si possible et met à jour le calculateur.
     */
    private void handleBackspace() {
        if (calculator.getCurrentIndex() > 0 && !typedInput.isEmpty()) {
            typedInput.deleteCharAt(typedInput.length() - 1);
        }
        calculator.onBackspace();
        inputListener.onInputUpdated();
    }

    /**
     * Valide et enregistre un caractère saisi, en filtrant les rafales trop rapides.
     *
     * @param c caractère tapé par l'utilisateur
     */
    private void handleCharacter(char c) {
        long now = System.nanoTime();
        if (lastCharNanos > 0L && (now - lastCharNanos) < MIN_KEYSTROKE_INTERVAL_NANOS) {
            return;
        }
        lastCharNanos = now;

        if (!Character.isISOControl(c) && Character.isDefined(c)) {
            calculator.onCharTyped(c);
            typedInput.append(c);
            inputListener.onInputUpdated();
        }
    }

    /**
     * Retourne l'ensemble des caractères actuellement saisis par l'utilisateur.
     *
     * @return texte saisi
     */
    public String getTypedText() {
        return typedInput.toString();
    }
}

