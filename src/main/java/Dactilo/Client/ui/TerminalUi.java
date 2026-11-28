package Dactilo.Client.ui;

import Dactilo.Client.GameLogic;
import Dactilo.Client.KeyHandler;
import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;

import java.io.IOException;
import java.util.Objects;

/**
 * Interface terminale Lanterna dédiée à l'affichage des écrans Lobby, Course et Résultats.
 */
public class TerminalUi implements KeyHandler.KeyListener {

    private static final String[] LOBBY_ASCII_ART = {
            "  ____             _   _ _          ____ _   _ ____   ____  __  ____     __",
            " |  _ \\  __ _  ___| |_(_) | ___    / ___| | | |  _ \\ |  _ \\|  \\/  \\ \\   / /",
            " | | | |/ _` |/ __| __| | |/ _ \\  | |   | |_| | | | || |_) | |\\/| |\\ \\ / / ",
            " | |_| | (_| | (__| |_| | | (_) | | |___|  _  | |_| ||  _ <| |  | | \\ V /  ",
            " |____/ \\__,_|\\___|\\__|_|_|\\___/   \\____|_| |_|____(_)_| \\_\\_|  |_|  \\_/   "
    };

    private final String roomName;
    private final String localUsername;
    private final GameLogic gameLogic;
    private final KeyHandler keyHandler;

    private volatile boolean running = false;
    private Terminal terminal;
    private Screen screen;

    public TerminalUi(String roomName, String localUsername, GameLogic gameLogic) {
        this.roomName = Objects.requireNonNull(roomName, "roomName must not be null");
        this.localUsername = Objects.requireNonNull(localUsername, "localUsername must not be null");
        this.gameLogic = Objects.requireNonNull(gameLogic, "gameLogic must not be null");
        this.keyHandler = new KeyHandler(this);
    }

    public void setPhase(GameLogic.GamePhase newPhase) {
        gameLogic.setPhase(newPhase);
    }

    public void setLocalResult(GameLogic.LocalResult result) {
        gameLogic.setLocalResult(result);
    }

    public String getTypedText() {
        return gameLogic.getTypedText();
    }

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

    public void close() {
        running = false;
        gameLogic.setPhase(GameLogic.GamePhase.Finish);
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

    private void inputAndRenderLoop() throws IOException {
        while (running) {
            switch (gameLogic.getPhase()) {
                case Lobby -> renderLobbyPhase();
                case Game -> renderGamePhase();
                case Finish -> renderFinishPhase();
            }

            KeyStroke key = screen.pollInput();
            if (key != null) {
                keyHandler.handle(key);
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

    @Override
    public void onExitKey() {
        running = false;
        gameLogic.handleExit();
    }

    @Override
    public void onBackspaceKey() {
        gameLogic.handleBackspace();
    }

    @Override
    public void onCharacterKey(char c) {
        gameLogic.handleCharacterTyped(c);
    }

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

        boolean finished = gameLogic.isFinished();
        int statsRow = Math.max(12, height - 3);
        String stats = finished
                ? String.format(" Terminé ! WPM: %.1f | Précision: %.1f%% | Temps: %.1fs ",
                gameLogic.getWpm(), gameLogic.getAccuracyPercent(), gameLogic.getElapsedSeconds())
                : String.format(" WPM: %.1f | Précision: %.1f%% | Progression: %.1f%% | Temps: %.1fs ",
                gameLogic.getWpm(), gameLogic.getAccuracyPercent(),
                gameLogic.getProgressPercent(), gameLogic.getElapsedSeconds());

        graphics.setForegroundColor(finished ? TextColor.ANSI.GREEN_BRIGHT : TextColor.ANSI.WHITE_BRIGHT);
        graphics.putString(0, statsRow, truncate(stats, width), SGR.BOLD);

        screen.refresh();
    }

    private void renderFinishPhase() throws IOException {
        TerminalSize size = getTerminalSize();
        int width = size.getColumns();

        TextGraphics graphics = screen.newTextGraphics();
        graphics.setBackgroundColor(TextColor.ANSI.BLACK);
        graphics.fill(' ');

        drawHeader(graphics, width, "RÉSULTATS");

        GameLogic.LocalResult result = gameLogic.getLocalResultOrDefault();

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

    private void drawHeader(TextGraphics graphics, int width, String phaseTag) {
        String header = String.format(" DACTILO [%s] | Salle: %s | Joueur: %s | [Echap] Quitter ",
                phaseTag, roomName, localUsername);
        graphics.setForegroundColor(TextColor.ANSI.CYAN);
        graphics.putString(0, 0, truncate(header, width), SGR.BOLD);
    }

    private TerminalSize getTerminalSize() {
        TerminalSize size = screen.doResizeIfNecessary();
        return size != null ? size : screen.getTerminalSize();
    }

    private void drawTargetText(TextGraphics graphics, int startX, int startY, int maxWidth, int maxRows) {
        if (maxWidth <= 0 || maxRows <= 0) {
            return;
        }

        String text = gameLogic.getTargetText();
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
            GameLogic.CharStatus status = gameLogic.getStatusAt(i);

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

    private void drawTypedInput(TextGraphics graphics, int startX, int startY, int maxWidth) {
        if (maxWidth <= 0) {
            return;
        }

        String typed = gameLogic.getTypedText();
        int limit = Math.min(typed.length(), maxWidth);

        for (int i = 0; i < limit; i++) {
            char ch = typed.charAt(i);
            GameLogic.CharStatus status = i < gameLogic.getTargetText().length()
                    ? gameLogic.getStatusAt(i)
                    : GameLogic.CharStatus.WRONG;
            graphics.setForegroundColor(status == GameLogic.CharStatus.CORRECT
                    ? TextColor.ANSI.GREEN
                    : TextColor.ANSI.RED);
            graphics.putString(startX + i, startY, String.valueOf(ch));
        }

        if (!gameLogic.isFinished() && limit < maxWidth) {
            graphics.setForegroundColor(TextColor.ANSI.WHITE);
            graphics.putString(startX + limit, startY, " ", SGR.REVERSE);
        }
    }

    private String truncate(String s, int maxLen) {
        if (s == null || maxLen <= 0) {
            return "";
        }
        return s.length() <= maxLen ? s : s.substring(0, maxLen - 1) + "…";
    }
}