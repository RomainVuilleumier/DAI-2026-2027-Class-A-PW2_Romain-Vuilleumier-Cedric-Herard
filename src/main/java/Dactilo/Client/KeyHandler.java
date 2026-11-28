package Dactilo.Client;

import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;

import java.util.Objects;

/**
 * Gère les entrées clavier pendant la partie et met à jour la saisie ainsi que le calculateur.
 */
public class KeyHandler {

    @FunctionalInterface
    public interface KeyActionListener {
        void onInputUpdated();
    }

    @FunctionalInterface
    public interface ExitListener {
        void onExit();
    }

    private static final long MIN_KEYSTROKE_INTERVAL_NANOS = 4_000_000L;

    private final TypeCalculator calculator;
    private final KeyActionListener inputListener;
    private final ExitListener exitListener;
    private final StringBuilder typedInput = new StringBuilder();

    private long lastCharNanos = 0L;

    public KeyHandler(TypeCalculator calculator, KeyActionListener inputListener, ExitListener exitListener) {
        this.calculator = Objects.requireNonNull(calculator);
        this.inputListener = Objects.requireNonNull(inputListener);
        this.exitListener = Objects.requireNonNull(exitListener);
    }

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

    private void handleBackspace() {
        if (calculator.getCurrentIndex() > 0 && !typedInput.isEmpty()) {
            typedInput.deleteCharAt(typedInput.length() - 1);
        }
        calculator.onBackspace();
        inputListener.onInputUpdated();
    }

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

    public String getTypedText() {
        return typedInput.toString();
    }
}