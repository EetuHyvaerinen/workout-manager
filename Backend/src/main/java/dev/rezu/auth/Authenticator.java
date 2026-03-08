package dev.rezu.auth;

import dev.rezu.json.JsonMapper;
import dev.rezu.json.JsonReader;
import dev.rezu.json.JsonNode;
import dev.rezu.logger.AsyncLogger;

import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.time.Instant;
import java.util.Base64;

public class Authenticator {
    private static final AsyncLogger logger = AsyncLogger.getLogger(Authenticator.class);

    private final SecretKeySpec signingKey;
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;

    public record AuthResult(int userId, boolean isAdmin) {}

    public Authenticator() {
        String secretKey = System.getenv("SecretKeyWorkoutHelper");
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        this.signingKey = new SecretKeySpec(keyBytes, HMAC_ALGORITHM);
    }

    public AuthResult verifyJwtFull(String token) {
        if (token == null || token.isEmpty()) return null;

        String[] parts = token.split("\\.");
        if (parts.length != 3) return null;

        try {
            String dataToVerify = parts[0] + "." + parts[1];
            byte[] receivedSignature = Base64.getUrlDecoder().decode(parts[2]);
            byte[] expectedSignature = createHmacSignature(dataToVerify.getBytes(StandardCharsets.UTF_8));

            if (!MessageDigest.isEqual(expectedSignature, receivedSignature)) return null;

            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            JsonNode jsonNode;
            try (StringReader sr = new StringReader(payloadJson)) {
                jsonNode = JsonReader.parse(sr);
            }
            JwtPayload payload = jsonNode.map(JsonMapper::readJwtPayload);

            if (Instant.now().getEpochSecond() > payload.exp()) return null;

            return new AuthResult(Integer.parseInt(payload.sub()), payload.admin());
        } catch (Exception e) {
            return null;
        }
    }

    public String createJwtToken(int userId, boolean isAdmin, long ttlSeconds) {
        try {
            String headerJson = JsonMapper.toJson(new JwtHeader("HS256", "JWT"));
            String encodedHeader = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
            long now = Instant.now().getEpochSecond();
            JwtPayload payload = new JwtPayload(String.valueOf(userId), isAdmin, now, now + ttlSeconds);
            String payloadJson = JsonMapper.toJson(payload);


            String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

            String dataToSign = encodedHeader + "." + encodedPayload;
            byte[] signatureBytes = createHmacSignature(dataToSign.getBytes(StandardCharsets.UTF_8));
            String signature = Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);

            return dataToSign + "." + signature;

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            logger.error("Critical error during JWT creation for user: " + userId, e);
            throw new RuntimeException("Error creating JWT", e);
        }
    }

    public String hashPassword(String password, String salt) {
        try {
            byte[] saltBytes = Base64.getDecoder().decode(salt);

            KeySpec spec = new PBEKeySpec(
                    password.toCharArray(),
                    saltBytes,
                    ITERATIONS,
                    KEY_LENGTH
            );

            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] hash = factory.generateSecret(spec).getEncoded();

            return Base64.getEncoder().encodeToString(hash);

        } catch (Exception e) {
            logger.error("Password hashing failed", e);
            throw new RuntimeException("Error during password hashing.", e);
        }
    }

    public boolean verifyPassword(String password, String salt, String hash) {
        String providedHash = hashPassword(password, salt);
        return MessageDigest.isEqual(
                providedHash.getBytes(StandardCharsets.UTF_8),
                hash.getBytes(StandardCharsets.UTF_8));
    }

    private byte[] createHmacSignature(byte[] data) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(this.signingKey);
        return mac.doFinal(data);
    }

    public static String getNewSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }
}
