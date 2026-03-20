package com.isp.lg.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CaptchaService {
    private static final long EXPIRE_MS = 2 * 60 * 1000L;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Challenge> store = new ConcurrentHashMap<>();

    public CaptchaChallenge issue(String sourceIp) {
        cleanupExpired();
        int a = random.nextInt(10, 100);
        int b = random.nextInt(1, 10);
        String id = UUID.randomUUID().toString().replace("-", "");
        String question = a + " + " + b + " = ?";
        store.put(id, new Challenge(sourceIp, String.valueOf(a + b), System.currentTimeMillis()));
        return new CaptchaChallenge(id, question);
    }

    public boolean validateAndConsume(String token, String sourceIp) {
        cleanupExpired();
        if (token == null || token.isBlank()) return false;
        String[] parts = token.split(":", 2);
        if (parts.length != 2) return false;
        String id = parts[0].trim();
        String answer = parts[1].trim();
        if (id.isEmpty() || answer.isEmpty()) return false;

        Challenge challenge = store.remove(id);
        if (challenge == null) return false;
        if (System.currentTimeMillis() - challenge.issuedAtMs > EXPIRE_MS) return false;
        if (challenge.sourceIp != null && sourceIp != null && !challenge.sourceIp.equals(sourceIp)) return false;
        return challenge.answer.equals(answer);
    }

    private void cleanupExpired() {
        long now = System.currentTimeMillis();
        store.entrySet().removeIf(e -> now - e.getValue().issuedAtMs > EXPIRE_MS);
    }

    private static class Challenge {
        final String sourceIp;
        final String answer;
        final long issuedAtMs;

        private Challenge(String sourceIp, String answer, long issuedAtMs) {
            this.sourceIp = sourceIp;
            this.answer = answer;
            this.issuedAtMs = issuedAtMs;
        }
    }

    public record CaptchaChallenge(String captchaId, String question) {}
}
