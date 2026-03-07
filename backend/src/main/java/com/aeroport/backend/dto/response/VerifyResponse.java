package com.aeroport.backend.dto.response;

public record VerifyResponse(
        boolean isMatch,
        String passengerName,
        String flight,
        String message,
        Double distance,
        String aiMessage
) {}