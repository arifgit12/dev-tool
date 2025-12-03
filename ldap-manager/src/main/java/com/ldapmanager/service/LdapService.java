package com.ldapmanager.service;

import com.ldapmanager.model.LdapConnection;
import com.ldapmanager.model.LdapGroup;
import com.ldapmanager.model.LdapUser;
import com.unboundid.ldap.sdk.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class LdapService {
    private static final Logger logger = LoggerFactory.getLogger(LdapService.class);

    public boolean testConnection(LdapConnection connection) {
        try (LDAPConnection ldapConnection = createConnection(connection)) {
            return ldapConnection.isConnected();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean authenticateUser(LdapConnection connection, String username, String password) {
        try (LDAPConnection ldapConnection = new LDAPConnection()) {
            ldapConnection.connect(connection.getHost(), connection.getPort());

            // First, bind with admin credentials to search for the user
            logger.debug("Binding with admin credentials to search for user: {}", username);
            BindResult adminBindResult = ldapConnection.bind(connection.getUserDn(), connection.getPassword());
            if (adminBindResult.getResultCode() != ResultCode.SUCCESS) {
                logger.error("Failed to bind with admin credentials");
                return false;
            }

            // Search for the user to get their DN
            String userDN = findUserDN(ldapConnection, connection, username);
            if (userDN == null) {
                logger.debug("User not found: {}", username);
                return false;
            }

            logger.debug("Found user DN: {}", userDN);

            // Now authenticate as the user
            BindResult bindResult = ldapConnection.bind(userDN, password);
            boolean authenticated = bindResult.getResultCode() == ResultCode.SUCCESS;
            logger.debug("User authentication result: {}", authenticated);
            return authenticated;
        } catch (Exception e) {
            logger.error("Authentication error: {}", e.getMessage(), e);
            return false;
        }
    }

    public LdapUser findUser(LdapConnection connection, String username) throws LDAPException {
        try (LDAPConnection ldapConnection = createConnection(connection)) {
            String searchBase = connection.getUserSearchBase() != null && !connection.getUserSearchBase().isEmpty() ?
                connection.getUserSearchBase() : connection.getBaseDn();

            logger.info("=== Finding User ===");
            logger.info("Username: {}", username);
            logger.info("Search Base: {}", searchBase);
            logger.info("User Search Base (from config): '{}'", connection.getUserSearchBase());
            logger.info("Base DN (from config): '{}'", connection.getBaseDn());

            if (searchBase == null || searchBase.isEmpty()) {
                String error = "Search base is NULL or EMPTY! Check your connection configuration.";
                logger.error(error);
                throw new LDAPException(ResultCode.PARAM_ERROR, error);
            }

            String filter = connection.getUserSearchFilter() != null ?
                connection.getUserSearchFilter().replace("{0}", username) :
                String.format("(|(uid=%s)(cn=%s)(sAMAccountName=%s)(mail=%s))",
                    username, username, username, username);

            logger.info("Search Filter: {}", filter);

            SearchResult searchResult = ldapConnection.search(
                searchBase,
                SearchScope.SUB,
                filter
            );

            logger.info("Search completed. Entries found: {}", searchResult.getEntryCount());

            if (searchResult.getEntryCount() > 0) {
                SearchResultEntry entry = searchResult.getSearchEntries().get(0);
                logger.info("User found: {}", entry.getDN());
                return mapToLdapUser(entry, connection, ldapConnection);
            }

            logger.warn("User not found: {}", username);
            return null;
        } catch (LDAPException e) {
            logger.error("=== LDAP EXCEPTION in findUser ===");
            logger.error("Error Code: {}", e.getResultCode());
            logger.error("Error Message: {}", e.getMessage());
            logger.error("Diagnostic Message: {}", e.getDiagnosticMessage());
            throw e;
        }
    }

    public List<LdapUser> searchUsers(LdapConnection connection, String searchTerm) throws LDAPException {
        logger.info("Searching users with term: {}", searchTerm);
        logger.info("Connection: {}:{}, Base DN: {}", connection.getHost(), connection.getPort(), connection.getBaseDn());

        try (LDAPConnection ldapConnection = createConnection(connection)) {
            String searchBase = connection.getUserSearchBase() != null && !connection.getUserSearchBase().isEmpty() ?
                connection.getUserSearchBase() : connection.getBaseDn();

            String filter = String.format(
                "(|(uid=*%s*)(cn=*%s*)(sAMAccountName=*%s*)(mail=*%s*)(displayName=*%s*))",
                searchTerm, searchTerm, searchTerm, searchTerm, searchTerm
            );

            logger.info("Search Base: {}", searchBase);
            logger.info("Search Filter: {}", filter);

            SearchRequest searchRequest = new SearchRequest(
                searchBase,
                SearchScope.SUB,
                filter
            );
            searchRequest.setSizeLimit(100);

            SearchResult searchResult = ldapConnection.search(searchRequest);
            logger.info("Search returned {} entries", searchResult.getEntryCount());

            List<LdapUser> users = new ArrayList<>();
            for (SearchResultEntry entry : searchResult.getSearchEntries()) {
                users.add(mapToLdapUser(entry, connection, ldapConnection));
            }
            logger.info("Mapped {} users", users.size());
            return users;
        } catch (LDAPException e) {
            logger.error("LDAP search failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    public List<LdapGroup> getUserGroups(LdapConnection connection, String userDN) throws LDAPException {
        try (LDAPConnection ldapConnection = createConnection(connection)) {
            String searchBase = connection.getGroupSearchBase() != null ?
                connection.getGroupSearchBase() : connection.getBaseDn();

            String filter = connection.getGroupSearchFilter() != null ?
                connection.getGroupSearchFilter().replace("{0}", userDN) :
                String.format("(member=%s)", userDN);

            SearchResult searchResult = ldapConnection.search(
                searchBase,
                SearchScope.SUB,
                filter
            );

            List<LdapGroup> groups = new ArrayList<>();
            for (SearchResultEntry entry : searchResult.getSearchEntries()) {
                LdapGroup group = new LdapGroup();
                group.setDn(entry.getDN());
                group.setName(getAttributeValue(entry, "cn"));
                group.setDescription(getAttributeValue(entry, "description"));
                groups.add(group);
            }
            return groups;
        }
    }

    public List<LdapUser> getAllUsers(LdapConnection connection, int maxResults) throws LDAPException {
        try (LDAPConnection ldapConnection = createConnection(connection)) {
            String searchBase = connection.getUserSearchBase() != null ?
                connection.getUserSearchBase() : connection.getBaseDn();

            String filter = "(objectClass=person)";

            SearchRequest searchRequest = new SearchRequest(
                searchBase,
                SearchScope.SUB,
                filter
            );
            searchRequest.setSizeLimit(maxResults);

            SearchResult searchResult = ldapConnection.search(searchRequest);

            List<LdapUser> users = new ArrayList<>();
            for (SearchResultEntry entry : searchResult.getSearchEntries()) {
                users.add(mapToLdapUser(entry, connection, ldapConnection));
            }
            return users;
        }
    }

    private LDAPConnection createConnection(LdapConnection connection) throws LDAPException {
        try {
            logger.info("Creating LDAP connection to {}:{} (SSL: {})",
                connection.getHost(), connection.getPort(), connection.isUseSsl());

            LDAPConnection ldapConnection;

            if (connection.isUseSsl()) {
                logger.info("Using SSL connection");
                ldapConnection = new LDAPConnection(
                    new com.unboundid.util.ssl.SSLUtil(
                        new com.unboundid.util.ssl.TrustAllTrustManager()
                    ).createSSLSocketFactory()
                );
            } else {
                ldapConnection = new LDAPConnection();
            }

            ldapConnection.connect(connection.getHost(), connection.getPort());
            logger.info("Connected to LDAP server");

            if (connection.getUserDn() != null && !connection.getUserDn().isEmpty() &&
                connection.getPassword() != null && !connection.getPassword().isEmpty()) {
                logger.info("Binding as: {}", connection.getUserDn());
                ldapConnection.bind(connection.getUserDn(), connection.getPassword());
                logger.info("Bind successful");
            } else {
                logger.warn("No bind credentials provided, using anonymous bind");
            }

            return ldapConnection;
        } catch (java.security.GeneralSecurityException e) {
            logger.error("SSL connection error: {}", e.getMessage(), e);
            throw new LDAPException(ResultCode.CONNECT_ERROR, "SSL connection error: " + e.getMessage(), e);
        } catch (LDAPException e) {
            logger.error("LDAP connection error: {}", e.getMessage(), e);
            throw e;
        }
    }

    private String findUserDN(LDAPConnection ldapConnection, LdapConnection connection, String username) throws LDAPException {
        String searchBase = connection.getUserSearchBase() != null && !connection.getUserSearchBase().isEmpty() ?
            connection.getUserSearchBase() : connection.getBaseDn();

        logger.info("=== Searching for user DN ===");
        logger.info("Username: {}", username);
        logger.info("Search Base: {}", searchBase);
        logger.info("User Search Base (from config): {}", connection.getUserSearchBase());
        logger.info("Base DN (from config): {}", connection.getBaseDn());

        try {
            String filter = connection.getUserSearchFilter() != null ?
                    connection.getUserSearchFilter().replace("{0}", username) :
                    String.format("(|(uid=%s)(sAMAccountName=%s)(cn=%s))", username, username, username);

            logger.info("Search Filter: {}", filter);

            SearchResult searchResult = ldapConnection.search(
                    searchBase,
                    SearchScope.SUB,
                    filter
            );

            logger.info("Search completed. Entries found: {}", searchResult.getEntryCount());

            if (searchResult.getEntryCount() > 0) {
                String userDN = searchResult.getSearchEntries().get(0).getDN();
                logger.info("User DN found: {}", userDN);
                return userDN;
            } else {
                logger.warn("=== USER NOT FOUND ===");
                logger.warn("No entries found for username: {}", username);
                logger.warn("Search Base: {}", searchBase);
                logger.warn("Filter: {}", filter);
                logger.warn("Possible reasons:");
                logger.warn("  1. Username is incorrect");
                logger.warn("  2. User doesn't exist in the directory");
                logger.warn("  3. Search base DN is wrong");
                logger.warn("  4. User is in a different OU not covered by search base");
                logger.warn("  5. LDAP filter doesn't match the user's attributes");
            }
            return null;
        } catch (Exception e) {
            logger.error("=== EXCEPTION DURING USER SEARCH ===");
            logger.error("Error Type: {}", e.getClass().getName());
            logger.error("Error Message: {}", e.getMessage());
            logger.error("Username: {}", username);
            logger.error("Search Base: {}", searchBase);
            logger.error("Full Stack Trace:", e);
            throw e;
        }

    }

    private LdapUser mapToLdapUser(SearchResultEntry entry, LdapConnection connection, LDAPConnection ldapConnection) {
        LdapUser user = new LdapUser();
        user.setDn(entry.getDN());
        user.setUsername(getAttributeValue(entry, "uid", "sAMAccountName", "cn"));
        user.setCn(getAttributeValue(entry, "cn"));
        user.setEmail(getAttributeValue(entry, "mail", "email"));
        user.setFirstName(getAttributeValue(entry, "givenName"));
        user.setLastName(getAttributeValue(entry, "sn"));
        user.setDisplayName(getAttributeValue(entry, "displayName"));
        user.setTelephone(getAttributeValue(entry, "telephoneNumber", "mobile"));
        user.setDepartment(getAttributeValue(entry, "department", "ou"));
        user.setTitle(getAttributeValue(entry, "title"));

        // Check if user is enabled
        String userAccountControl = getAttributeValue(entry, "userAccountControl");
        if (userAccountControl != null) {
            try {
                int uac = Integer.parseInt(userAccountControl);
                user.setEnabled((uac & 0x2) == 0);  // ADS_UF_ACCOUNTDISABLE = 0x2
            } catch (NumberFormatException e) {
                user.setEnabled(true);
            }
        } else {
            user.setEnabled(true);
        }

        // Get all attributes
        Map<String, List<String>> attributes = new HashMap<>();
        for (com.unboundid.ldap.sdk.Attribute attr : entry.getAttributes()) {
            attributes.put(attr.getName(), Arrays.asList(attr.getValues()));
        }
        user.setAttributes(attributes);

        // Get user groups
        try {
            List<LdapGroup> groups = getUserGroups(connection, entry.getDN());
            user.setGroups(groups.stream().map(LdapGroup::getName).toList());
        } catch (Exception e) {
            user.setGroups(new ArrayList<>());
        }

        return user;
    }

    private String getAttributeValue(SearchResultEntry entry, String... attributeNames) {
        for (String attrName : attributeNames) {
            com.unboundid.ldap.sdk.Attribute attr = entry.getAttribute(attrName);
            if (attr != null && attr.getValue() != null) {
                return attr.getValue();
            }
        }
        return null;
    }
}
