package com.aeroport.backend.service;

import com.aeroport.backend.dto.request.PythonExtractRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class PythonIntegrationService {

    private final RestTemplate restTemplate;

    public PythonIntegrationService() {
        this.restTemplate = new RestTemplate();
    }

    public String sendImageToPython(String encryptedImage) {
        // AICI pui URL-ul unde ruleaza serverul de Python al colegului tau
        String pythonUrl = "https://aeroid-py.ticaratandrei.dev/api/enroll";

        // 1. Impachetam textul criptat in DTO-ul creat mai devreme
        PythonExtractRequest payload = new PythonExtractRequest(encryptedImage);

        try {
            // 2. Facem request-ul POST catre Python.
            // Trimiterm payload-ul (JSON) si ii zicem ca asteptam un String inapoi.
            ResponseEntity<String> response = restTemplate.postForEntity(pythonUrl, payload, String.class);

            // 3. Returnam ce ne-a raspuns Python (probabil vectorul biometric)
            return response.getBody();

        } catch (Exception e) {
            // Daca Python e oprit sau da eroare, prindem problema aici
            System.err.println("Eroare la comunicarea cu Python: " + e.getMessage());
            return "{\"error\": \"Nu am putut contacta serverul Python\"}";
        }
    }
}
