package com.aiweather.entity;

public enum RiskCategory {
    LOW(0), MODERATE(1), HIGH(2), VERY_HIGH(3), INSUFFICIENT_DATA(-1);

    private final int rank;

    RiskCategory(int rank) {
        this.rank = rank;
    }

    /** Ordering by seriousness. INSUFFICIENT_DATA ranks below LOW but is never treated as "safe". */
    public int rank() {
        return rank;
    }
}
