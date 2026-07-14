package com.assessment.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Component
public class HmacTokenValidator {

    private final String secret;

    public HmacTokenValidator(@Value("${assessment.security.hmac-secret}") String secret) {
        this.secret = secret;
    }

    public String generateToken(String employeeId) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(employeeId.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to generate HMAC token", e);
        }
    }

    public boolean validateToken(String employeeId, String token) {
        try {
            String expected = generateToken(employeeId);
            return expected.equals(token);
        } catch (Exception e) {
            return false;
        }
    }
}
