package com.aeroport.backend.repository;

import com.aeroport.backend.model.FlightRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FlightRecordRepository extends JpaRepository<FlightRecord, Long> {

    Optional<FlightRecord> findByFlightIdAndLastNameAndFirstName(String flightId, String lastName, String firstName);
}
