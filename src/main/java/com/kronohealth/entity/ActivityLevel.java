package com.kronohealth.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ActivityLevel {
    SEDENTARY(1.2, "Sedentary (little or no exercise)"),
    LIGHTLY_ACTIVE(1.375, "Lightly active (1–3 days/week)"),
    MODERATELY_ACTIVE(1.55, "Moderately active (3–5 days/week)"),
    VERY_ACTIVE(1.725, "Very active (6–7 days/week)"),
    SUPER_ACTIVE(1.9, "Super active (physical job or 2x training/day)");

    private final double multiplier;
    private final String label;
}

