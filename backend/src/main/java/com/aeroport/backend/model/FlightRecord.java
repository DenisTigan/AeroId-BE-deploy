package com.aeroport.backend.model;


import jakarta.persistence.*;

@Entity
@Table(name = "flight_records") // Asa se va numi tabelul in baza de date
public class FlightRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Cheia primara generata automat (1, 2, 3...)

    @Column(nullable = false)
    private String firstName; // Prenume

    @Column(nullable = false)
    private String lastName; // Nume

    @Column(nullable = false, name = "flight_id")
    private String flightId; // id_zbor (ex: RO305)

    @Column(name = "flight_date")
    private String flightDate;

    private String departure; // ex: OTP (Bucuresti)
    private String arrival;   // ex: CDG (Paris)

    @Column(name = "boarding_hour")
    private String boardingHour; // ex: 14:30

    private String gate; // ex: 14B
    private String seat; // ex: 12A

    @Column(name = "flight_time")
    private String flightTime; // durata zborului, ex: 2h 45m

    // Constructor gol (OBLIGATORIU pentru Spring Boot / JPA)
    public FlightRecord() {
    }

    // Constructor cu toti parametrii (pentru a crea usor obiecte)
    public FlightRecord(String firstName, String lastName, String flightId, String flightDate, String departure, String arrival, String boardingHour, String gate, String seat, String flightTime) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.flightId = flightId;
        this.flightDate = flightDate;
        this.departure = departure;
        this.arrival = arrival;
        this.boardingHour = boardingHour;
        this.gate = gate;
        this.seat = seat;
        this.flightTime = flightTime;
    }

    // GETTERI SI SETTERI (Poti genera asta automat cu IntelliJ: Alt+Insert -> Getter and Setter)
    public Long getId() { return id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getFlightId() { return flightId; }
    public void setFlightId(String flightId) { this.flightId = flightId; }
    public String getFlightDate() { return flightDate; }
    public void setFlightDate(String flightDate) { this.flightDate = flightDate; }
    public String getDeparture() { return departure; }
    public void setDeparture(String departure) { this.departure = departure; }
    public String getArrival() { return arrival; }
    public void setArrival(String arrival) { this.arrival = arrival; }
    public String getBoardingHour() { return boardingHour; }
    public void setBoardingHour(String boardingHour) { this.boardingHour = boardingHour; }
    public String getGate() { return gate; }
    public void setGate(String gate) { this.gate = gate; }
    public String getSeat() { return seat; }
    public void setSeat(String seat) { this.seat = seat; }
    public String getFlightTime() { return flightTime; }
    public void setFlightTime(String flightTime) { this.flightTime = flightTime; }
}
