package com.banenor.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;


public final class DateTimeUtils {

    // Prevent instantiation
    private DateTimeUtils() {
        throw new UnsupportedOperationException("DateTimeUtils is a utility class and cannot be instantiated.");
    }

    public static LocalDateTime parseOrDefault(String dateTimeStr, LocalDateTime defaultValue) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            // Parse using the standard ISO_DATE_TIME formatter.
            return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_DATE_TIME);
        } catch (DateTimeParseException ex) {
            // For production code, you might want to log this exception at a debug level.
            return defaultValue;
        }
    }
}
