// src/test/java/com/banenor/tools/GenerateJwtKey.java
package com.banenor.tools;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;

public class GenerateJwtKey {

    public static void main(String[] args) {
        Date now = new Date();
        Date expires = new Date(now.getTime() + 36000 * 1000);
        System.out.println("Now: " + now);
        System.out.println("Expire: " + expires);
        // Generate a secure random key for HS256
        SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS256);

        // Base64-encode it in the same way JwtTokenUtil will decode it
        String base64Key = Encoders.BASE64.encode(key.getEncoded());

        System.out.println();
        System.out.println("-------------------------------------------------");
        System.out.println("Generated JWT Secret Key (Base64, HS256):");
        System.out.println();
        System.out.println(base64Key);
        System.out.println("-------------------------------------------------");
        System.out.println();
        System.out.println("Paste that value into your `jwt.secret` property.");
    }
}
