package com.aeroport.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

@Service
public class JwtService {
    private final PrivateKey privateKey;
    private final PublicKey publicKey;

    // Constructor: Generam cheile automat in memorie la pornirea aplicatiei!
    public JwtService() throws Exception {
        KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance("RSA");
        keyGenerator.initialize(2048);
        KeyPair keyPair = keyGenerator.generateKeyPair();

        this.privateKey = keyPair.getPrivate();
        this.publicKey = keyPair.getPublic();

        System.out.println("✅ Chei RSA generate automat in memorie cu succes!");
    }

    /**
     * Generează biletul (JWT) folosit în Fluxul 1.
     */
    public String generateToken(String passengerName, String flight, String biometricVector) {
        long expirationTimeInMs = 48 * 60 * 60 * 1000; // Valabil 48 de ore

        return Jwts.builder()
                .subject(passengerName)
                .claim("flight", flight)
                .claim("bio", biometricVector)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationTimeInMs))
                .signWith(this.privateKey) // Semnat digital
                .compact();
    }

    /**
     * Validează și extrage datele din bilet (Folosit în Fluxul 2 la poartă).
     */
    public Claims validateAndExtractToken(String token) {
        return Jwts.parser()
                .verifyWith(this.publicKey) // Verificat cu cheia publica
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
