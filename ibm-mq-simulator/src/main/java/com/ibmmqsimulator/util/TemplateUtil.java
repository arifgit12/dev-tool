package com.ibmmqsimulator.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for replacing dynamic parameters in XML templates.
 * Supports placeholders like ${ID}, ${AMOUNT}, ${NAME}, etc.
 */
public class TemplateUtil {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");
    private static final Random RANDOM = new Random();
    
    // Common first and last names for generating random names
    private static final String[] FIRST_NAMES = {
        "James", "Mary", "John", "Patricia", "Robert", "Jennifer", "Michael", "Linda",
        "William", "Elizabeth", "David", "Barbara", "Richard", "Susan", "Joseph", "Jessica",
        "Thomas", "Sarah", "Charles", "Karen", "Christopher", "Nancy", "Daniel", "Lisa"
    };
    
    private static final String[] LAST_NAMES = {
        "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis",
        "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson", "Thomas",
        "Taylor", "Moore", "Jackson", "Martin", "Lee", "Thompson", "White", "Harris"
    };

    /**
     * Replaces dynamic placeholders in the template with generated values.
     * Supports: ${ID}, ${UUID}, ${NUMBER}, ${AMOUNT}, ${PRICE}, ${NAME}, ${EMAIL}, ${PHONE}, ${DATE}, ${TIMESTAMP}
     * 
     * @param template The XML template with placeholders
     * @return XML with placeholders replaced with generated values
     */
    public static String replacePlaceholders(String template) {
        if (template == null || !template.contains("${")) {
            return template;
        }

        StringBuffer result = new StringBuffer();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        
        while (matcher.find()) {
            String placeholder = matcher.group(1).toUpperCase().trim();
            String replacement = generateValue(placeholder);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        
        return result.toString();
    }

    /**
     * Generates a value based on the placeholder type
     */
    private static String generateValue(String placeholder) {
        switch (placeholder) {
            case "ID":
                return String.valueOf(generateId());
            
            case "UUID":
                return UUID.randomUUID().toString();
            
            case "NUMBER":
                return String.valueOf(RANDOM.nextInt(999999) + 1);
            
            case "AMOUNT":
            case "PRICE":
                return generateAmount();
            
            case "NAME":
                return generateName();
            
            case "FIRSTNAME":
            case "FIRST_NAME":
                return FIRST_NAMES[RANDOM.nextInt(FIRST_NAMES.length)];
            
            case "LASTNAME":
            case "LAST_NAME":
                return LAST_NAMES[RANDOM.nextInt(LAST_NAMES.length)];
            
            case "EMAIL":
                return generateEmail();
            
            case "PHONE":
                return generatePhone();
            
            case "DATE":
                return java.time.LocalDate.now().toString();
            
            case "TIMESTAMP":
                return java.time.LocalDateTime.now().toString();
            
            case "TIME":
                return java.time.LocalTime.now().toString();
            
            case "RANDOM":
                return UUID.randomUUID().toString().substring(0, 8);
            
            default:
                // If placeholder contains a colon, parse as type:format
                if (placeholder.contains(":")) {
                    return generateCustomValue(placeholder);
                }
                // Unknown placeholder - return as-is
                return "${" + placeholder + "}";
        }
    }

    /**
     * Generates a numeric ID (1-999999999)
     */
    private static long generateId() {
        return RANDOM.nextInt(999999999) + 1;
    }

    /**
     * Generates a random amount/price (0.01 - 9999.99)
     */
    private static String generateAmount() {
        double amount = RANDOM.nextDouble() * 9999.99 + 0.01;
        BigDecimal bd = new BigDecimal(amount).setScale(2, RoundingMode.HALF_UP);
        return bd.toString();
    }

    /**
     * Generates a random full name
     */
    private static String generateName() {
        String firstName = FIRST_NAMES[RANDOM.nextInt(FIRST_NAMES.length)];
        String lastName = LAST_NAMES[RANDOM.nextInt(LAST_NAMES.length)];
        return firstName + " " + lastName;
    }

    /**
     * Generates a random email address
     */
    private static String generateEmail() {
        String firstName = FIRST_NAMES[RANDOM.nextInt(FIRST_NAMES.length)].toLowerCase();
        String lastName = LAST_NAMES[RANDOM.nextInt(LAST_NAMES.length)].toLowerCase();
        String[] domains = {"example.com", "test.com", "demo.com", "mail.com"};
        String domain = domains[RANDOM.nextInt(domains.length)];
        return firstName + "." + lastName + "@" + domain;
    }

    /**
     * Generates a random phone number (format: ###-###-####)
     */
    private static String generatePhone() {
        return String.format("%03d-%03d-%04d", 
            RANDOM.nextInt(900) + 100,
            RANDOM.nextInt(900) + 100,
            RANDOM.nextInt(9000) + 1000);
    }

    /**
     * Generates custom values based on format specifiers
     * Examples: NUMBER:1-100, AMOUNT:10-1000, STRING:5 (5 chars)
     */
    private static String generateCustomValue(String placeholder) {
        String[] parts = placeholder.split(":");
        if (parts.length != 2) {
            return "${" + placeholder + "}";
        }

        String type = parts[0].trim();
        String format = parts[1].trim();

        try {
            switch (type) {
                case "NUMBER":
                    return generateNumberInRange(format);
                
                case "AMOUNT":
                    return generateAmountInRange(format);
                
                case "STRING":
                    return generateRandomString(Integer.parseInt(format));
                
                default:
                    return "${" + placeholder + "}";
            }
        } catch (Exception e) {
            return "${" + placeholder + "}";
        }
    }

    /**
     * Generates a number within a specified range (e.g., "1-100")
     */
    private static String generateNumberInRange(String range) {
        String[] bounds = range.split("-");
        if (bounds.length != 2) {
            return String.valueOf(RANDOM.nextInt(100));
        }
        
        int min = Integer.parseInt(bounds[0].trim());
        int max = Integer.parseInt(bounds[1].trim());
        return String.valueOf(RANDOM.nextInt(max - min + 1) + min);
    }

    /**
     * Generates an amount within a specified range (e.g., "10-1000")
     */
    private static String generateAmountInRange(String range) {
        String[] bounds = range.split("-");
        if (bounds.length != 2) {
            return generateAmount();
        }
        
        double min = Double.parseDouble(bounds[0].trim());
        double max = Double.parseDouble(bounds[1].trim());
        double amount = RANDOM.nextDouble() * (max - min) + min;
        BigDecimal bd = new BigDecimal(amount).setScale(2, RoundingMode.HALF_UP);
        return bd.toString();
    }

    /**
     * Generates a random alphanumeric string of specified length
     */
    private static String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * Checks if a template contains any dynamic placeholders
     */
    public static boolean hasPlaceholders(String template) {
        return template != null && template.contains("${");
    }
}
