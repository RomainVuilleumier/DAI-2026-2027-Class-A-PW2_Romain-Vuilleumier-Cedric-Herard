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
 * Interface terminale Lanterna gérant les écrans Lobby, Partie et Résultats.
 */
public class TerminalUi {

    private static final String[] LOBBY_ASCII_ART = {
            "  ____             _   _ _          ____ _   _ ____   ____  __  ____     __",
            " |  _ \\  __ _  ___| |_(_) | ___    / ___| | | |  _ \\ |  _ \\|  \\/  \\ \\   / /",
            " | | | |/ _` |/ __| __| | |/ _ \\  | |   | |_| | | | || |_) | |\\/| |\\ \\ / / ",
            " | |_| | (_| | (__| |_| | | (_) | | |___|  _  | |_| ||  _ <| |  | | \\ V /  ",
            " |____/ \\__,_|\\___|\\__|_|_|\\___/   \\____|_| |_|____(_)_| \\_\\_|  |_|  \\_/   "
    };

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

    /**
     * Phases d'affichage de l'interface.
     */
    public enum GamePhase {
        Lobby,
        Game,
        Finish
    }

    /**
     * Résultat final et classement du joueur local envoyés par le serveur.
     *
     * @param position        position finale du joueur (1 = premier)
     * @param wpm             vitesse finale en mots par minute
     * @param accuracyPercent précision finale en pourcentage
     * @param elapsedSeconds  temps total en secondes
     */
    public record LocalResult(int position, double wpm, double accuracyPercent, double elapsedSeconds) {
    }

    private final String roomName;
    private final String localUsername;
    private final TypeCalculator calculator;
    private final UiCallbacks callbacks;
    private final StringBuilder typedInput = new StringBuilder();

    private volatile GamePhase phase = GamePhase.Lobby;
    private volatile LocalResult localResult;
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
     * Change la phase courante de l'interface (Lobby, Game ou Finish).
     *
     * @param newPhase nouvelle phase à afficher
     */
    public void setPhase(GamePhase newPhase) {
        this.phase = Objects.requireNonNull(newPhase);
    }

    /**
     * Enregistre le résultat final et la position du joueur local reçus du serveur.
     *
     * @param result statistiques finales et rang du joueur local
     */
    public void setLocalResult(LocalResult result) {
        this.localResult = Objects.requireNonNull(result);
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
        phase = GamePhase.Finish;
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
     * Boucle principale qui redessine l'écran selon la phase et traite les événements clavier.
     *
     * @throws IOException en cas d'erreur lors du rendu ou de la lecture
     */
    private void inputAndRenderLoop() throws IOException {
        while (running) {
            switch (phase) {
                case Lobby -> renderLobbyPhase();
                case Game -> renderGamePhase();
                case Finish -> renderFinishPhase();
            }

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
            phase = GamePhase.Finish;
            callbacks.onExitRequested();
            return;
        }
        if (phase != GamePhase.Game || calculator.isFinished()) {
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
     */
    private void handleBackspace() {
        if (calculator.getCurrentIndex() > 0 && !typedInput.isEmpty()) {
            typedInput.deleteCharAt(typedInput.length() - 1);
        }
        calculator.onBackspace();
        notifyLocalStats();
    }

    /**
     * Traitement d'un caractère UTF-8 tapé.
     *
     * @param c caractère saisi
     */
    private void handleCharacter(char c) {
        if (!Character.isISOControl(c) && Character.isDefined(c)) {
            calculator.onCharTyped(c);
            typedInput.append(c);
            notifyLocalStats();
        }
    }

    /**
     * Récupère les métriques courantes et notifie les callbacks.
     */
    private void notifyLocalStats() {
        double progress = calculator.getProgressPercent();
        double wpm = calculator.getWpm();
        double accuracy = calculator.getAccuracyPercent();
        boolean finished = calculator.isFinished();
        callbacks.onLocalStatsUpdated(progress, wpm, accuracy, finished);

        if (finished && localResult == null) {
            localResult = new LocalResult(1, wpm, accuracy, calculator.getElapsedSeconds());
        }
    }

    /**
     * Dessine l'écran d'attente (Lobby) avec l'ASCII art et un message d'attente.
     *
     * @throws IOException en cas d'erreur lors du rafraîchissement de l'écran
     */
    private void renderLobbyPhase() throws IOException {
        TerminalSize size = getTerminalSize();
        int width = size.getColumns();
        int height = size.getRows();

        TextGraphics graphics = screen.newTextGraphics();
        graphics.setBackgroundColor(TextColor.ANSI.BLACK);
        graphics.fill(' ');

        drawHeader(graphics, width, "LOBBY");

        graphics.setForegroundColor(TextColor.ANSI.CYAN_BRIGHT);
        int row = 2;
        for (String line : LOBBY_ASCII_ART) {
            if (row >= height - 4) {
                break;
            }
            graphics.putString(2, row++, truncate(line, width - 4), SGR.BOLD);
        }

        graphics.setForegroundColor(TextColor.ANSI.YELLOW);
        graphics.putString(2, Math.min(row + 2, height - 3),
                truncate("En attente du début de la partie...", width - 4), SGR.BOLD);

        screen.refresh();
    }

    /**
     * Dessine l'en-tête, le texte cible, la saisie du joueur et la barre de statistiques pendant la partie.
     *
     * @throws IOException en cas d'erreur lors du rafraîchissement de l'écran
     */
    private void renderGamePhase() throws IOException {
        TerminalSize size = getTerminalSize();
        int width = size.getColumns();
        int height = size.getRows();

        TextGraphics graphics = screen.newTextGraphics();
        graphics.setBackgroundColor(TextColor.ANSI.BLACK);
        graphics.fill(' ');

        drawHeader(graphics, width, "COURSE");

        graphics.setForegroundColor(TextColor.ANSI.YELLOW);
        graphics.putString(2, 2, "Texte à taper :");
        drawTargetText(graphics, 2, 4, width - 4, 3);

        graphics.setForegroundColor(TextColor.ANSI.YELLOW);
        graphics.putString(2, 8, "Ta saisie :");
        drawTypedInput(graphics, 2, 10, width - 4);

        int statsRow = Math.max(12, height - 3);
        String stats = calculator.isFinished()
                ? String.format(" Terminé ! WPM: %.1f | Précision: %.1f%% | Temps: %.1fs ",
                calculator.getWpm(), calculator.getAccuracyPercent(), calculator.getElapsedSeconds())
                : String.format(" WPM: %.1f | Précision: %.1f%% | Progression: %.1f%% | Temps: %.1fs ",
                calculator.getWpm(), calculator.getAccuracyPercent(),
                calculator.getProgressPercent(), calculator.getElapsedSeconds());

        graphics.setForegroundColor(calculator.isFinished() ? TextColor.ANSI.GREEN_BRIGHT : TextColor.ANSI.WHITE_BRIGHT);
        graphics.putString(0, statsRow, truncate(stats, width), SGR.BOLD);

        screen.refresh();
    }

    /**
     * Dessine l'écran de fin avec uniquement les statistiques et la position du joueur local.
     *
     * @throws IOException en cas d'erreur lors du rafraîchissement de l'écran
     */
    private void renderFinishPhase() throws IOException {
        TerminalSize size = getTerminalSize();
        int width = size.getColumns();
        int height = size.getRows();

        TextGraphics graphics = screen.newTextGraphics();
        graphics.setBackgroundColor(TextColor.ANSI.BLACK);
        graphics.fill(' ');

        drawHeader(graphics, width, "RÉSULTATS");

        LocalResult result = localResult != null
                ? localResult
                : new LocalResult(1, calculator.getWpm(), calculator.getAccuracyPercent(), calculator.getElapsedSeconds());

        graphics.setForegroundColor(TextColor.ANSI.GREEN_BRIGHT);
        graphics.putString(2, 2, "Partie terminée — Tes statistiques :", SGR.BOLD);

        graphics.setForegroundColor(TextColor.ANSI.CYAN_BRIGHT);
        graphics.putString(2, 4, truncate("Position : #" + result.position(), width - 4), SGR.BOLD);

        graphics.setForegroundColor(TextColor.ANSI.WHITE);
        graphics.putString(2, 6, truncate(String.format("WPM : %.1f", result.wpm()), width - 4));
        graphics.putString(2, 7, truncate(String.format("Précision : %.1f%%", result.accuracyPercent()), width - 4));
        graphics.putString(2, 8, truncate(String.format("Temps : %.1fs", result.elapsedSeconds()), width - 4));

        screen.refresh();
    }

    /**
     * Dessine la barre d'en-tête commune aux trois écrans.
     *
     * @param graphics contexte graphique Lanterna
     * @param width    largeur du terminal
     * @param phaseTag libellé de la phase courante
     */
    private void drawHeader(TextGraphics graphics, int width, String phaseTag) {
        String header = String.format(" DACTILO [%s] | Salle: %s | Joueur: %s | [Echap] Quitter ",
                phaseTag, roomName, localUsername);
        graphics.setForegroundColor(TextColor.ANSI.CYAN);
        graphics.putString(0, 0, truncate(header, width), SGR.BOLD);
    }

    /**
     * Retourne la taille courante du terminal en gérant les redimensionnements.
     *
     * @return dimensions du terminal
     */
    private TerminalSize getTerminalSize() {
        TerminalSize size = screen.doResizeIfNecessary();
        return size != null ? size : screen.getTerminalSize();
    }
    /**
     * Retourne le texte actuellement saisi par le joueur.
     *
     * @return texte tapé dans la zone de saisie
     */
    public String getTypedText() {
        return typedInput.toString();
    }
    /**
     * Affiche le texte cible en colorant chaque caractère selon son état (vert, rouge ou en attente).
     *
     * @param graphics contexte graphique Lanterna
     * @param startX   colonne de départ d'affichage
     * @param startY   ligne de départ d'affichage
     * @param maxWidth largeur maximale disponible
     * @param maxRows  nombre de lignes maximal disponible
     */
    private void drawTargetText(TextGraphics graphics, int startX, int startY, int maxWidth, int maxRows) {
        if (maxWidth <= 0 || maxRows <= 0) {
            return;
        }

        String text = calculator.getTargetText();
        int x = startX;
        int y = startY;

        for (int i = 0; i < text.length(); i++) {
            if (x >= startX + maxWidth) {
                x = startX;
                y++;
                if (y >= startY + maxRows) {
                    break;
                }
            }

            char ch = text.charAt(i);
            TypeCalculator.CharStatus status = calculator.getStatusAt(i);

            TextColor fg = switch (status) {
                case CORRECT -> TextColor.ANSI.GREEN;
                case WRONG -> TextColor.ANSI.RED;
                case PENDING -> TextColor.ANSI.WHITE;
            };

            graphics.setForegroundColor(fg);
            graphics.putString(x, y, String.valueOf(ch));
            x++;
        }
    }

    /**
     * Affiche les caractères saisis par le joueur en vert si corrects et en rouge si faux.
     *
     * @param graphics contexte graphique Lanterna
     * @param startX   colonne d'affichage de la saisie
     * @param startY   ligne d'affichage de la saisie
     * @param maxWidth largeur maximale disponible
     */
    private void drawTypedInput(TextGraphics graphics, int startX, int startY, int maxWidth) {
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
            graphics.setForegroundColor(status == TypeCalculator.CharStatus.CORRECT
                    ? TextColor.ANSI.GREEN
                    : TextColor.ANSI.RED);
            graphics.putString(startX + i, startY, String.valueOf(ch));
        }

        if (!calculator.isFinished() && limit < maxWidth) {
            graphics.setForegroundColor(TextColor.ANSI.WHITE);
            graphics.putString(startX + limit, startY, " ", SGR.REVERSE);
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
