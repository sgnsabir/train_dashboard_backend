package com.banenor.util;

/**
 * Utility class for client IP extraction, sanitization, and masking.
 * Provides methods to extract the client IP from common headers (X-Forwarded-For, X-Real-IP)
 * and to sanitize/mask the IP for secure logging.
 */
public final class IPUtils {

    // Private constructor to prevent instantiation
    private IPUtils() {
        throw new UnsupportedOperationException("IPUtils is a utility class and cannot be instantiated.");
    }

    /**
     * Extracts the client IP address based on the following priority:
     * 1. X-Forwarded-For header (first value if multiple exist)
     * 2. X-Real-IP header
     * 3. Fallback to the provided remote address.
     *
     * @param xForwardedFor the value of the X-Forwarded-For header.
     * @param xRealIp the value of the X-Real-IP header.
     * @param remoteAddress the fallback remote address.
     * @return the sanitized client IP address.
     */
    public static String extractClientIP(String xForwardedFor, String xRealIp, String remoteAddress) {
        if (xForwardedFor != null && !xForwardedFor.trim().isEmpty()) {
            // If there are multiple IPs, take the first one.
            String[] parts = xForwardedFor.split(",");
            return sanitizeIP(parts[0].trim());
        }
        if (xRealIp != null && !xRealIp.trim().isEmpty()) {
            return sanitizeIP(xRealIp.trim());
        }
        return sanitizeIP(remoteAddress);
    }

    /**
     * Sanitizes an IP address string by removing any characters that are not valid in an IP address.
     *
     * @param ip the IP address string.
     * @return the sanitized IP address; or "unknown" if input is null or empty.
     */
    public static String sanitizeIP(String ip) {
        if (ip == null || ip.isEmpty()) {
            return "unknown";
        }
        // Allow digits, letters (for IPv6), colons, and dots.
        return ip.replaceAll("[^0-9a-fA-F:.]", "");
    }

    /**
     * Masks an IP address for logging or error reporting.
     * For IPv4 addresses, masks the last two octets.
     * For IPv6 addresses, masks the last segment.
     *
     * @param ip the sanitized IP address.
     * @return the masked IP address.
     */
    public static String maskIP(String ip) {
        if (ip == null || ip.equals("unknown")) {
            return "unknown";
        }
        if (ip.contains(".")) { // IPv4
            String[] parts = ip.split("\\.");
            if (parts.length == 4) {
                return String.format("%s.%s.***.***", parts[0], parts[1]);
            }
        }
        if (ip.contains(":")) { // IPv6
            int lastColon = ip.lastIndexOf(':');
            if (lastColon != -1) {
                return ip.substring(0, lastColon + 1) + "****";
            }
        }
        return "masked";
    }
}
