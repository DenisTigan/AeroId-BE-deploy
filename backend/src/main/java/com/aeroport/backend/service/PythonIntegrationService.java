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

    // Schimbam tipul returnat din boolean in PythonVerifyResult
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

                // Cautam obiectul "verification_results" in JSON-ul de la Python
                if (rootNode.has("verification_results")) {
                    JsonNode resultsNode = rootNode.get("verification_results");

                    // Extragem fiecare valoare in parte, cu grija la null-uri
                    boolean isMatch = resultsNode.has("is_match") && resultsNode.get("is_match").asBoolean();

                    Double distance = null;
                    if (resultsNode.hasNonNull("distance")) {
                        distance = resultsNode.get("distance").asDouble();
                    }

                    String message = "Fara mesaj";
                    if (resultsNode.hasNonNull("message")) {
                        message = resultsNode.get("message").asText();
                    }

                    // Returnam pachetul complet!
                    return new PythonVerifyResult(isMatch, distance, message);
                }
            }
            // Fail-safe: daca JSON-ul e prost, returnam false si date goale
            return new PythonVerifyResult(false, null, "Eroare la parsarea JSON-ului din Python.");

        } catch (Exception e) {
            System.err.println("Eroare la comunicarea cu Python pt verificare: " + e.getMessage());
            return new PythonVerifyResult(false, null, "Eroare de conexiune cu serverul Python.");
        }
    }
}
