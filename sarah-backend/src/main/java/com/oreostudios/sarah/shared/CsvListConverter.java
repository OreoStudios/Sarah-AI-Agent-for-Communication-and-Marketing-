package com.oreostudios.sarah.shared; // Shared, dependency-free building blocks.

import jakarta.persistence.AttributeConverter; // SPI for custom column conversion.
import jakarta.persistence.Converter; // Marks a JPA attribute converter.

import java.util.Arrays; // Splitting helper.
import java.util.List; // Converted attribute type.

/** Persists a {@code List<String>} as a comma-separated column value. */
@Converter // JPA attribute converter.
public class CsvListConverter implements AttributeConverter<List<String>, String> {

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        if (attribute == null || attribute.isEmpty()) { // Nothing to persist.
            return ""; // Store an empty string.
        }
        return String.join(",", attribute); // Join with commas.
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) { // Empty column.
            return List.of(); // Return an empty list.
        }
        return Arrays.stream(dbData.split(",")) // Split on commas.
                .map(String::strip) // Trim each token.
                .filter(s -> !s.isEmpty()) // Drop empties.
                .toList(); // Collect to an immutable list.
    }
}
