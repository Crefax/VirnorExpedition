package com.virnor.expedition.data;

/**
 * Mob difficulty levels that affect health, damage, and count
 */
public enum MobDifficulty {
    EASY(0.5, 0.5, 0.75),      // 50% health, 50% damage, 75% count
    NORMAL(1.0, 1.0, 1.0),     // 100% health, 100% damage, 100% count
    HARD(1.5, 1.5, 1.25);      // 150% health, 150% damage, 125% count

    private final double healthMultiplier;
    private final double damageMultiplier;
    private final double countMultiplier;

    MobDifficulty(double healthMultiplier, double damageMultiplier, double countMultiplier) {
        this.healthMultiplier = healthMultiplier;
        this.damageMultiplier = damageMultiplier;
        this.countMultiplier = countMultiplier;
    }

    public double getHealthMultiplier() {
        return healthMultiplier;
    }

    public double getDamageMultiplier() {
        return damageMultiplier;
    }

    public double getCountMultiplier() {
        return countMultiplier;
    }

    /**
     * Get display name with color
     */
    public String getDisplayName() {
        return switch (this) {
            case EASY -> "&aKolay";
            case NORMAL -> "&eNormal";
            case HARD -> "&cZor";
        };
    }

    /**
     * Get next difficulty in cycle
     */
    public MobDifficulty next() {
        return switch (this) {
            case EASY -> NORMAL;
            case NORMAL -> HARD;
            case HARD -> EASY;
        };
    }

    /**
     * Parse from string, defaults to NORMAL
     */
    public static MobDifficulty fromString(String str) {
        if (str == null) return NORMAL;
        try {
            return valueOf(str.toUpperCase());
        } catch (IllegalArgumentException e) {
            return NORMAL;
        }
    }
}
