package com.kronohealth.service;

import com.kronohealth.dto.profile.*;
import com.kronohealth.entity.ActivityLevel;
import com.kronohealth.entity.BiologicalSex;
import com.kronohealth.entity.UserProfile;
import com.kronohealth.repository.UserProfileRepository;
import com.kronohealth.entity.User;
import com.kronohealth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileService {

    private final UserProfileRepository profileRepository;
    private final UserRepository userRepository;

    @Transactional
    public UserProfileResponse save(UserProfileRequest request) {
        User user = getAuthenticatedUser();

        UserProfile profile = profileRepository.findByUserId(user.getId())
                .orElseGet(() -> UserProfile.builder().userId(user.getId()).build());

        profile.setDateOfBirth(request.dateOfBirth());
        profile.setBiologicalSex(request.biologicalSex());
        profile.setHeightCm(request.heightCm());
        profile.setWeightKg(request.weightKg());
        profile.setActivityLevel(request.activityLevel());
        profile.setUpdatedAt(Instant.now());

        profileRepository.save(profile);
        log.debug("Saved profile for user {}", user.getId());

        return toResponse(user, profile);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse get() {
        User user = getAuthenticatedUser();

        UserProfile profile = profileRepository.findByUserId(user.getId())
                .orElseGet(() -> UserProfile.builder().userId(user.getId()).build());

        return toResponse(user, profile);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private UserProfileResponse toResponse(User user, UserProfile profile) {
        Double bmi  = calculateBmi(profile.getWeightKg(), profile.getHeightCm());
        Double bmr  = calculateBmr(profile.getWeightKg(), profile.getHeightCm(),
                                   profile.getDateOfBirth(), profile.getBiologicalSex());
        Double tdee = calculateTdee(bmr, profile.getActivityLevel());

        return new UserProfileResponse(
                user.getName(),
                user.getEmail(),
                profile.getDateOfBirth(),
                profile.getBiologicalSex(),
                profile.getHeightCm(),
                profile.getWeightKg(),
                profile.getActivityLevel(),
                bmi,
                bmr,
                tdee
        );
    }

    /**
     * BMI = weight(kg) / height(m)²
     * Rounded to 1 decimal.
     */
    private Double calculateBmi(Double weightKg, Double heightCm) {
        if (weightKg == null || heightCm == null) return null;
        double heightM = heightCm / 100.0;
        return Math.round((weightKg / (heightM * heightM)) * 10.0) / 10.0;
    }

    /**
     * BMR — Mifflin-St Jeor equation (more accurate than Harris-Benedict).
     * Male:   BMR = (10 × kg) + (6.25 × cm) − (5 × age) + 5
     * Female: BMR = (10 × kg) + (6.25 × cm) − (5 × age) − 161
     * Rounded to whole number.
     */
    private Double calculateBmr(Double weightKg, Double heightCm,
                                 LocalDate dateOfBirth, BiologicalSex sex) {
        if (weightKg == null || heightCm == null || dateOfBirth == null || sex == null) return null;
        int age = Period.between(dateOfBirth, LocalDate.now()).getYears();
        double base = (10 * weightKg) + (6.25 * heightCm) - (5 * age);
        double bmr  = sex == BiologicalSex.MALE ? base + 5 : base - 161;
        return Math.round(bmr) * 1.0;
    }

    /**
     * TDEE = BMR × activity multiplier
     * Rounded to whole number.
     */
    private Double calculateTdee(Double bmr, ActivityLevel activityLevel) {
        if (bmr == null || activityLevel == null) return null;
        return Math.round(bmr * activityLevel.getMultiplier()) * 1.0;
    }

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + email));
    }
}

