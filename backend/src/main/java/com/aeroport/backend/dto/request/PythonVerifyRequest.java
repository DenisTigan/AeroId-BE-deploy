package com.aeroport.backend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PythonVerifyRequest(
        @JsonProperty("encrypted_image") String encryptedImage,
        @JsonProperty("biometric_vector") String biometricVector
) {}
