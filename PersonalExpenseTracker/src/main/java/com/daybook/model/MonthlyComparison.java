package com.daybook.model;

/**
 * Value object holding month-over-month expense comparison metrics.
 */
public class MonthlyComparison {

    private final double currentMonthExpense;
    private final double previousMonthExpense;
    private final double difference;
    private final double percentageChange;
    private final boolean hasPreviousData;

    public MonthlyComparison(double currentMonthExpense, double previousMonthExpense, boolean hasPreviousData) {
        this.currentMonthExpense = currentMonthExpense;
        this.previousMonthExpense = previousMonthExpense;
        this.hasPreviousData = hasPreviousData;
        this.difference = currentMonthExpense - previousMonthExpense;
        if (hasPreviousData && previousMonthExpense > 0) {
            this.percentageChange = ((currentMonthExpense - previousMonthExpense) / previousMonthExpense) * 100.0;
        } else {
            this.percentageChange = 0.0;
        }
    }

    public double getCurrentMonthExpense() {
        return currentMonthExpense;
    }

    public double getPreviousMonthExpense() {
        return previousMonthExpense;
    }

    public double getDifference() {
        return difference;
    }

    public double getPercentageChange() {
        return percentageChange;
    }

    public boolean hasPreviousData() {
        return hasPreviousData;
    }

    public boolean isSpendingMore() {
        return hasPreviousData && difference > 0.01;
    }

    public boolean isSpendingLess() {
        return hasPreviousData && difference < -0.01;
    }

    public boolean isSame() {
        return hasPreviousData && Math.abs(difference) <= 0.01;
    }
}
