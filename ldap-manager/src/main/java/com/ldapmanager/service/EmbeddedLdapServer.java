package com.ldapmanager.service;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldif.LDIFReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

@Service
public class EmbeddedLdapServer {
    private static final Logger logger = LoggerFactory.getLogger(EmbeddedLdapServer.class);

    private InMemoryDirectoryServer server;
    private int port = 10389;
    private String baseDN = "dc=example,dc=com";
    private boolean running = false;

    public void start(int port, String baseDN, String adminPassword) throws Exception {
        logger.info("╔════════════════════════════════════════════════════════════════╗");
        logger.info("║          STARTING EMBEDDED LDAP SERVER                         ║");
        logger.info("╚════════════════════════════════════════════════════════════════╝");

        if (running) {
            throw new IllegalStateException("Server is already running");
        }

        logger.info("Configuration:");
        logger.info("  Port: {}", port);
        logger.info("  Base DN: {}", baseDN);
        logger.info("  Admin DN: cn=admin,{}", baseDN);

        this.port = port;
        this.baseDN = baseDN;

        logger.info("Step 1: Creating InMemoryDirectoryServerConfig...");
        InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig(baseDN);

        logger.info("Step 2: Configuring listener on port {}...", port);
        config.setListenerConfigs(InMemoryListenerConfig.createLDAPConfig("default", port));

        logger.info("Step 3: Setting up admin credentials...");
        config.addAdditionalBindCredentials("cn=admin," + baseDN, adminPassword);

        logger.info("Step 4: Creating InMemoryDirectoryServer instance...");
        server = new InMemoryDirectoryServer(config);
        logger.info("  Server instance created successfully");

        // Add the base DN entry
        // Extract the leftmost dc component (e.g., "dc=local" -> "local")
        String dcValue = baseDN.split(",")[0].substring(3);

        logger.info("╔════════════════════════════════════════════════════════════════╗");
        logger.info("║          STEP 5: ADDING BASE DN ENTRY                          ║");
        logger.info("╚════════════════════════════════════════════════════════════════╝");
        logger.info("  Base DN: {}", baseDN);
        logger.info("  DC value: {}", dcValue);
        logger.info("  Entry to add:");
        logger.info("    dn: {}", baseDN);
        logger.info("    objectClass: top");
        logger.info("    objectClass: domain");
        logger.info("    dc: {}", dcValue);

        try {
            server.add(
                "dn: " + baseDN,
                "objectClass: top",
                "objectClass: domain",
                "dc: " + dcValue
            );
            logger.info("✓ Base DN entry added successfully");

            // Verify it was added
            logger.info("Step 6: Verifying base DN entry exists...");
            if (server.getEntry(baseDN) != null) {
                logger.info("✓ VERIFIED: Base DN entry EXISTS in server");
                logger.info("  Entry DN: {}", server.getEntry(baseDN).getDN());
            } else {
                logger.error("✗ CRITICAL ERROR: Base DN entry was NOT found after adding!");
            }
        } catch (Exception e) {
            logger.error("✗ FAILED to add base DN entry", e);
            throw e;
        }

        logger.info("Step 7: Starting listener on port {}...", port);
        server.startListening();
        running = true;

        logger.info("╔════════════════════════════════════════════════════════════════╗");
        logger.info("║       EMBEDDED LDAP SERVER STARTED SUCCESSFULLY                ║");
        logger.info("╚════════════════════════════════════════════════════════════════╝");
        logger.info("  Listen Address: localhost:{}", port);
        logger.info("  Base DN: {}", baseDN);
        logger.info("  Admin DN: cn=admin,{}", baseDN);
        logger.info("  Protocol: LDAP (non-SSL)");
        logger.info("  Connection String: ldap://localhost:{}", port);
    }

    public void stop() throws LDAPException {
        if (!running || server == null) {
            throw new IllegalStateException("Server is not running");
        }

        server.shutDown(true);
        running = false;
        logger.info("Embedded LDAP server stopped");
    }

    public void addSampleData() throws Exception {
        if (!running) {
            throw new IllegalStateException("Server is not running");
        }

        try {
            logger.info("╔════════════════════════════════════════════════════════════════╗");
            logger.info("║          ADDING SAMPLE DATA TO LDAP SERVER                     ║");
            logger.info("╚════════════════════════════════════════════════════════════════╝");
            logger.info("Base DN: {}", baseDN);

            // Add organizational units
            logger.info("Step 1: Adding ou=users...");
            server.add(
                "dn: ou=users," + baseDN,
                "objectClass: top",
                "objectClass: organizationalUnit",
                "ou: users"
            );
            logger.info("✓ Added: ou=users," + baseDN);

            logger.info("Step 2: Adding ou=groups...");
            server.add(
                "dn: ou=groups," + baseDN,
                "objectClass: top",
                "objectClass: organizationalUnit",
                "ou: groups"
            );
            logger.info("✓ Added: ou=groups," + baseDN);

            // Add sample users
            logger.info("Step 3: Adding 5 sample users...");
            addUser("John Doe", "Doe", "John", "jdoe", "john.doe@example.com",
                    "+1-555-0101", "1001", "Software Engineer", "IT");
            addUser("Jane Smith", "Smith", "Jane", "jsmith", "jane.smith@example.com",
                    "+1-555-0102", "1002", "Project Manager", "Management");
            addUser("Bob Wilson", "Wilson", "Bob", "bwilson", "bob.wilson@example.com",
                    "+1-555-0103", "1003", "Database Administrator", "IT");
            addUser("Alice Brown", "Brown", "Alice", "abrown", "alice.brown@example.com",
                    "+1-555-0104", "1004", "Business Analyst", "Business");
            addUser("Charlie Davis", "Davis", "Charlie", "cdavis", "charlie.davis@example.com",
                    "+1-555-0105", "1005", "DevOps Engineer", "IT");

            // Add sample groups
            logger.info("Step 4: Adding 4 sample groups...");
            addGroup("Developers", "Development Team",
                    "cn=John Doe,ou=users," + baseDN,
                    "cn=Bob Wilson,ou=users," + baseDN,
                    "cn=Charlie Davis,ou=users," + baseDN);
            addGroup("Managers", "Management Team",
                    "cn=Jane Smith,ou=users," + baseDN);
            addGroup("IT Department", "IT Department Staff",
                    "cn=John Doe,ou=users," + baseDN,
                    "cn=Bob Wilson,ou=users," + baseDN,
                    "cn=Charlie Davis,ou=users," + baseDN);
            addGroup("All Employees", "All Company Employees",
                    "cn=John Doe,ou=users," + baseDN,
                    "cn=Jane Smith,ou=users," + baseDN,
                    "cn=Bob Wilson,ou=users," + baseDN,
                    "cn=Alice Brown,ou=users," + baseDN,
                    "cn=Charlie Davis,ou=users," + baseDN);

            logger.info("╔════════════════════════════════════════════════════════════════╗");
            logger.info("║       SAMPLE DATA ADDED SUCCESSFULLY                           ║");
            logger.info("╚════════════════════════════════════════════════════════════════╝");
            logger.info("  Added: 2 organizational units");
            logger.info("  Added: 5 users");
            logger.info("  Added: 4 groups");
        } catch (Exception e) {
            logger.error("╔════════════════════════════════════════════════════════════════╗");
            logger.error("║       FAILED TO ADD SAMPLE DATA                                ║");
            logger.error("╚════════════════════════════════════════════════════════════════╝");
            logger.error("Error type: {}", e.getClass().getName());
            logger.error("Error message: {}", e.getMessage());
            logger.error("Stack trace:", e);
            throw new Exception("Failed to add sample data: " + e.getMessage(), e);
        }
    }

    private String generateSampleLDIF() {
        // Build LDIF string directly without format specifiers
        StringBuilder ldif = new StringBuilder();

        // Organizational Units
        ldif.append("dn: ou=users,").append(baseDN).append("\n");
        ldif.append("objectClass: top\n");
        ldif.append("objectClass: organizationalUnit\n");
        ldif.append("ou: users\n\n");

        ldif.append("dn: ou=groups,").append(baseDN).append("\n");
        ldif.append("objectClass: top\n");
        ldif.append("objectClass: organizationalUnit\n");
        ldif.append("ou: groups\n\n");

        // Sample Users
        ldif.append("dn: cn=John Doe,ou=users,").append(baseDN).append("\n");
        ldif.append("objectClass: top\n");
        ldif.append("objectClass: person\n");
        ldif.append("objectClass: organizationalPerson\n");
        ldif.append("objectClass: inetOrgPerson\n");
        ldif.append("cn: John Doe\n");
        ldif.append("sn: Doe\n");
        ldif.append("givenName: John\n");
        ldif.append("uid: jdoe\n");
        ldif.append("mail: john.doe@example.com\n");
        ldif.append("userPassword: password123\n");
        ldif.append("telephoneNumber: +1-555-0101\n");
        ldif.append("mobile: +1-555-0101\n");
        ldif.append("employeeNumber: 1001\n");
        ldif.append("title: Software Engineer\n");
        ldif.append("departmentNumber: IT\n");
        ldif.append("description: Senior Software Engineer\n\n");

        ldif.append("dn: cn=Jane Smith,ou=users,").append(baseDN).append("\n");
        ldif.append("objectClass: top\n");
        ldif.append("objectClass: person\n");
        ldif.append("objectClass: organizationalPerson\n");
        ldif.append("objectClass: inetOrgPerson\n");
        ldif.append("cn: Jane Smith\n");
        ldif.append("sn: Smith\n");
        ldif.append("givenName: Jane\n");
        ldif.append("uid: jsmith\n");
        ldif.append("mail: jane.smith@example.com\n");
        ldif.append("userPassword: password123\n");
        ldif.append("telephoneNumber: +1-555-0102\n");
        ldif.append("mobile: +1-555-0102\n");
        ldif.append("employeeNumber: 1002\n");
        ldif.append("title: Project Manager\n");
        ldif.append("departmentNumber: Management\n");
        ldif.append("description: Senior Project Manager\n\n");

        ldif.append("dn: cn=Bob Wilson,ou=users,").append(baseDN).append("\n");
        ldif.append("objectClass: top\n");
        ldif.append("objectClass: person\n");
        ldif.append("objectClass: organizationalPerson\n");
        ldif.append("objectClass: inetOrgPerson\n");
        ldif.append("cn: Bob Wilson\n");
        ldif.append("sn: Wilson\n");
        ldif.append("givenName: Bob\n");
        ldif.append("uid: bwilson\n");
        ldif.append("mail: bob.wilson@example.com\n");
        ldif.append("userPassword: password123\n");
        ldif.append("telephoneNumber: +1-555-0103\n");
        ldif.append("mobile: +1-555-0103\n");
        ldif.append("employeeNumber: 1003\n");
        ldif.append("title: Database Administrator\n");
        ldif.append("departmentNumber: IT\n");
        ldif.append("description: Senior DBA\n\n");

        ldif.append("dn: cn=Alice Brown,ou=users,").append(baseDN).append("\n");
        ldif.append("objectClass: top\n");
        ldif.append("objectClass: person\n");
        ldif.append("objectClass: organizationalPerson\n");
        ldif.append("objectClass: inetOrgPerson\n");
        ldif.append("cn: Alice Brown\n");
        ldif.append("sn: Brown\n");
        ldif.append("givenName: Alice\n");
        ldif.append("uid: abrown\n");
        ldif.append("mail: alice.brown@example.com\n");
        ldif.append("userPassword: password123\n");
        ldif.append("telephoneNumber: +1-555-0104\n");
        ldif.append("mobile: +1-555-0104\n");
        ldif.append("employeeNumber: 1004\n");
        ldif.append("title: Business Analyst\n");
        ldif.append("departmentNumber: Business\n");
        ldif.append("description: Senior Business Analyst\n\n");

        ldif.append("dn: cn=Charlie Davis,ou=users,").append(baseDN).append("\n");
        ldif.append("objectClass: top\n");
        ldif.append("objectClass: person\n");
        ldif.append("objectClass: organizationalPerson\n");
        ldif.append("objectClass: inetOrgPerson\n");
        ldif.append("cn: Charlie Davis\n");
        ldif.append("sn: Davis\n");
        ldif.append("givenName: Charlie\n");
        ldif.append("uid: cdavis\n");
        ldif.append("mail: charlie.davis@example.com\n");
        ldif.append("userPassword: password123\n");
        ldif.append("telephoneNumber: +1-555-0105\n");
        ldif.append("mobile: +1-555-0105\n");
        ldif.append("employeeNumber: 1005\n");
        ldif.append("title: DevOps Engineer\n");
        ldif.append("departmentNumber: IT\n");
        ldif.append("description: Senior DevOps Engineer\n\n");

        // Sample Groups
        ldif.append("dn: cn=Developers,ou=groups,").append(baseDN).append("\n");
        ldif.append("objectClass: top\n");
        ldif.append("objectClass: groupOfNames\n");
        ldif.append("cn: Developers\n");
        ldif.append("description: Development Team\n");
        ldif.append("member: cn=John Doe,ou=users,").append(baseDN).append("\n");
        ldif.append("member: cn=Bob Wilson,ou=users,").append(baseDN).append("\n");
        ldif.append("member: cn=Charlie Davis,ou=users,").append(baseDN).append("\n\n");

        ldif.append("dn: cn=Managers,ou=groups,").append(baseDN).append("\n");
        ldif.append("objectClass: top\n");
        ldif.append("objectClass: groupOfNames\n");
        ldif.append("cn: Managers\n");
        ldif.append("description: Management Team\n");
        ldif.append("member: cn=Jane Smith,ou=users,").append(baseDN).append("\n\n");

        ldif.append("dn: cn=IT Department,ou=groups,").append(baseDN).append("\n");
        ldif.append("objectClass: top\n");
        ldif.append("objectClass: groupOfNames\n");
        ldif.append("cn: IT Department\n");
        ldif.append("description: IT Department Staff\n");
        ldif.append("member: cn=John Doe,ou=users,").append(baseDN).append("\n");
        ldif.append("member: cn=Bob Wilson,ou=users,").append(baseDN).append("\n");
        ldif.append("member: cn=Charlie Davis,ou=users,").append(baseDN).append("\n\n");

        ldif.append("dn: cn=All Employees,ou=groups,").append(baseDN).append("\n");
        ldif.append("objectClass: top\n");
        ldif.append("objectClass: groupOfNames\n");
        ldif.append("cn: All Employees\n");
        ldif.append("description: All Company Employees\n");
        ldif.append("member: cn=John Doe,ou=users,").append(baseDN).append("\n");
        ldif.append("member: cn=Jane Smith,ou=users,").append(baseDN).append("\n");
        ldif.append("member: cn=Bob Wilson,ou=users,").append(baseDN).append("\n");
        ldif.append("member: cn=Alice Brown,ou=users,").append(baseDN).append("\n");
        ldif.append("member: cn=Charlie Davis,ou=users,").append(baseDN).append("\n");

        logger.debug("Generated LDIF:\n{}", ldif.toString());
        return ldif.toString();
    }

    private void addUser(String cn, String sn, String givenName, String uid, String mail,
                         String mobile, String employeeNumber, String title, String department) throws Exception {
        logger.debug("  Adding user: {}", cn);
        server.add(
            "dn: cn=" + cn + ",ou=users," + baseDN,
            "objectClass: top",
            "objectClass: person",
            "objectClass: organizationalPerson",
            "objectClass: inetOrgPerson",
            "cn: " + cn,
            "sn: " + sn,
            "givenName: " + givenName,
            "uid: " + uid,
            "mail: " + mail,
            "userPassword: password123",
            "telephoneNumber: " + mobile,
            "mobile: " + mobile,
            "employeeNumber: " + employeeNumber,
            "title: " + title,
            "departmentNumber: " + department,
            "description: Senior " + title
        );
        logger.info("  ✓ Added user: {}", uid);
    }

    private void addGroup(String cn, String description, String... members) throws Exception {
        logger.debug("  Adding group: {}", cn);

        // Build the group entry with members
        java.util.List<String> attributes = new java.util.ArrayList<>();
        attributes.add("dn: cn=" + cn + ",ou=groups," + baseDN);
        attributes.add("objectClass: top");
        attributes.add("objectClass: groupOfNames");
        attributes.add("cn: " + cn);
        attributes.add("description: " + description);
        for (String member : members) {
            attributes.add("member: " + member);
        }

        server.add(attributes.toArray(new String[0]));
        logger.info("  ✓ Added group: {} ({} members)", cn, members.length);
    }

    public void addCustomEntry(String ldifEntry) throws Exception {
        if (!running) {
            throw new IllegalStateException("Server is not running");
        }

        try {
            logger.info("Attempting to add custom LDIF entry");
            logger.debug("LDIF Entry:\n{}", ldifEntry);

            // Parse LDIF and add entry directly using server.add()
            LDIFReader ldifReader = new LDIFReader(
                new ByteArrayInputStream(ldifEntry.getBytes(StandardCharsets.UTF_8))
            );

            // Read the entry from LDIF
            com.unboundid.ldif.LDIFRecord ldifRecord = ldifReader.readLDIFRecord();
            ldifReader.close();

            if (ldifRecord == null) {
                throw new Exception("No valid LDIF entry found");
            }

            // Convert to Entry and add directly
            if (ldifRecord instanceof com.unboundid.ldif.LDIFAddChangeRecord) {
                com.unboundid.ldif.LDIFAddChangeRecord addRecord =
                    (com.unboundid.ldif.LDIFAddChangeRecord) ldifRecord;

                logger.debug("Adding entry: {}", addRecord.getDN());
                server.add(addRecord.toAddRequest().toEntry());
                logger.info("✓ Custom entry added successfully: {}", addRecord.getDN());
            } else if (ldifRecord instanceof com.unboundid.ldap.sdk.Entry) {
                com.unboundid.ldap.sdk.Entry entry = (com.unboundid.ldap.sdk.Entry) ldifRecord;

                logger.debug("Adding entry: {}", entry.getDN());
                server.add(entry);
                logger.info("✓ Custom entry added successfully: {}", entry.getDN());
            } else {
                throw new Exception("Unsupported LDIF record type: " + ldifRecord.getClass().getName());
            }
        } catch (Exception e) {
            logger.error("✗ Failed to add custom entry", e);
            logger.error("LDIF content that failed:\n{}", ldifEntry);
            throw new Exception("Failed to parse or add LDIF entry: " + e.getMessage() +
                              "\n\nPlease check:\n" +
                              "1. DN format is correct (e.g., cn=Name,ou=users," + baseDN + ")\n" +
                              "2. All required fields are present (dn, objectClass, cn, sn)\n" +
                              "3. No extra spaces or special characters\n" +
                              "4. Organizational units (ou=users, ou=groups) exist\n\n" +
                              "Original error: " + e.getMessage(), e);
        }
    }

    public void clearAllData() throws Exception {
        if (!running) {
            throw new IllegalStateException("Server is not running");
        }

        server.clear();

        // Re-add the base DN entry
        String dcValue = baseDN.split(",")[0].substring(3);

        server.add(
            "dn: " + baseDN,
            "objectClass: top",
            "objectClass: domain",
            "dc: " + dcValue
        );

        logger.info("All data cleared from LDAP server");
    }

    public boolean isRunning() {
        return running;
    }

    public int getPort() {
        return port;
    }

    public String getBaseDN() {
        return baseDN;
    }

    public String getServerInfo() {
        if (!running) {
            return "Server is not running";
        }

        return String.format("""
            Server Status: RUNNING
            Listen Address: localhost:%d
            Base DN: %s
            Admin DN: cn=admin,%s
            Protocol: LDAP (non-SSL)

            Connection String: ldap://localhost:%d
            """, port, baseDN, baseDN, port);
    }
}
