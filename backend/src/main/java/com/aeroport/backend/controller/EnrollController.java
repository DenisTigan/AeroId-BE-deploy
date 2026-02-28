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



@RestController
@RequestMapping("/api")
public class EnrollController {


    private final CryptoService cryptoService;
    private final PythonIntegrationService pythonIntegrationService;
    private final JwtService jwtService;
    private final QrService qrService;


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
            String pythonJsonResponse = pythonIntegrationService.sendImageToPython(encryptedData);

            if (pythonJsonResponse == null || pythonJsonResponse.contains("error")) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Eroare la procesarea fetei in Python.");
            }

            // 2. Taierea vectorului

            String justTheVector = "";

            if (pythonJsonResponse.contains("\"biometric_vector\":\"")) {
                int startIndex = pythonJsonResponse.indexOf("\"biometric_vector\":\"") + 20;
                int endIndex = pythonJsonResponse.indexOf("\"", startIndex);
                justTheVector = pythonJsonResponse.substring(startIndex, endIndex);
            }
            else if (pythonJsonResponse.contains("\"biometric_vector\": \"")) {
                int startIndex = pythonJsonResponse.indexOf("\"biometric_vector\": \"") + 21;
                int endIndex = pythonJsonResponse.indexOf("\"", startIndex);
                justTheVector = pythonJsonResponse.substring(startIndex, endIndex);
            }
            else {
                justTheVector = "Vector_Negasit";
                System.out.println("Atentie: Nu am gasit 'biometric_vector' in JSON-ul de la Python!");
            }

            // 3. Extragem Numele si Prenumele
            String[] nameParts = name.split(" ", 2);
            String lastName = nameParts.length > 0 ? nameParts[0] : "Nume";
            String firstName = nameParts.length > 1 ? nameParts[1] : "Prenume";
            String date = java.time.LocalDate.now().toString();

            // 4. CONSTRUIM STRING-UL
            String rawQrData = justTheVector + "|" + lastName + "|" + firstName + "|" + flight + "|" + date;

            // 5. Generam Imaginea QR Code
            String qrCodeBase64 = qrService.generateQrCode(rawQrData);

            // 6. Returnam rezultatul final
            EnrollResponse response = new EnrollResponse(true, rawQrData, qrCodeBase64, name, flight);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("Eroare la enroll: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Eroare interna la emiterea biletului.");
        }
    }
}
