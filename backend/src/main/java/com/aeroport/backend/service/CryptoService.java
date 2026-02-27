package com.aeroport.backend.service;

import com.macasaet.fernet.Key;
import com.macasaet.fernet.Token;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CryptoService {
    private final Key fernetKey;

    // Constructorul trage cheia direct din application.properties
    public CryptoService(@Value("${aeroport.security.fernet-key}") String keyString) {
        // Transformăm string-ul într-un obiect Key specific librăriei
        this.fernetKey = new Key(keyString);
    }

    /**
     * Primește bytes-urile pozei și returnează string-ul lung criptat
     */
    public String encryptImage(byte[] imageBytes) {
        // Generăm token-ul Fernet folosind cheia și datele noastre (poza)
        Token token = Token.generate(this.fernetKey, imageBytes);

        // Returnăm token-ul sub formă de String (acela care începe mereu cu gAAAAA...)
        return token.serialise();
    }
}
