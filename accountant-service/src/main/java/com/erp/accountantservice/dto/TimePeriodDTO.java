package com.erp.accountantservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.lang.Comparable;  // ✅ ADD THIS

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimePeriodDTO implements Comparable<TimePeriodDTO>, Serializable {

    private String period;
    private double revenue;
    private double expenses;
    private double profit;

    @Override
    public int compareTo(TimePeriodDTO other) {
        return this.period.compareTo(other.period);
    }
}

