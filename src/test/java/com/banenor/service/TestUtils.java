package com.banenor.service;

import java.lang.reflect.Field;

public class TestUtils {

    /**
     * Recursively searches for a declared field in the class hierarchy and sets its value.
     *
     * @param target    The object whose field should be set.
     * @param fieldName The name of the field.
     * @param value     The value to set.
     */
    public static void setField(Object target, String fieldName, Object value) {
        Field field = getField(target.getClass(), fieldName);
        if (field == null) {
            throw new RuntimeException("Field '" + fieldName + "' not found in " + target.getClass());
        }
        field.setAccessible(true);
        try {
            field.set(target, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to set field '" + fieldName + "' on " + target.getClass(), e);
        }
    }

    /**
     * Recursively searches the class hierarchy for the declared field.
     *
     * @param clazz     The class to start searching in.
     * @param fieldName The name of the field.
     * @return The Field if found; otherwise, null.
     */
    private static Field getField(Class<?> clazz, String fieldName) {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                // Search in superclass
                clazz = clazz.getSuperclass();
            }
        }
        return null;
    }
}
