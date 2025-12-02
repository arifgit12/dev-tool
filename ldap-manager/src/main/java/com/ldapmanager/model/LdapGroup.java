package com.ldapmanager.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LdapGroup implements Serializable {
    private String dn;
    private String name;
    private String description;
    private List<String> members = new ArrayList<>();
}
