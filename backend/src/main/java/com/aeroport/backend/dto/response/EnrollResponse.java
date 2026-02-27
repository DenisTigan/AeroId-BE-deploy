package com.aeroport.backend.dto.response;

public record EnrollResponse(
        boolean success,
        String token,
        String qrCode,
        String passengerName,
        String flight
) {}