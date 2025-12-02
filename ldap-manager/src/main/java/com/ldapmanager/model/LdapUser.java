package com.ldapmanager.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LdapUser implements Serializable {
    private String dn;
    private String username;
    private String cn;
    private String email;
    private String firstName;
    private String lastName;
    private String displayName;
    private String telephone;
    private String department;
    private String title;
    private boolean enabled;
    private List<String> groups = new ArrayList<>();
    private Map<String, List<String>> attributes = new HashMap<>();

    public String getFullName() {
        if (displayName != null && !displayName.isEmpty()) {
            return displayName;
        }
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        }
        return cn != null ? cn : username;
    }
}
