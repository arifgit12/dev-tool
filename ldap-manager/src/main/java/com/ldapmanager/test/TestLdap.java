package com.ldapmanager.test;

public class TestLdap {

    private static final String strPatternRegex = "\\$\\{[a-zA-Z0-9]+\\}";//RegX for ${P[0-9]+}
    public static void main(String[] args) {
        System.out.println("LDAP Manager Test");
        System.out.println(getAdUsername("user1", "domain.local"));
    }

    private static String getAdUsername(String userName, String domain) {
        String searchPath = "CN=${username},CN=users,DC=local,DC=cerebra,DC=sa";
        return searchPath.replaceFirst(strPatternRegex, userName);
    }
}
