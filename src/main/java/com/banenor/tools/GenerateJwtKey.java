// src/test/java/com/banenor/tools/GenerateJwtKey.java
package com.banenor.tools;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Base64;

/**
 * Utility to generate a Base64‑encoded JWT secret key.
 * Run as: mvn test-compile && mvn exec:java \
 *   -Dexec.mainClass="com.banenor.tools.GenerateJwtKey"
 */
public class GenerateJwtKey {

    public static void main(String[] args) {
        // Generate a strong HS512 key
        SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS512);
        String base64Key = Base64.getEncoder().encodeToString(key.getEncoded());

        System.out.println("Generated JWT Secret Key (Base64):");
        System.out.println(base64Key);
    }
}
