package com.aeroport.backend.service;

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

    public boolean verifyFaceWithPython(String encryptedImage, String biometricVector) {
        String pythonVerifyUrl = "https://aeroid-py.ticaratandrei.dev/api/verify";

        PythonVerifyRequest requestBody = new PythonVerifyRequest(encryptedImage, biometricVector);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<PythonVerifyRequest> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    pythonVerifyUrl,
                    requestEntity,
                    String.class
            );

            String responseBody = response.getBody();
            System.out.println("RASPUNS BRUT DE LA PYTHON (VERIFY): " + responseBody);

            if (response.getStatusCode().is2xxSuccessful() && responseBody != null) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode rootNode = mapper.readTree(responseBody);

                if (rootNode.has("match")) {
                    boolean isMatch = rootNode.get("match").asBoolean();
                    System.out.println("Verdict final extras: " + isMatch);
                    return isMatch;
                } else if (responseBody.toLowerCase().contains("true")) {
                    return true;
                }
            }

            return false;

        } catch (Exception e) {
            System.err.println("Eroare la comunicarea cu Python pt verificare: " + e.getMessage());
            return false;
        }
    }
}
