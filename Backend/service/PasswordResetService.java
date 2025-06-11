package com.example.demo.service;


import org.springframework.stereotype.Service;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PasswordResetService {

    // Store reset codes with email as key (in production, add an expiration policy)
    private ConcurrentHashMap<String, String> resetCodes = new ConcurrentHashMap<>();

    public String generateAndSaveResetCode(String email) {
        // Generate a 6-digit random number as a string (with leading zeros if necessary)
        String code = String.format("%06d", new Random().nextInt(1000000));
        resetCodes.put(email, code);
        return code;
    }

    public boolean verifyResetCode(String email, String code) {
        String savedCode = resetCodes.get(email);
        return savedCode != null && savedCode.equals(code);
    }

    public void removeResetCode(String email) {
        resetCodes.remove(email);
    }
}
