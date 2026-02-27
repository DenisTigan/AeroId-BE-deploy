package com.aeroport.backend.controller;

import com.aeroport.backend.dto.response.EnrollResponse;
import com.aeroport.backend.service.JwtService;
import com.aeroport.backend.service.QrService;
import org.springframework.http.HttpStatus;
import com.aeroport.backend.service.CryptoService;
import com.aeroport.backend.service.PythonIntegrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api")
public class EnrollController {

    private final CryptoService cryptoService;
    private final PythonIntegrationService pythonIntegrationService;
    private final JwtService jwtService;
    private final QrService qrService;

    // Injectam toate serviciile de care avem nevoie
    public EnrollController(CryptoService cryptoService, PythonIntegrationService pythonIntegrationService, JwtService jwtService, QrService qrService) {
        this.cryptoService = cryptoService;
        this.pythonIntegrationService = pythonIntegrationService;
        this.jwtService = jwtService;
        this.qrService = qrService;
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
            // 1. Criptam poza si o trimitem la Python
            String encryptedData = cryptoService.encryptImage(photo.getBytes());
            String biometricVector = pythonIntegrationService.sendImageToPython(encryptedData);

            // Verificam daca Python ne-a dat o eroare in loc de vector
            if (biometricVector == null || biometricVector.contains("error")) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Eroare la procesarea fetei in Python.");
            }

            // 2. Generam Pasaportul Digital (JWT)
            String token = jwtService.generateToken(name, flight, biometricVector);

            // 3. Generam Imaginea QR Code
            String qrCodeBase64 = qrService.generateQrCode(token);

            // 4. Returnam rezultatul final catre React
            EnrollResponse response = new EnrollResponse(true, token, qrCodeBase64, name, flight);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("Eroare la enroll: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Eroare interna la emiterea biletului.");
        }
    }
}
