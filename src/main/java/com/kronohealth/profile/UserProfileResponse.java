package com.kronohealth.profile;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserProfileResponse(
        String name,
        String email,
        LocalDate dateOfBirth,
        BiologicalSex biologicalSex,
        Double heightCm,
        Double weightKg,
        ActivityLevel activityLevel,
        Double bmi,
        Double bmr,
        Double tdee
) {}

