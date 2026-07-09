package com.akash.pooler_backend.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum MatchPreference {
    BOTH,
    MALE,
    FEMALE;

    @JsonCreator
    public static MatchPreference fromJson(String value) {
        if (value == null || value.isBlank()) {
            return BOTH;
        }
        return switch (value.trim().toUpperCase()) {
            case "MALE", "MEN", "M" -> MALE;
            case "FEMALE", "WOMEN", "F" -> FEMALE;
            case "BOTH", "ANY", "ALL" -> BOTH;
            default -> BOTH;
        };
    }
}
