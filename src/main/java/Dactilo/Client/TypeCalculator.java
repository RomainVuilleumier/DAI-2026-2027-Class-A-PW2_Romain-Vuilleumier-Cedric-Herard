package Dactilo.Client;

import java.util.Arrays;
import java.util.Objects;

/**
 * Moteur de calcul pour le suivi de frappe, la progression, la précision et les WPM.
 */
public class TypeCalculator {

    /**
     * État de validation d'un caractère du texte cible.
     */
    public enum CharStatus {
        /** Caractère pas encore atteint. */
        PENDING,
        /** Caractère saisi correspondant au texte cible. */
        CORRECT,
        /** Caractère saisi différent du texte cible. */
        WRONG
    }

    private final String targetText;
    private final CharStatus[] statuses;

    private long startTimeNanos = -1L;
    private long endTimeNanos = -1L;

    private int totalKeystrokes = 0;
    private int correctKeystrokes = 0;
    private int currentIndex = 0;

    /**
     * Initialise le calculateur avec le texte cible à saisir.
     *
     * @param targetText texte de référence (non nul)
     */
    public TypeCalculator(String targetText) {
        this.targetText = Objects.requireNonNull(targetText, "targetText must not be null");
        this.statuses = new CharStatus[targetText.length()];
        reset();
    }

    /**
     * Enregistre la saisie d'un caractère à la position courante et met à jour le chronomètre.
     *
     * @param typedChar caractère saisi par l'utilisateur
     */
    public void onCharTyped(char typedChar) {
        if (isFinished() || targetText.isEmpty()) {
            return;
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
    }

    /**
     * Recule d'un caractère si possible et repasse sa position en attente.
     */
    public void onBackspace() {
        if (currentIndex <= 0) {
            return;
        }
        currentIndex--;
        statuses[currentIndex] = CharStatus.PENDING;
    }

    /**
     * Réinitialise l'état de la saisie, les compteurs et le chronomètre.
     */
    public void reset() {
        this.startTimeNanos = -1L;
        this.endTimeNanos = -1L;
        this.totalKeystrokes = 0;
        this.correctKeystrokes = 0;
        this.currentIndex = 0;
        Arrays.fill(statuses, CharStatus.PENDING);
    }

    /**
     * Retourne le texte cible à taper.
     *
     * @return texte de référence
     */
    public String getTargetText() {
        return targetText;
    }

    /**
     * Retourne l'état d'un caractère donné dans le texte cible.
     *
     * @param index indice du caractère
     * @return statut du caractère ({@link CharStatus#PENDING}, {@link CharStatus#CORRECT} ou {@link CharStatus#WRONG})
     */
    public CharStatus getStatusAt(int index) {
        return statuses[index];
    }

    /**
     * Retourne l'indice courant de frappe dans le texte cible.
     *
     * @return position courante
     */
    public int getCurrentIndex() {
        return currentIndex;
    }

    /**
     * Indique si l'ensemble du texte cible a été parcouru.
     *
     * @return {@code true} si la saisie est terminée, {@code false} sinon
     */
    public boolean isFinished() {
        return !targetText.isEmpty() && currentIndex >= targetText.length();
    }

    /**
     * Calcule le pourcentage de caractères corrects par rapport à la longueur du texte cible.
     *
     * @return progression en pourcentage (0.0 à 100.0)
     */
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

    /**
     * Calcule la précision globale en pourcentage sur l'ensemble des frappes effectuées.
     *
     * @return précision en pourcentage (0.0 à 100.0)
     */
    public double getAccuracyPercent() {
        if (totalKeystrokes == 0) {
            return 100.0;
        }
        return (correctKeystrokes * 100.0) / totalKeystrokes;
    }

    /**
     * Retourne le temps écoulé en secondes depuis la première frappe.
     *
     * @return durée écoulée en secondes
     */
    public double getElapsedSeconds() {
        if (startTimeNanos < 0L) {
            return 0.0;
        }
        long end = isFinished() && endTimeNanos >= startTimeNanos ? endTimeNanos : System.nanoTime();
        return Math.max(0L, end - startTimeNanos) / 1_000_000_000.0;
    }

    /**
     * Calcule la vitesse en mots par minute (WPM), sur la base standard de 5 caractères par mot.
     *
     * @return vitesse en WPM
     */
    public double getWpm() {
        double elapsedMinutes = getElapsedSeconds() / 60.0;
        if (elapsedMinutes <= 0.0 || correctKeystrokes == 0) {
            return 0.0;
        }
        return (correctKeystrokes / 5.0) / elapsedMinutes;
    }
}