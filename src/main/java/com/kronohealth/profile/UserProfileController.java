package com.kronohealth.profile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@Tag(name = "Profile", description = "Personal data, BMI & BMR management")
public class UserProfileController {

    private final UserProfileService profileService;

    @GetMapping
    @Operation(summary = "Get current user's profile with calculated BMI, BMR and TDEE")
    public ResponseEntity<UserProfileResponse> getProfile() {
        return ResponseEntity.ok(profileService.get());
    }

    @PutMapping
    @Operation(summary = "Save or update current user's personal data")
    public ResponseEntity<UserProfileResponse> saveProfile(@Valid @RequestBody UserProfileRequest request) {
        return ResponseEntity.ok(profileService.save(request));
    }
}

