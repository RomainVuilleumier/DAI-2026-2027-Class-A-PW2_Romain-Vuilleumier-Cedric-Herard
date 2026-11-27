package Dactilo.Client;

import java.util.Arrays;
import java.util.Objects;

public class TypeCalculator {

    public enum CharStatus {
        PENDING,
        CORRECT,
        WRONG
    }

    private final String targetText;
    private final CharStatus[] statuses;

    private long startTimeNanos = -1L;
    private long endTimeNanos = -1L;

    private int totalKeystrokes = 0;
    private int correctKeystrokes = 0;
    private int currentIndex = 0;

    public TypeCalculator(String targetText) {
        this.targetText = Objects.requireNonNull(targetText, "targetText must not be null");
        this.statuses = new CharStatus[targetText.length()];
        reset();
    }

    public boolean onCharTyped(char typedChar) {
        if (isFinished() || targetText.isEmpty()) {
            return false;
        }

        if (startTimeNanos < 0L) {
            startTimeNanos = System.nanoTime();
        }

        totalKeystrokes++;
        boolean isCorrect = typedChar == targetText.charAt(currentIndex);

        if (isCorrect) {
            statuses[currentIndex] = CharStatus.CORRECT;
            correctKeystrokes++;
        } else {
            statuses[currentIndex] = CharStatus.WRONG;
        }

        currentIndex++;

        if (currentIndex >= targetText.length()) {
            endTimeNanos = System.nanoTime();
        }

        return isCorrect;
    }

    public void onBackspace() {
        if (currentIndex <= 0) {
            return;
        }
        currentIndex--;
        statuses[currentIndex] = CharStatus.PENDING;
    }

    public void reset() {
        this.startTimeNanos = -1L;
        this.endTimeNanos = -1L;
        this.totalKeystrokes = 0;
        this.correctKeystrokes = 0;
        this.currentIndex = 0;
        Arrays.fill(statuses, CharStatus.PENDING);
    }

    public CharStatus getStatusAt(int index) {
        return statuses[index];
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public boolean isFinished() {
        return !targetText.isEmpty() && currentIndex >= targetText.length();
    }

    public double getProgressPercent() {
        if (targetText.isEmpty()) {
            return 100.0;
        }
        int validChars = 0;
        for (CharStatus s : statuses) {
            if (s == CharStatus.CORRECT) {
                validChars++;
            }
        }
        return (validChars * 100.0) / targetText.length();
    }

    public double getAccuracyPercent() {
        if (totalKeystrokes == 0) {
            return 100.0;
        }
        return (correctKeystrokes * 100.0) / totalKeystrokes;
    }

    public double getElapsedSeconds() {
        if (startTimeNanos < 0L) {
            return 0.0;
        }
        long end = isFinished() && endTimeNanos >= startTimeNanos ? endTimeNanos : System.nanoTime();
        return Math.max(0L, end - startTimeNanos) / 1_000_000_000.0;
    }

    public double getWpm() {
        double elapsedMinutes = getElapsedSeconds() / 60.0;
        if (elapsedMinutes <= 0.0 || correctKeystrokes == 0) {
            return 0.0;
        }
        return (correctKeystrokes / 5.0) / elapsedMinutes;
    }
}