package Dactilo.Client.ui;

import Dactilo.Client.TypeCalculator;
import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;

import java.io.IOException;
import java.util.Objects;

/**
 * Interface terminale Lanterna.
 */
public class TerminalUi {

    /**
     * Callbacks déclenchés par l'interface vers la couche applicative / réseau.
     */
    public interface UiCallbacks {
        /**
         * Appelé lorsque les statistiques locales du joueur évoluent.
         *
         * @param progressPercent progression en pourcentage (0 à 100)
         * @param wpm             vitesse en mots par minute
         * @param accuracyPercent précision en pourcentage (0 à 100)
         * @param finished        vrai si le texte est entièrement terminé
         */
        void onLocalStatsUpdated(double progressPercent, double wpm, double accuracyPercent, boolean finished);

        /**
         * Appelé lorsque l'utilisateur demande la fermeture de l'interface.
         */
        void onExitRequested();
    }

    private final String roomName;
    private final String localUsername;
    private final TypeCalculator calculator;
    private final UiCallbacks callbacks;
    private final StringBuilder typedInput = new StringBuilder();

    private volatile boolean running = false;
    private Terminal terminal;
    private Screen screen;

    /**
     * Crée une instance de l'interface terminale.
     *
     * @param roomName      nom de la salle affichée dans l'en-tête
     * @param localUsername pseudo du joueur local
     * @param calculator    moteur de calcul de frappe et statistiques
     * @param callbacks     écouteur des événements locaux et de sortie
     */
    public TerminalUi(String roomName, String localUsername, TypeCalculator calculator, UiCallbacks callbacks) {
        this.roomName = Objects.requireNonNull(roomName);
        this.localUsername = Objects.requireNonNull(localUsername);
        this.calculator = Objects.requireNonNull(calculator);
        this.callbacks = Objects.requireNonNull(callbacks);
    }

    /**
     * Initialise l'écran Lanterna et lance la boucle d'affichage et de lecture clavier.
     *
     * @throws IOException en cas d'erreur d'entrée/sortie sur le terminal
     */
    public void start() throws IOException {
        DefaultTerminalFactory factory = new DefaultTerminalFactory();
        this.terminal = factory.createTerminal();
        this.screen = new TerminalScreen(terminal);
        this.screen.startScreen();
        this.screen.setCursorPosition(null);
        this.running = true;

        try {
            inputAndRenderLoop();
        } finally {
            close();
        }
    }

    /**
     * Ferme proprement l'écran et le terminal Lanterna.
     */
    public void close() {
        running = false;
        try {
            if (screen != null) {
                screen.close();
            }
        } catch (IOException ignored) {
        }
        try {
            if (terminal != null) {
                terminal.close();
            }
        } catch (IOException ignored) {
        }
    }

    /**
     * Boucle principale qui redessine l'écran et traite les événements clavier.
     *
     * @throws IOException en cas d'erreur lors du rendu ou de la lecture
     */
    private void inputAndRenderLoop() throws IOException {
        while (running) {
            render();

            KeyStroke key = screen.pollInput();
            if (key != null) {
                handleKey(key);
            } else {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    /**
     * Traite une touche pressée (quitter, retour arrière ou caractère UTF-8).
     *
     * @param key touche interceptée par Lanterna
     */
    private void handleKey(KeyStroke key) {
        if (key.getKeyType() == KeyType.EOF || key.getKeyType() == KeyType.Escape) {
            running = false;
            callbacks.onExitRequested();
            return;
        }

        if (calculator.isFinished()) {
            return;
        }

        if (key.getKeyType() == KeyType.Backspace) {
            handleBackspace();
            return;
        }

        if (key.getKeyType() == KeyType.Character && key.getCharacter() != null) {
            handleCharacter(key.getCharacter());
        }
    }

    /**
     * Traitement de la touche Backspace.
     *
     */
    private void handleBackspace() {
        if (calculator.getCurrentIndex() > 0 && !typedInput.isEmpty()) {
            typedInput.deleteCharAt(typedInput.length() - 1);
        }
        calculator.onBackspace();
        notifyLocalStats();
    }

    /**
     * Traite d'un  caractère UTF-8 tappée.
     *
     */
    private void handleCharacter(char c) {
        if (!Character.isISOControl(c) && Character.isDefined(c)) {
            calculator.onCharTyped(c);
            typedInput.append(c);
            notifyLocalStats();
        }
    }

    /**
     * Récupère les métriques courantes et notifie-les callbacks.
     */
    private void notifyLocalStats() {
        double progress = calculator.getProgressPercent();
        double wpm = calculator.getWpm();
        double accuracy = calculator.getAccuracyPercent();
        boolean finished = calculator.isFinished();
        callbacks.onLocalStatsUpdated(progress, wpm, accuracy, finished);
    }

    /**
     * Dessine l'en-tête, le texte cible, la saisie du joueur et la barre de statistiques.
     *
     * @throws IOException en cas d'erreur lors du rafraîchissement de l'écran
     */
    private void render() throws IOException {
        if (screen == null) {
            return;
        }

        TerminalSize size = screen.doResizeIfNecessary();
        if (size == null) {
            size = screen.getTerminalSize();
        }

        int width = size.getColumns();
        int height = size.getRows();

        TextGraphics g = screen.newTextGraphics();
        g.setBackgroundColor(TextColor.ANSI.BLACK);
        g.fill(' ');

        String header = String.format(" DACTILO | Salle: %s | Joueur: %s | [Echap] Quitter ",
                roomName, localUsername);
        g.setForegroundColor(TextColor.ANSI.CYAN);
        g.putString(0, 0, truncate(header, width), SGR.BOLD);

        g.setForegroundColor(TextColor.ANSI.YELLOW);
        g.putString(2, 2, "Texte à taper :");
        drawTargetText(g, 4, width - 4, 3);

        g.setForegroundColor(TextColor.ANSI.YELLOW);
        g.putString(2, 8, "Ta saisie :");
        drawTypedInput(g, 10, width - 4);

        int statsRow = Math.max(12, height - 3);
        String stats = calculator.isFinished()
                ? String.format(" Terminé ! WPM: %.1f | Précision: %.1f%% | Temps: %.1fs ",
                calculator.getWpm(), calculator.getAccuracyPercent(), calculator.getElapsedSeconds())
                : String.format(" WPM: %.1f | Précision: %.1f%% | Progression: %.1f%% | Temps: %.1fs ",
                calculator.getWpm(), calculator.getAccuracyPercent(),
                calculator.getProgressPercent(), calculator.getElapsedSeconds());

        g.setForegroundColor(calculator.isFinished() ? TextColor.ANSI.GREEN_BRIGHT : TextColor.ANSI.WHITE_BRIGHT);
        g.putString(0, statsRow, truncate(stats, width), SGR.BOLD);

        screen.refresh();
    }

    /**
     * Affiche le texte cible en colorant chaque caractère selon son état (vert, rouge ou en attente).
     *
     * @param g        contexte graphique Lanterna
     * @param startY   ligne de départ d'affichage
     * @param maxWidth largeur maximale disponible
     * @param maxRows  nombre de lignes maximal disponible
     */
    private void drawTargetText(TextGraphics g, int startY, int maxWidth, int maxRows) {
        if (maxWidth <= 0 || maxRows <= 0) {
            return;
        }

        String text = calculator.getTargetText();
        int x = 2;
        int y = startY;

        for (int i = 0; i < text.length(); i++) {
            if (x >= 2 + maxWidth) {
                x = 2;
                y++;
                if (y >= startY + maxRows) {
                    break;
                }
            }

            char ch = text.charAt(i);
            TypeCalculator.CharStatus status = calculator.getStatusAt(i);
            boolean isCursor = (i == calculator.getCurrentIndex()) && !calculator.isFinished();

            TextColor fg = switch (status) {
                case CORRECT -> TextColor.ANSI.GREEN;
                case WRONG -> TextColor.ANSI.RED;
                case PENDING -> TextColor.ANSI.WHITE;
            };

            g.setForegroundColor(fg);
            String s = String.valueOf(ch);
            if (isCursor) {
                g.putString(x, y, s, SGR.REVERSE);
            } else {
                g.putString(x, y, s);
            }
            g.setForegroundColor(fg);
            g.putString(x, y, String.valueOf(ch));
            x++;
        }
    }

    /**
     * Affiche les caractères saisis par le joueur en vert si corrects et en rouge si faux.
     *
     * @param g        contexte graphique Lanterna
     * @param y        ligne d'affichage de la saisie
     * @param maxWidth largeur maximale disponible
     */
    private void drawTypedInput(TextGraphics g, int y, int maxWidth) {
        if (maxWidth <= 0) {
            return;
        }

        String typed = typedInput.toString();
        int limit = Math.min(typed.length(), maxWidth);

        for (int i = 0; i < limit; i++) {
            char ch = typed.charAt(i);
            TypeCalculator.CharStatus status = i < calculator.getTargetText().length()
                    ? calculator.getStatusAt(i)
                    : TypeCalculator.CharStatus.WRONG;
            g.setForegroundColor(status == TypeCalculator.CharStatus.CORRECT
                    ? TextColor.ANSI.GREEN
                    : TextColor.ANSI.RED);
            g.putString(2 + i, y, String.valueOf(ch));
        }

        if (!calculator.isFinished() && limit < maxWidth) {
            g.setForegroundColor(TextColor.ANSI.WHITE);
            g.putString(2 + limit, y, " ", SGR.REVERSE);
        }
    }

    /**
     * Tronque une chaîne à la longueur maximale en ajoutant une ellipse si nécessaire.
     *
     * @param s      chaîne d'entrée
     * @param maxLen longueur maximale autorisée
     * @return chaîne tronquée
     */
    private String truncate(String s, int maxLen) {
        if (s == null || maxLen <= 0) {
            return "";
        }
        return s.length() <= maxLen ? s : s.substring(0, maxLen - 1) + "…";
    }
}