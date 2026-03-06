package com.aeroport.backend.service;


import com.aeroport.backend.model.FlightRecord;
import com.aeroport.backend.repository.FlightRecordRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class FlightDatabaseService {
    private final FlightRecordRepository repository;

    // Injectam repository-ul in mod sigur prin constructor
    public FlightDatabaseService(FlightRecordRepository repository) {
        this.repository = repository;
    }

    /**
     * Metoda care cauta detaliile unui zbor pentru un anumit pasager.
     * Este folosita de FlightInfoController la pasul 1 (verificarea biletului).
     */
    public Optional<FlightRecord> getPassengerFlightDetails(String flightId, String lastName, String firstName) {
        System.out.println("Caut in baza de date: Zborul=" + flightId + ", Nume=" + lastName + ", Prenume=" + firstName);

        return repository.findByFlightIdAndLastNameAndFirstName(flightId, lastName, firstName);
    }

    /**
     * (Optional) Metoda pentru a salva date in viitor daca veti vrea
     * sa adaugati pasageri direct din React (un formular de cumparare bilet).
     */
    public FlightRecord saveFlightRecord(FlightRecord record) {
        return repository.save(record);
    }
}
