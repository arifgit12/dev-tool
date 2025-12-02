package com.ldapmanager.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LdapConnection implements Serializable {
    private String id;
    private String name;
    private String host;
    private int port;
    private String baseDn;
    private String userDn;
    private String password;
    private boolean useSsl;
    private String userSearchFilter;  // e.g., (uid={0}) or (sAMAccountName={0})
    private String userSearchBase;    // e.g., ou=users,dc=example,dc=com
    private String groupSearchBase;   // e.g., ou=groups,dc=example,dc=com
    private String groupSearchFilter; // e.g., (member={0})

    @JsonIgnore
    public String getUrl() {
        String protocol = useSsl ? "ldaps" : "ldap";
        return String.format("%s://%s:%d", protocol, host, port);
    }

    @JsonIgnore
    public String getFullBaseDn() {
        return baseDn;
    }
}
