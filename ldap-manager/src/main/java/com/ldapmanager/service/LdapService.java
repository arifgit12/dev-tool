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

            String userDN = findUserDN(ldapConnection, connection, username);
            if (userDN == null) {
                return false;
            }

            BindResult bindResult = ldapConnection.bind(userDN, password);
            return bindResult.getResultCode() == ResultCode.SUCCESS;
        } catch (Exception e) {
            return false;
        }
    }

    public LdapUser findUser(LdapConnection connection, String username) throws LDAPException {
        try (LDAPConnection ldapConnection = createConnection(connection)) {
            String searchBase = connection.getUserSearchBase() != null ?
                connection.getUserSearchBase() : connection.getBaseDn();

            String filter = connection.getUserSearchFilter() != null ?
                connection.getUserSearchFilter().replace("{0}", username) :
                String.format("(|(uid=%s)(cn=%s)(sAMAccountName=%s)(mail=%s))",
                    username, username, username, username);

            SearchResult searchResult = ldapConnection.search(
                searchBase,
                SearchScope.SUB,
                filter
            );

            if (searchResult.getEntryCount() > 0) {
                SearchResultEntry entry = searchResult.getSearchEntries().get(0);
                return mapToLdapUser(entry, connection, ldapConnection);
            }
            return null;
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
        String searchBase = connection.getUserSearchBase() != null ?
            connection.getUserSearchBase() : connection.getBaseDn();

        String filter = connection.getUserSearchFilter() != null ?
            connection.getUserSearchFilter().replace("{0}", username) :
            String.format("(|(uid=%s)(sAMAccountName=%s)(cn=%s))", username, username, username);

        SearchResult searchResult = ldapConnection.search(
            searchBase,
            SearchScope.SUB,
            filter
        );

        if (searchResult.getEntryCount() > 0) {
            return searchResult.getSearchEntries().get(0).getDN();
        }
        return null;
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
