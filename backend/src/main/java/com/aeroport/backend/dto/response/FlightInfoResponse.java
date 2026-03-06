package com.aeroport.backend.dto.response;

public record FlightInfoResponse(
        boolean success,
        String message,
        String passengerName,
        String flightId,
        String departure,
        String arrival,
        String flightDate,
        String boardingHour,
        String gate,
        String seat,
        String flightTime
) {
}