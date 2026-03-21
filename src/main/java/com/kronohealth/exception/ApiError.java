package com.kronohealth.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

/**
 * Egységes JSON hibaüzenet formátum az összes REST végponthoz.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {

    /** Gépi kód a hiba típusának azonosítására (pl. VALIDATION_FAILED, AUTH_FAILED). */
    private final String errorCode;

    /** Ember által olvasható hibaüzenet. */
    private final String message;

    /** HTTP státuszkód (pl. 400, 401, 404, 500). */
    private final int status;

    /** A hiba keletkezésének időbélyege (UTC). */
    @Builder.Default
    private final Instant timestamp = Instant.now();

    /** Opcionális: validációs hibák mezőnkénti bontásban. */
    private final Map<String, String> fieldErrors;
}

