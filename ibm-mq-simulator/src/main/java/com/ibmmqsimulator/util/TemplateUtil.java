package com.ibmmqsimulator.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for replacing dynamic parameters in XML templates.
 * Supports placeholders like ${ID}, ${AMOUNT}, ${NAME}, etc.
 */
public class TemplateUtil {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");
    
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
                return String.valueOf(ThreadLocalRandom.current().nextInt(999999) + 1);
            
            case "AMOUNT":
            case "PRICE":
                return generateAmount();
            
            case "NAME":
                return generateName();
            
            case "FIRSTNAME":
            case "FIRST_NAME":
                return FIRST_NAMES[ThreadLocalRandom.current().nextInt(FIRST_NAMES.length)];
            
            case "LASTNAME":
            case "LAST_NAME":
                return LAST_NAMES[ThreadLocalRandom.current().nextInt(LAST_NAMES.length)];
            
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
        return ThreadLocalRandom.current().nextInt(999999999) + 1;
    }

    /**
     * Generates a random amount/price (0.01 - 9999.99)
     */
    private static String generateAmount() {
        double amount = ThreadLocalRandom.current().nextDouble() * 9999.99 + 0.01;
        BigDecimal bd = new BigDecimal(amount).setScale(2, RoundingMode.HALF_UP);
        return bd.toString();
    }

    /**
     * Generates a random full name
     */
    private static String generateName() {
        String firstName = FIRST_NAMES[ThreadLocalRandom.current().nextInt(FIRST_NAMES.length)];
        String lastName = LAST_NAMES[ThreadLocalRandom.current().nextInt(LAST_NAMES.length)];
        return firstName + " " + lastName;
    }

    /**
     * Generates a random email address
     */
    private static String generateEmail() {
        String firstName = FIRST_NAMES[ThreadLocalRandom.current().nextInt(FIRST_NAMES.length)].toLowerCase();
        String lastName = LAST_NAMES[ThreadLocalRandom.current().nextInt(LAST_NAMES.length)].toLowerCase();
        String[] domains = {"example.com", "test.com", "demo.com", "mail.com"};
        String domain = domains[ThreadLocalRandom.current().nextInt(domains.length)];
        return firstName + "." + lastName + "@" + domain;
    }

    /**
     * Generates a random phone number (format: ###-###-####)
     */
    private static String generatePhone() {
        return String.format("%03d-%03d-%04d", 
            ThreadLocalRandom.current().nextInt(900) + 100,
            ThreadLocalRandom.current().nextInt(900) + 100,
            ThreadLocalRandom.current().nextInt(9000) + 1000);
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
            return String.valueOf(ThreadLocalRandom.current().nextInt(100));
        }
        
        try {
            int min = Integer.parseInt(bounds[0].trim());
            int max = Integer.parseInt(bounds[1].trim());
            
            // Validate min <= max
            if (min > max) {
                int temp = min;
                min = max;
                max = temp;
            }
            
            return String.valueOf(ThreadLocalRandom.current().nextInt(max - min + 1) + min);
        } catch (NumberFormatException e) {
            return String.valueOf(ThreadLocalRandom.current().nextInt(100));
        }
    }

    /**
     * Generates an amount within a specified range (e.g., "10-1000")
     */
    private static String generateAmountInRange(String range) {
        String[] bounds = range.split("-");
        if (bounds.length != 2) {
            return generateAmount();
        }
        
        try {
            double min = Double.parseDouble(bounds[0].trim());
            double max = Double.parseDouble(bounds[1].trim());
            
            // Validate min <= max
            if (min > max) {
                double temp = min;
                min = max;
                max = temp;
            }
            
            double amount = ThreadLocalRandom.current().nextDouble() * (max - min) + min;
            BigDecimal bd = new BigDecimal(amount).setScale(2, RoundingMode.HALF_UP);
            return bd.toString();
        } catch (NumberFormatException e) {
            return generateAmount();
        }
    }

    /**
     * Generates a random alphanumeric string of specified length
     */
    private static String generateRandomString(int length) {
        // Validate length
        if (length <= 0) {
            return "";
        }
        if (length > 1000) {
            length = 1000; // Cap at reasonable length
        }
        
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(ThreadLocalRandom.current().nextInt(chars.length())));
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
