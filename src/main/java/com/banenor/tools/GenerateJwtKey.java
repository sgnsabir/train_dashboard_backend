// src/test/java/com/banenor/tools/GenerateJwtKey.java
package com.banenor.tools;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;

/**
 * Utility to generate a Base64-encoded JWT secret key suitable for HS512.
 *
 * Usage:
 *   mvn test-compile &&
 *   mvn exec:java \
 *     -Dexec.mainClass="com.banenor.tools.GenerateJwtKey"
 *
 * Copy the printed string into your application.yml as:
 *   jwt:
 *     secret: <PASTE_BASE64_STRING>
 */
public class GenerateJwtKey {

    public static void main(String[] args) {
        // Generate a secure random key for HS512
        SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS512);

        // Base64-encode it in the same way JwtTokenUtil will decode it
        String base64Key = Encoders.BASE64.encode(key.getEncoded());

        System.out.println();
        System.out.println("-------------------------------------------------");
        System.out.println("Generated JWT Secret Key (Base64, HS512):");
        System.out.println();
        System.out.println(base64Key);
        System.out.println("-------------------------------------------------");
        System.out.println();
        System.out.println("Paste that value into your `jwt.secret` property.");
    }
}
