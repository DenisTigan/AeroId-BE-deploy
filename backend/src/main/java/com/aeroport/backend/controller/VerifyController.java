package com.aeroport.backend.controller;


import com.aeroport.backend.dto.intern.PythonVerifyResult;
import com.aeroport.backend.dto.response.VerifyResponse;
import com.aeroport.backend.service.CryptoService;
import com.aeroport.backend.service.PythonIntegrationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class VerifyController {

    private final CryptoService cryptoService;
    private final PythonIntegrationService pythonIntegrationService;

    public VerifyController(CryptoService cryptoService, PythonIntegrationService pythonIntegrationService) {
        this.cryptoService = cryptoService;
        this.pythonIntegrationService = pythonIntegrationService;
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPassenger(
            @RequestParam("qrData") String qrData,
            @RequestParam("livePhoto") MultipartFile livePhoto) {

        if (livePhoto == null || livePhoto.isEmpty()) {
            return ResponseEntity.badRequest().body("Eroare: Poza live de la poarta lipseste.");
        }
        if (qrData == null || !qrData.contains("|")) {
            return ResponseEntity.badRequest().body("Eroare: Codul QR scanat este invalid.");
        }

        try {
            String[] qrParts = qrData.split("\\|");

            // Cerem 5 elemente acum
            if (qrParts.length < 5) {
                return ResponseEntity.badRequest().body("Eroare: Format QR incomplet sau versiune veche de bilet.");
            }

            String biometricVector = qrParts[0];
            String lastName = qrParts[1];
            String firstName = qrParts[2];
            String flight = qrParts[3];
            String expirationString = qrParts[4]; // Extragem data

            String fullName = lastName + " " + firstName;

            // --- NOU: Verificam daca QR-ul este expirat ---
            try {
                java.time.LocalDateTime expirationDate = java.time.LocalDateTime.parse(expirationString);

                // Daca timpul de acum este DUPA timpul din bilet -> Respins!
                if (java.time.LocalDateTime.now().isAfter(expirationDate)) {
                    java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
                    String dataFrumoasa = expirationDate.format(formatter);

                    return ResponseEntity.ok(new VerifyResponse(
                            false,
                            fullName,
                            flight,
                            "Acces Respins! Biletul a expirat la: " + dataFrumoasa,
                            null,
                            "QR Expirat"
                    ));
                }
            } catch (java.time.format.DateTimeParseException e) {
                return ResponseEntity.badRequest().body("Eroare: Formatul datei din QR este invalid.");
            }

            // Daca biletul NU e expirat, continuam normal...
            String encryptedLivePhoto = cryptoService.encryptImage(livePhoto.getBytes());

            PythonVerifyResult aiResult = pythonIntegrationService.verifyFaceWithPython(encryptedLivePhoto, biometricVector);

            // Dam verdictul pe baza aiResult.isMatch() si trimitem TOTUL catre React
            if (aiResult.isMatch()) {
                VerifyResponse response = new VerifyResponse(
                        true,
                        fullName,
                        flight,
                        "Acces Permis! Fata confirmata.",
                        aiResult.distance(),
                        aiResult.message()
                );
                return ResponseEntity.ok(response);
            } else {
                VerifyResponse response = new VerifyResponse(
                        false,
                        fullName,
                        flight,
                        "Acces Respins! Motiv AI: " + aiResult.message(),
                        aiResult.distance(),
                        aiResult.message()
                );
                return ResponseEntity.ok(response);
            }

        } catch (Exception e) {
            System.err.println("Eroare grava la verificarea la poarta: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Eroare interna pe server la verificare.");
        }
    }
}
