package com.aeroport.backend.service;

import com.aeroport.backend.dto.intern.PythonVerifyResult;
import com.aeroport.backend.dto.request.PythonExtractRequest;
import com.aeroport.backend.dto.request.PythonVerifyRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
        String pythonUrl = "https://aeroid-py.ticaratandrei.dev/api/enroll";

        PythonExtractRequest payload = new PythonExtractRequest(encryptedImage);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(pythonUrl, payload, String.class);

            return response.getBody();

        } catch (Exception e) {
            System.err.println("Eroare la comunicarea cu Python: " + e.getMessage());
            return "{\"error\": \"Nu am putut contacta serverul Python\"}";
        }
    }

    public PythonVerifyResult verifyFaceWithPython(String encryptedImage, String biometricVector) {
        String pythonVerifyUrl = "https://aeroid-py.ticaratandrei.dev/api/verify";
        PythonVerifyRequest requestBody = new PythonVerifyRequest(encryptedImage, biometricVector);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<PythonVerifyRequest> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(pythonVerifyUrl, requestEntity, String.class);
            String responseBody = response.getBody();
            System.out.println("RASPUNS BRUT DE LA PYTHON: " + responseBody);

            if (response.getStatusCode().is2xxSuccessful() && responseBody != null) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode rootNode = mapper.readTree(responseBody);

                // --- NOU: 1. Verificam "Tailgating-ul" (mai multe persoane la poarta) ---
                if (rootNode.has("multiple_persons") && rootNode.get("multiple_persons").asBoolean()) {
                    String msg = rootNode.hasNonNull("message") ? rootNode.get("message").asText() : "Acces Respins! Mai multe persoane detectate.";
                    return new PythonVerifyResult(false, null, msg);
                }

                // --- NOU: 2. Verificam daca lipseste complet obiectul verification_results (ex: nicio persoana) ---
                if (!rootNode.hasNonNull("verification_results")) {
                    String msg = rootNode.hasNonNull("message") ? rootNode.get("message").asText() : "Eroare: Nu a fost detectată nicio față.";
                    return new PythonVerifyResult(false, null, msg);
                }

                // 3. Daca totul e bine, extragem datele de match
                JsonNode resultsNode = rootNode.get("verification_results");

                boolean isMatch = resultsNode.has("is_match") && resultsNode.get("is_match").asBoolean();

                Double distance = null;
                if (resultsNode.hasNonNull("distance")) {
                    distance = resultsNode.get("distance").asDouble();
                }

                String message = "Fara mesaj";
                if (resultsNode.hasNonNull("message")) {
                    message = resultsNode.get("message").asText();
                } else if (rootNode.hasNonNull("message")) {
                    message = rootNode.get("message").asText(); // fallback pe root message
                }

                // Returnam pachetul complet!
                return new PythonVerifyResult(isMatch, distance, message);
            }

            // Fail-safe: daca JSON-ul e prost
            return new PythonVerifyResult(false, null, "Eroare la parsarea JSON-ului din Python.");

        } catch (Exception e) {
            System.err.println("Eroare la comunicarea cu Python pt verificare: " + e.getMessage());
            return new PythonVerifyResult(false, null, "Eroare de conexiune cu serverul Python.");
        }
    }
}