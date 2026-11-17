package com.kaya.dto.wallet;

import java.math.BigDecimal;

/** Aggregated successful income/expense amounts for one wallet and month. */
public class MonthlyTotals {

    private BigDecimal income;
    private BigDecimal spent;

    public BigDecimal getIncome() {
        return income;
    }

    public void setIncome(BigDecimal income) {
        this.income = income;
    }

    public BigDecimal getSpent() {
        return spent;
    }

    public void setSpent(BigDecimal spent) {
        this.spent = spent;
    }
}
