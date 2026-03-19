package com.kronohealth.profile;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record UserProfileRequest(

        @Past(message = "Date of birth must be in the past")
        LocalDate dateOfBirth,

        BiologicalSex biologicalSex,

        @DecimalMin(value = "50.0", message = "Height must be at least 50 cm")
        @DecimalMax(value = "300.0", message = "Height must be at most 300 cm")
        Double heightCm,

        @DecimalMin(value = "10.0", message = "Weight must be at least 10 kg")
        @DecimalMax(value = "700.0", message = "Weight must be at most 700 kg")
        Double weightKg,

        ActivityLevel activityLevel
) {}

