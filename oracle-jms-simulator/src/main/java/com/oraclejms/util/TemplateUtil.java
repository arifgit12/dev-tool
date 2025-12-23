package com.oraclejms.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for processing XML templates with dynamic parameters.
 * Supports placeholder substitution for generating unique values in each message.
 */
public class TemplateUtil {

    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\$\\{([^}]+)}");
    private static final Random random = new Random();
    
    // Common first and last names for realistic data generation
    private static final String[] FIRST_NAMES = {
        "James", "Mary", "John", "Patricia", "Robert", "Jennifer", "Michael", "Linda",
        "William", "Barbara", "David", "Elizabeth", "Richard", "Susan", "Joseph", "Jessica",
        "Thomas", "Sarah", "Charles", "Karen", "Christopher", "Nancy", "Daniel", "Lisa",
        "Matthew", "Betty", "Anthony", "Margaret", "Mark", "Sandra", "Donald", "Ashley"
    };
    
    private static final String[] LAST_NAMES = {
        "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis",
        "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson", "Thomas",
        "Taylor", "Moore", "Jackson", "Martin", "Lee", "Thompson", "White", "Harris",
        "Sanchez", "Clark", "Ramirez", "Lewis", "Robinson", "Walker", "Young", "Allen"
    };

    private TemplateUtil() {
        // Utility class
    }

    /**
     * Checks if the XML contains template variables
     */
    public static boolean hasTemplateVariables(String xml) {
        if (xml == null || xml.trim().isEmpty()) {
            return false;
        }
        Matcher matcher = TEMPLATE_PATTERN.matcher(xml);
        return matcher.find();
    }

    /**
     * Processes the XML template and substitutes all placeholders with generated values
     */
    public static String processTemplate(String template) {
        if (template == null) {
            return null;
        }

        StringBuffer result = new StringBuffer();
        Matcher matcher = TEMPLATE_PATTERN.matcher(template);

        while (matcher.find()) {
            String placeholder = matcher.group(1);
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
        // Handle custom format placeholders
        if (placeholder.contains(":")) {
            String[] parts = placeholder.split(":", 2);
            String type = parts[0].trim();
            String param = parts[1].trim();
            
            switch (type.toUpperCase()) {
                case "NUMBER":
                    return generateNumberInRange(param);
                case "AMOUNT":
                    return generateAmountInRange(param);
                case "STRING":
                    return generateRandomString(param);
                default:
                    return "${" + placeholder + "}"; // Return as-is if unknown
            }
        }

        // Handle standard placeholders
        switch (placeholder.toUpperCase()) {
            case "ID":
                return String.valueOf(ThreadLocalRandom.current().nextInt(1, 1000000000));
            
            case "UUID":
                return UUID.randomUUID().toString();
            
            case "NUMBER":
                return String.valueOf(ThreadLocalRandom.current().nextInt(1, 1000000));
            
            case "RANDOM":
                return generateRandomString("8");
            
            case "AMOUNT":
            case "PRICE":
                return String.format("%.2f", ThreadLocalRandom.current().nextDouble(0.01, 10000.00));
            
            case "NAME":
                return generateFullName();
            
            case "FIRSTNAME":
            case "FIRST_NAME":
                return generateFirstName();
            
            case "LASTNAME":
            case "LAST_NAME":
                return generateLastName();
            
            case "EMAIL":
                return generateEmail();
            
            case "PHONE":
                return generatePhone();
            
            case "DATE":
                return LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            
            case "TIME":
                return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
            
            case "TIMESTAMP":
                return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"));
            
            default:
                return "${" + placeholder + "}"; // Return as-is if unknown
        }
    }

    /**
     * Generates a number within the specified range (format: "min-max")
     */
    private static String generateNumberInRange(String param) {
        try {
            String[] range = param.split("-");
            if (range.length == 2) {
                int min = Integer.parseInt(range[0].trim());
                int max = Integer.parseInt(range[1].trim());
                return String.valueOf(ThreadLocalRandom.current().nextInt(min, max + 1));
            }
        } catch (NumberFormatException e) {
            // Invalid format, return as-is
        }
        return "${NUMBER:" + param + "}";
    }

    /**
     * Generates a decimal amount within the specified range (format: "min-max")
     */
    private static String generateAmountInRange(String param) {
        try {
            String[] range = param.split("-");
            if (range.length == 2) {
                double min = Double.parseDouble(range[0].trim());
                double max = Double.parseDouble(range[1].trim());
                return String.format("%.2f", ThreadLocalRandom.current().nextDouble(min, max));
            }
        } catch (NumberFormatException e) {
            // Invalid format, return as-is
        }
        return "${AMOUNT:" + param + "}";
    }

    /**
     * Generates a random alphanumeric string of specified length
     */
    private static String generateRandomString(String lengthParam) {
        try {
            int length = Integer.parseInt(lengthParam.trim());
            String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
            StringBuilder sb = new StringBuilder(length);
            for (int i = 0; i < length; i++) {
                sb.append(chars.charAt(random.nextInt(chars.length())));
            }
            return sb.toString();
        } catch (NumberFormatException e) {
            return "${STRING:" + lengthParam + "}";
        }
    }

    /**
     * Generates a random full name
     */
    private static String generateFullName() {
        return generateFirstName() + " " + generateLastName();
    }

    /**
     * Generates a random first name
     */
    private static String generateFirstName() {
        return FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
    }

    /**
     * Generates a random last name
     */
    private static String generateLastName() {
        return LAST_NAMES[random.nextInt(LAST_NAMES.length)];
    }

    /**
     * Generates a random email address
     */
    private static String generateEmail() {
        String firstName = generateFirstName().toLowerCase();
        String lastName = generateLastName().toLowerCase();
        String[] domains = {"example.com", "test.com", "demo.com", "sample.com"};
        String domain = domains[random.nextInt(domains.length)];
        return firstName + "." + lastName + "@" + domain;
    }

    /**
     * Generates a random phone number in format ###-###-####
     */
    private static String generatePhone() {
        return String.format("%03d-%03d-%04d",
            random.nextInt(1000),
            random.nextInt(1000),
            random.nextInt(10000));
    }
}
