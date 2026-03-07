package com.aeroport.backend.controller;

import com.aeroport.backend.dto.response.EnrollResponse;
import com.aeroport.backend.model.FlightRecord;
import com.aeroport.backend.service.CryptoService;
import com.aeroport.backend.service.FlightDatabaseService;
import com.aeroport.backend.service.JwtService;
import com.aeroport.backend.service.PythonIntegrationService;
import com.aeroport.backend.service.QrService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


import java.util.Optional;

@RestController
@RequestMapping("/api")
public class EnrollController {

    private final CryptoService cryptoService;
    private final PythonIntegrationService pythonIntegrationService;
    private final JwtService jwtService;
    private final QrService qrService;

    // NOU: Am adus serviciul bazei de date!
    private final FlightDatabaseService flightDatabaseService;

    public EnrollController(CryptoService cryptoService,
                            PythonIntegrationService pythonIntegrationService,
                            JwtService jwtService,
                            QrService qrService,
                            FlightDatabaseService flightDatabaseService) {
        this.cryptoService = cryptoService;
        this.pythonIntegrationService = pythonIntegrationService;
        this.jwtService = jwtService;
        this.qrService = qrService;
        this.flightDatabaseService = flightDatabaseService;
    }

    @PostMapping("/enroll")
    public ResponseEntity<?> enrollPassenger(
            @RequestParam("photo") MultipartFile photo,
            @RequestParam("name") String name,
            @RequestParam("flight") String flight) {

        if (photo == null || photo.isEmpty()) {
            return ResponseEntity.badRequest().body("Eroare: Poza lipseste.");
        }

        try {
            // 1. Curatam numele (reparam problema cu %20)
            String cleanName = name.replace("%20", " ").toLowerCase();
            String[] nameParts = cleanName.trim().split(" ", 2);
            String firstName = nameParts.length > 0 ? nameParts[0].trim() : "";
            String lastName = nameParts.length > 1 ? nameParts[1].trim() : "";

            // 2. VERIFICARE DE SECURITATE IN BAZA DE DATE!
            Optional<FlightRecord> passengerOpt = flightDatabaseService.getPassengerFlightDetails(flight.trim(), lastName, firstName);

            // Daca nu il gasim, aruncam eroarea si OPRIM executia aici!
            if (passengerOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Eroare de securitate: Pasagerul " + cleanName + " nu are un bilet valid pentru zborul " + flight + ".");
            }

            // --- Daca a trecut de IF-ul de mai sus, pasagerul e pe bune! ---

            // 3. Criptam poza si o trimitem la Python
            String encryptedData = cryptoService.encryptImage(photo.getBytes());
            String pythonJsonResponse = pythonIntegrationService.sendImageToPython(encryptedData);

            if (pythonJsonResponse == null || pythonJsonResponse.contains("error")) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Eroare la procesarea fetei in Python.");
            }

            // 4. Taierea vectorului biometric
            // 4. Parsam JSON-ul NOU de la Python folosind ObjectMapper
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(pythonJsonResponse);

            // 4a. Verificam regula "Bouncer-ului" (YOLO Rejection)
            if (rootNode.has("multiple_persons") && rootNode.get("multiple_persons").asBoolean()) {
                return ResponseEntity.badRequest().body("Eroare de securitate: Au fost detectate mai multe persoane în cadru! Doar o singură persoană este permisă.");
            }

            // 4b. Verificam daca vectorul biometric lipseste (DeepFace Error / No face)
            JsonNode biometricNode = rootNode.get("biometric_vector");
            if (biometricNode == null || biometricNode.isNull() || !biometricNode.has("biometric_vector")) {
                String aiMessage = rootNode.has("message") ? rootNode.get("message").asText() : "Nu a fost detectată nicio față.";
                return ResponseEntity.badRequest().body("Eroare AI: " + aiMessage);
            }

            // 4c. Daca totul e perfect, extragem vectorul din interiorul obiectului
            String justTheVector = biometricNode.get("biometric_vector").asText();


            // 5. Calculam data de expirare (Acum + 48 ore) si taiem secundele
            java.time.LocalDateTime expirationDate = java.time.LocalDateTime.now()
                    .plusHours(48)
                    .truncatedTo(java.time.temporal.ChronoUnit.MINUTES);
            String expirationString = expirationDate.toString();

            // 6. CONSTRUIM STRING-UL (Vector + Nume + Prenume + Zbor + Data Expirare)
            String rawQrData = justTheVector + "|" + lastName + "|" + firstName + "|" + flight + "|" + expirationString;

            // 7. Generam Imaginea QR Code
            String qrCodeBase64 = qrService.generateQrCode(rawQrData);

            // 8. Returnam rezultatul final
            EnrollResponse response = new EnrollResponse(true, rawQrData, qrCodeBase64, cleanName, flight);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("Eroare la enroll: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Eroare interna la emiterea biletului.");
        }
    }

    @GetMapping("/check-passenger")
    public ResponseEntity<?> checkPassengerBeforeEnroll(
            @RequestParam("name") String name,
            @RequestParam("flight") String flight) {

        try {
            // 1. Curatam si pregatim datele (exact cu aceeasi logica de la enroll)
            String cleanName = name.replace("%20", " ").toLowerCase();
            String[] nameParts = cleanName.trim().split(" ", 2);
            String firstName = nameParts.length > 0 ? nameParts[0].trim() : "";
            String lastName = nameParts.length > 1 ? nameParts[1].trim() : "";

            // 2. Cautam in baza de date
            Optional<FlightRecord> passengerOpt = flightDatabaseService.getPassengerFlightDetails(flight.trim(), lastName, firstName);

            // 3. Daca NU il gasim, returnam eroare 404 (React va tine camera inchisa)
            if (passengerOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Eroare: Pasagerul nu a fost gasit in baza de date pentru acest zbor.");
            }

            // 4. Daca il gasim, impachetam Numele si Zborul intr-un JSON si le returnam
            java.util.Map<String, String> responseData = new java.util.HashMap<>();
            responseData.put("name", name);       // Returneaza numele trimis
            responseData.put("flight", flight);   // Returneaza zborul trimis
            responseData.put("status", "success");
            responseData.put("message", "Pasager gasit! Permisiune de a deschide camera acordata.");

            return ResponseEntity.ok(responseData);

        } catch (Exception e) {
            System.err.println("Eroare la check-passenger: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Eroare interna la interogarea bazei de date.");
        }
    }

}