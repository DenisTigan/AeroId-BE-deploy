package com.aeroport.backend.controller;


import com.aeroport.backend.dto.response.FlightInfoResponse;
import com.aeroport.backend.model.FlightRecord;
import com.aeroport.backend.service.FlightDatabaseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api")
public class FlightInfoController {

    private final FlightDatabaseService flightDatabaseService;

    // Injectam doar serviciul bazei de date, nu avem nevoie de Python aici
    public FlightInfoController(FlightDatabaseService flightDatabaseService) {
        this.flightDatabaseService = flightDatabaseService;
    }

    @GetMapping("/flight-info")
    public ResponseEntity<?> getFlightDetails(
            @RequestParam("name") String name,
            @RequestParam("flight") String flight) {

        try {
            // 1. Despartim numele exact cum am facut si inainte
            String cleanName = name.replace("%20", " ");

            // 2. Acum despartim numele curat
            String[] nameParts = cleanName.trim().split(" ", 2);
            String firstName = nameParts.length > 0 ? nameParts[0].trim() : "";
            String lastName = nameParts.length > 1 ? nameParts[1].trim() : "";

            // 3. Cautam in baza de date
            Optional<FlightRecord> passengerOpt = flightDatabaseService.getPassengerFlightDetails(flight.trim(), lastName, firstName);

            // 3. Daca NU gasim pasagerul, returnam eroare 404 Not Found
            if (passengerOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Eroare: Nu am gasit zborul " + flight + " pentru pasagerul " + name + ".");
            }

            // 4. Daca l-am gasit, extragem datele
            FlightRecord dbRecord = passengerOpt.get();

            // 5. Impachetam totul in noul nostru DTO
            FlightInfoResponse response = new FlightInfoResponse(
                    true,
                    "Datele au fost gasite cu succes!",
                    name,
                    dbRecord.getFlightId(),
                    dbRecord.getDeparture(),
                    dbRecord.getArrival(),
                    dbRecord.getFlightDate(),
                    dbRecord.getBoardingHour(),
                    dbRecord.getGate(),
                    dbRecord.getSeat(),
                    dbRecord.getFlightTime()
            );

            // 6. Returnam JSON-ul cu Status 200 OK
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("Eroare la cautarea in baza de date: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Eroare interna la interogarea zborului.");
        }
    }
}
